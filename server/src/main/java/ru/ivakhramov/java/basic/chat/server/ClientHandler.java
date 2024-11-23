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
    private DatabaseManager databaseManager;
    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {

        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.databaseManager = new DatabaseManager(server.getConnection());

        new Thread(() -> {
            try {
                System.out.println("Клиент подключился ");

                //цикл аутентификации
                while (true) {
                    sendMessage("Перед работой необходимо пройти аутентификацию с помощью команды\n" +
                            "/auth \"login\" \"password\" или регистрацию с помощью команды\n" +
                            "/reg \"login\" \"password\" \"name\"");
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
                                    .registration(this, elements[1], elements[2], elements[3], EnumRole.USER)) {
                                break;
                            }
                            continue;
                        }
                    }
                }
                System.out.println("Клиент " + user.getName() + " успешно прошел аутентификацию");
                sendMessage("Вы можете узнать список команд для работы с чатом с помощью команды /help");

                //цикл работы
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {

                        String[] substrings = message.split(" ");

                        if (message.startsWith("/changeName ")) {

                            if (substrings.length != 2) {
                                sendMessage("Неверный формат команды /changeName ");
                                continue;
                            }

                            user.setName(substrings[1]);
                            databaseManager.updateName(user.getId(), substrings[1]);
                            sendMessage("Ваш новый ник: " + user.getName());
                        }

                        if (message.startsWith("/getName")) {

                            sendMessage("Ваш ник: " + user.getName());
                        }

                        if (message.startsWith("/changeRole ")) {

                            if (substrings.length != 3) {
                                sendMessage("Неверный формат команды /changeRole ");
                                continue;
                            }

                            if (!isRoleAdmin()) {
                                sendMessage("Вы не администратор и не можете менять роли пользователей");
                                continue;
                            }

                            if (!isNameExist(substrings[1])) {
                                sendMessage("Пользователь с ником " + substrings[1] + " не зарегистрирован в чате");
                                continue;
                            }

                            if (substrings[2].equals("ADMIN")) {
                                server.getClientByName(substrings[1]).getUser().addRoleToRoles(EnumRole.ADMIN);
                                databaseManager.addRole(server.getUserIdByName(substrings[1]), EnumRole.ADMIN);
                                sendMessage("Теперь у " + substrings[1] + " есть роль/роли " + server.getClientByName(substrings[1]).getUser().getRoles());
                            } else if (substrings[2].equals("USER")) {
                                server.getClientByName(substrings[1]).getUser().removeRoleFromRoles(EnumRole.ADMIN);
                                databaseManager.deleteRole(server.getUserIdByName(substrings[1]), EnumRole.ADMIN);
                                sendMessage("Теперь у " + substrings[1] + " есть роль/роли " + server.getClientByName(substrings[1]).getUser().getRoles());
                            } else {
                                sendMessage("Указанная вами роль \"" + substrings[2] + "\" не существует");
                            }
                        }

                        if (message.startsWith("/getRole")) {

                            sendMessage("Ваша роль/роли: " + user.getRoles());
                        }

                        if (message.startsWith("/w ")) {

                            if (substrings.length < 3) {
                                sendMessage("Неверный формат команды /w ");
                                continue;
                            }

                            String privateName = substrings[1];

                            String bodyMessage = "";
                            for (int i = 2; i < substrings.length; i++) {
                                bodyMessage += (substrings[i] + " ");
                            }

                            server.privateMessage(user.getName() + " : " + bodyMessage, privateName, user.getName());
                        }

                        if (message.startsWith("/kick ")) {

                            if (substrings.length != 2) {
                                sendMessage("Неверный формат команды /kick ");
                                continue;
                            }

                            if (!isRoleAdmin()) {
                                sendMessage("Вы не администратор и не можете удалять пользователей из чата");
                                continue;
                            }

                            if (!isNameExist(substrings[1])) {
                                sendMessage("Пользователь с ником " + substrings[1] + " не зарегистрирован в чате");
                                continue;
                            }

                            server.kickUser(substrings[1]);
                            sendExitok(substrings[1]);
                            sendMessage("Клиент с ником " + substrings[1] + " отключен от чата");
                        }

                        if (message.startsWith("/help")) {
                            sendMessage("Вы можете воспользоваться следующими командами:\n" +
                                    "/auth \"login\" \"password\" - пройти аутентификацию\n" +
                                    "/reg \"login\" \"password\" \"name\" - пройти регистрацию\n" +
                                    "/changeName \"name\" - изменить name\n" +
                                    "/getName - узнать name\n" +
                                    "/changeRole \"name\" \"ADMIN/USER\"- изменить роль (если вы администратор)\n" +
                                    "/getRole - узнать роль/роли\n" +
                                    "/w \"name\" \"сообщение\" - отправить сообщение пользователю с ником \"name\"\n" +
                                    "\"сообщение\" - отправить сообщение всем пользователям\n" +
                                    "/kick \"name\" - удалить пользователя из чата (если вы администратор)\n" +
                                    "/exit - выйти из программы\n" +
                                    "/help - список команд");
                        }

                        if (message.startsWith("/exit")) {
                            sendExitok(user.getName());
                            break;
                        }
                    } else {
                        server.broadcastMessage(user.getName() + " : " + message);
                    }
                }
            } catch (SocketException e) {
                System.out.println("Ошибка: потеряна связь с клиентом: ");
                e.printStackTrace();
            } catch (EOFException e) {
                System.out.println("Ошибка: потеряна связь с клиентом: ");
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

    public void sendExitok(String name) {
        for (ClientHandler client : server.getClients()) {
            if (client.getUser().getName().equals(name)) {
                sendMessage("/exitok");
                break;
            }
        }
    }

    private boolean isRoleAdmin() {

        boolean isRoleAdmin = false;
        for (Role role : user.getRoles()) {
            isRoleAdmin = role.getRole().equals(EnumRole.ADMIN);
            break;
        }
        return isRoleAdmin;
    }

    private boolean isNameExist(String name) {

        return server.getClientByName(name) != null;
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