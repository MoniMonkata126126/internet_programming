package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncFileTransferServer {
    private static final int MAX_THREADS = 3;
    private static final int PORT = 5000;
    private static final int BYTES = 1024;
    private static AsynchronousChannelGroup channelGroup;

    static {
        try {
            channelGroup = AsynchronousChannelGroup.withFixedThreadPool(MAX_THREADS, Executors.defaultThreadFactory());
        } catch (IOException e) {
            e.printStackTrace();
            channelGroup = null;
        }
    }

    public static void main(String[] args) {
        if (channelGroup == null) {
            System.err.println("Failed to initialize AsynchronousChannelGroup. Server cannot start.");
            return;
        }

        try(AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup).bind(new InetSocketAddress(PORT))){

            serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {

                @Override
                public void completed(AsynchronousSocketChannel result, Void attachment) {
                    serverSocketChannel.accept(null, this);
                    handleClient(result);
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    System.err.println("Failed connection: " + exc.getMessage());
                }
            });

            channelGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            if (channelGroup != null && !channelGroup.isShutdown()) {
                try {
                    channelGroup.shutdownNow();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void handleClient(AsynchronousSocketChannel clientSocket){
        ByteBuffer buffer = ByteBuffer.allocate(BYTES);
        String fileName = "file_received.dat";
        Path path = Path.of("/home/simeon/School/Internet-Programming/ClassRepo/internet_programming/secondHomeworkSimeon_25_10_2024/server_files",
                fileName);
        try {
            Files.createDirectories(path.getParent());

            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        try(FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)){
            writeFile(clientSocket, fileChannel, buffer);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void writeFile(AsynchronousSocketChannel clientChannel, FileChannel fileChannel, ByteBuffer buffer){
        clientChannel.read(buffer, buffer, new CompletionHandler<>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result == -1) {
                    try {
                        fileChannel.close();
                        clientChannel.close();
                        System.out.println("File received and saved.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                attachment.flip();
                try {
                    fileChannel.write(attachment);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                attachment.clear();
                writeFile(clientChannel, fileChannel, attachment);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                System.err.println("Failed read: " + exc.getMessage());
                try {
                    fileChannel.close();
                    clientChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }
}
