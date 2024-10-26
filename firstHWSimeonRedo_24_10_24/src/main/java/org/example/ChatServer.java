package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {
    private static final int PORT = 5000;
    private Map<String, AsynchronousSocketChannel> clients = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        new ChatServer().start();
    }

    public void start() {
        try {
            AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(PORT));
            System.out.println("Server started on port " + PORT);

            serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
                @Override
                public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
                    serverChannel.accept(null, this);

                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    clientChannel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result, ByteBuffer buffer) {
                            buffer.flip();
                            String message = new String(buffer.array(), 0, result).trim();
                            buffer.clear();

                            if (message.startsWith("REGISTER ")) {
                                String username = message.substring(9);
                                if (!clients.containsKey(username)) {
                                    clients.put(username, clientChannel);
                                    System.out.println(username + " registered.");
                                    broadcast(username + " joined the chat.", null);
                                } else {
                                    sendMessage(clientChannel, "ERROR: Username already taken.");
                                }
                            } else if (message.equalsIgnoreCase("exit")) {
                                String username = getUsername(clientChannel);
                                if (username != null) {
                                    clients.remove(username);
                                    broadcast(username + " has left the chat.", null);
                                    System.out.println(username + " has left the chat.");
                                    try {
                                        clientChannel.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                String username = getUsername(clientChannel);
                                if (username != null) {
                                    broadcast(username + ": " + message, clientChannel);
                                } else {
                                    sendMessage(clientChannel, "ERROR: You must register before sending messages.");
                                }
                            }
                            clientChannel.read(buffer, buffer, this);
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer buffer) {
                            exc.getMessage();
                        }
                    });
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

    private void broadcast(String message, AsynchronousSocketChannel sender) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        for (Map.Entry<String, AsynchronousSocketChannel> client : clients.entrySet()) {
            if (client.getValue() != sender) {
                client.getValue().write(buffer.duplicate(), null, new CompletionHandler<Integer, Void>() {
                    @Override
                    public void completed(Integer result, Void attachment) {
                        System.out.println("Successful write");
                    }

                    @Override
                    public void failed(Throwable exc, Void attachment) {
                        exc.getMessage();
                    }
                });
            }
        }
    }

    private void sendMessage(AsynchronousSocketChannel clientChannel, String message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        clientChannel.write(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {
                System.out.println("Successful write");
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.getMessage();
            }
        });
    }

    private String getUsername(AsynchronousSocketChannel clientChannel) {
        for (Map.Entry<String, AsynchronousSocketChannel> entry : clients.entrySet()) {
            if (entry.getValue().equals(clientChannel)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
