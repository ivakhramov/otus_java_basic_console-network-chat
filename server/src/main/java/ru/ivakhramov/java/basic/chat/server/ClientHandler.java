package ru.ivakhramov.java.basic.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler {

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private UserRole role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
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
                                    .registration(this, elements[1], elements[2], elements[3], UserRole.USER)) {
                                break;
                            }
                            continue;
                        }
                    }
                }
                System.out.println("Клиент " + username + " успешно прошел аутентификацию");
                sendMessage("Вы можете узнать список команд для работы с чатом с помощью команды /help");

                //цикл работы
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {

                        String[] substrings = message.split(" ");

                        if (message.startsWith("/changeNickname ")) {

                            if (substrings.length != 2) {
                                sendMessage("Неверный формат команды /changeNickname ");
                                continue;
                            }

                            this.username = substrings[1];
                            sendMessage("Ваш новый ник: " + this.username);
                        }

                        if (message.startsWith("/getNickname")) {

                            sendMessage("Ваш ник: " + this.username);
                        }

                        if (message.startsWith("/changeRole ")) {

                            if (substrings.length != 3) {
                                sendMessage("Неверный формат команды /changeRole ");
                                continue;
                            }

                            if (isRoleAdmin()) {
                                if (isUsernameExist(substrings[1])) {
                                    if (substrings[2].equals("ADMIN")) {
                                        server.getClientByUsername(substrings[1]).role = UserRole.ADMIN;
                                        sendMessage("Вы поменяли роль " + substrings[1] + " на " + server.getClientByUsername(substrings[1]).role);
                                    } else if (substrings[2].equals("USER")) {
                                        server.getClientByUsername(substrings[1]).role = server.getClientByUsername(substrings[1]).role;
                                        sendMessage("Вы поменяли роль " + substrings[1] + " на " + this.role);
                                    } else {
                                        sendMessage("Указанная вами роль \"" + substrings[2] + "\" не существует");
                                    }
                                } else {
                                    sendMessage("Пользователь с ником " + substrings[1] + " не зарегистрирован в чате");
                                }
                            } else {
                                sendMessage("Вы не администратор и не можете менять роли пользователей");
                            }
                        }

                        if (message.startsWith("/getRole")) {

                            sendMessage("Ваша роль: " + this.role);
                        }

                        if (message.startsWith("/w ")) {

                            if (substrings.length < 3) {
                                sendMessage("Неверный формат команды /w ");
                                continue;
                            }

                            String privateUsername = substrings[1];

                            String bodyMessage = "";
                            for (int i = 2; i < substrings.length; i++) {
                                bodyMessage += (substrings[i] + " ");
                            }

                            server.privateMessage(username + " : " + bodyMessage, privateUsername, this.username);
                        }

                        if (message.startsWith("/kick ")) {

                            if (substrings.length != 2) {
                                sendMessage("Неверный формат команды /kick ");
                                continue;
                            }

                            if (isRoleAdmin()) {
                                if (isUsernameExist(substrings[1])) {
                                    server.kickUser(substrings[1]);
                                    sendExitok(substrings[1]);
                                    sendMessage("Клиент с ником " + substrings[1] + " отключен от чата");
                                } else {
                                    sendMessage("Пользователь с ником " + substrings[1] + " не зарегистрирован в чате");
                                }
                            } else {
                                sendMessage("Вы не администратор и не можете удалять пользователей из чата");
                            }
                        }

                        if (message.startsWith("/help")) {
                            sendMessage("Вы можете воспользоваться следующими командами:\n" +
                                    "/auth \"login\" \"password\" - пройти аутентификацию\n" +
                                    "/reg \"login\" \"password\" \"username\" - пройти регистрацию\n" +
                                    "/changeNickname \"ник\" - изменить ник\n" +
                                    "/getNickname - узнать ник\n" +
                                    "/changeRole \"ник\" \"ADMIN/USER\"- изменить роль (если вы администратор)\n" +
                                    "/w \"ник\" \"сообщение\" - отправить сообщение пользователю с ником \"ник\"\n" +
                                    "\"сообщение\" - отправить сообщение всем пользователям\n" +
                                    "/kick \"username\" - удалить пользователя из чата (если вы администратор)\n" +
                                    "/exit - выйти из программы\n" +
                                    "/help - список команд");
                        }

                        if (message.startsWith("/exit")) {
                            sendExitok(this.username);
                            break;
                        }
                    } else {
                        server.broadcastMessage(username + " : " + message);
                    }
                }
            } catch (SocketException e) {
                System.out.print("Ошибка: потеряна связь с клиентом: ");
                e.printStackTrace();
            } catch (EOFException e) {
                System.out.print("Ошибка: потеряна связь с клиентом: ");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Ошибка: ");
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

    public void sendExitok(String username) {
        for (ClientHandler client : server.getClients()) {
            if (client.username.equals(username)) {
                sendMessage("/exitok");
                break;
            }
        }
    }

    private boolean isRoleAdmin() {

        if (this.role.equals(UserRole.ADMIN)) {
            return true;
        }
        return false;
    }

    private boolean isUsernameExist(String username) {

        return server.getClientByUsername(username) != null;
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
