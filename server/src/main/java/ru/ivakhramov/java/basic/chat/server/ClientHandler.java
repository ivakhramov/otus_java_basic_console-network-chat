package ru.ivakhramov.java.basic.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {

        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                System.out.println("Клиент подключился ");

                //цикл аутентификации
                while (true) {
                    sendMessage("Перед работой необходимо пройти аутентификацию с помощью команды\n" +
                            "/auth \"login\" \"password\" или регистрацию с помощью команды\n" +
                            "/reg \"login\" \"password\" \"username\"");
                    String message = in.readUTF();
                    if (message.startsWith("/")) {

                        if (message.startsWith("/exit")) {
                            sendMessage("/exitok");
                            break;
                        }

                        // /auth login password
                        if (message.startsWith("/auth ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 3) {
                                sendMessage("Неверный формат команды /auth ");
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .authenticate(this, elements[1], elements[2])) {
                                break;
                            }
                            continue;
                        }

                        // /reg login password username
                        if (message.startsWith("/reg ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 4) {
                                sendMessage("Неверный формат команды /reg ");
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .registration(this, elements[1], elements[2], elements[3])) {
                                break;
                            }
                            continue;
                        }
                    }
                }
                System.out.println("Клиент " + username + " успешно прошел аутентификацию");

                //цикл работы
                while (true) {
                    sendMessage("Вы можете узнать список команд для работы с чатом с помощью команды /help");
                    String message = in.readUTF();
                    if (message.startsWith("/")) {

                        String[] substrings = message.split(" ");

                        if (message.startsWith("/changeNickname")) {
                            this.username = substrings[1];
                            sendMessage("Ваш новый ник: " + this.username);
                        }

                        if (message.startsWith("/w")) {
                            String privateUsername = substrings[1];

                            String bodyMessage = "";
                            for (int i = 2; i < substrings.length; i++) {
                                bodyMessage += (substrings[i] + " ");
                            }

                            server.privateMessage(username + " : " + bodyMessage, privateUsername, this.username);
                        }

                        if (message.startsWith("/help")) {
                            sendMessage("Вы можете воспользоваться следующими командами:\n" +
                                    "/auth \"login\" \"password\" - пройти аутентификацию\n" +
                                    "/reg \"login\" \"password\" \"username\" - пройти регистрацию\n" +
                                    "/changeNickname - изменить ник\n" +
                                    "/w \"ник\" \"сообщение\" - отправить сообщение пользователю с ником \"ник\"\n" +
                                    "\"сообщение\" - отправить сообщение всем пользователям\n" +
                                    "/exit - выйти из программы\n" +
                                    "/help - список команд");
                        }

                        if (message.startsWith("/exit")) {
                            sendMessage("/exitok");
                            break;
                        }
                    } else {
                        server.broadcastMessage(username + " : " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message) {

        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {

        server.unsubscribe(this);

        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
