package ru.ivakhramov.java.basic.chat.server;

import java.util.ArrayList;
import java.util.List;

public class InMemoryAuthenticationProvider implements AuthenticatedProvider {

    private class User {

        private String login;
        private String password;
        private String username;
        private UserRole role;

        public User(String login, String password, String username, UserRole role) {

            this.login = login;
            this.password = password;
            this.username = username;
            this.role = role;
        }
    }

    private Server server;
    private List<User> users;

    public InMemoryAuthenticationProvider(Server server) {

        this.server = server;
        this.users = new ArrayList<>();

        this.users.add(new User("admin", "admin", "default_admin", UserRole.ADMIN));
        this.users.add(new User("user", "user", "default_user", UserRole.USER));
    }

    @Override
    public void initialize() {

        System.out.println("Сервис аутентификации запущен: In memory режим");
    }

    private String getUsernameByLoginAndPassword(String login, String password) {

        for (User user : users) {
            if (user.login.equals(login) && user.password.equals(password)) {
                return user.username;
            }
        }
        return null;
    }

    private UserRole getRoleByLogin(String login) {

        for (User user : users) {
            if (user.login.equals(login)) {
                return user.role;
            }
        }
        return null;
    }

    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {

        String authName = getUsernameByLoginAndPassword(login, password);
        UserRole authRole = getRoleByLogin(login);

        if (authName == null) {
            clientHandler.sendMessage("Некорректный логин/пароль");
            return false;
        }

        if (server.isUsernameBusy(authName)) {
            clientHandler.sendMessage("Учетная запись уже занята");
            return false;
        }

        clientHandler.setUsername(authName);
        clientHandler.setRole(authRole);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authName);
        return true;
    }

    private boolean isLoginAlreadyExist(String login) {

        for (User user : users) {
            if (user.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsernameAlreadyExist(String username) {

        for (User user : users) {
            if (user.username.equals(username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username, UserRole role) {

        if (login.trim().length() < 3 || password.trim().length() < 6
                || username.trim().length() < 2) {
            clientHandler.sendMessage("Требования логин 3+ символа, пароль 6+ символа," +
                    "имя пользователя 2+ символа не выполнены");
            return false;
        }

        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }

        if (isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }

        users.add(new User(login, password, username, role));
        clientHandler.setUsername(username);
        clientHandler.setRole(role);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + username);
        return true;
    }
}