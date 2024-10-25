package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class AsyncFileTransferClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the file path: ");
        String filePath = scanner.nextLine();
        scanner.close();

        CountDownLatch latch = new CountDownLatch(1);

        try (AsynchronousSocketChannel clientChannel = AsynchronousSocketChannel.open()) {
            clientChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attachment) {
                    System.out.println("Connected to the server.");
                    sendFile(clientChannel,
                            Path.of(
                            "/home/simeon/School/Internet-Programming/ClassRepo/internet_programming/secondHomeworkSimeon_25_10_2024/client_files",
                            filePath));
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    System.err.println("Failed to connect to the server: " + exc.getMessage());
                    latch.countDown();
                }
            });

            Thread.sleep(5000);
        } catch ( IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void sendFile(AsynchronousSocketChannel clientChannel, Path filePath) {
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            System.err.println("File does not exist or is not readable: " + filePath);
            return;
        }

        try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            readFile(clientChannel, fileChannel, buffer);
        } catch (IOException e) {
            System.err.println("Error opening file: " + e.getMessage());
        }
    }

    private static void readFile(AsynchronousSocketChannel clientChannel, FileChannel fileChannel,
                                 ByteBuffer buffer) {
        try {
            int bytesRead = fileChannel.read(buffer);
            if (bytesRead == -1) {
                System.out.println("File sent.");
                clientChannel.close();
                return;
            }

            buffer.flip();
            clientChannel.write(buffer, buffer, new CompletionHandler<>() {
                @Override
                public void completed(Integer result, ByteBuffer buffer) {
                    buffer.clear();
                    readFile(clientChannel, fileChannel, buffer);
                }

                @Override
                public void failed(Throwable exc, ByteBuffer buffer) {
                    System.err.println("Failed send: " + exc.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
