package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Scanner;

public class ChatClient {
    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private AsynchronousSocketChannel clientChannel;

    public static void main(String[] args) {
        new ChatClient().start();
    }

    public void start() {
        try {
            clientChannel = AsynchronousSocketChannel.open();
            clientChannel.connect(new InetSocketAddress(HOST, PORT), null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attachment) {
                    System.out.println("Connected to the server.");
                    register();
                    listenForMessages();
                    sendMessages();
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    exc.getMessage();
                }
            });


            Thread.sleep(Long.MAX_VALUE);

        } catch (IOException | InterruptedException e) {
            e.getMessage();
        }
    }

    private void register() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter a username: ");
        String username = scanner.nextLine();
        sendMessage("REGISTER " + username);
    }

    private void sendMessages() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String message = scanner.nextLine();
            sendMessage(message);
            if (message.equalsIgnoreCase("exit")) {
                try {
                    clientChannel.close();
                    break;
                } catch (IOException e) {
                    e.getMessage();
                }
            }
        }
    }

    private void listenForMessages() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        clientChannel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer buffer) {
                buffer.flip();
                String message = new String(buffer.array(), 0, result).trim();
                System.out.println(message);
                buffer.clear();
                clientChannel.read(buffer, buffer, this);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer buffer) {
                exc.getMessage();
            }
        });
    }

    private void sendMessage(String message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        clientChannel.write(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {
                System.out.println("Successfully sent");
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.getMessage();
            }
        });
    }
}
