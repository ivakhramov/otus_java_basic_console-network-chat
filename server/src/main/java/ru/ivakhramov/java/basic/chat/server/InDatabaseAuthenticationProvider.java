package ru.ivakhramov.java.basic.chat.server;

import java.util.ArrayList;
import java.util.List;

public class InDatabaseAuthenticationProvider implements AuthenticatedProvider {

    private Server server;
    private DatabaseManager databaseManager;

    List<User> users = new ArrayList<>();

    public InDatabaseAuthenticationProvider(Server server) {

        this.server = server;
        this.databaseManager = new DatabaseManager(server.getConnection());
        this.users = databaseManager.getUsers();
    }

    @Override
    public void initialize() {

        System.out.println("Сервис аутентификации запущен: In database режим");
    }

    private User getUserByLoginAndPassword(String login, String password) {

        for (User user : users) {
            if (user.getLogin().equals(login) && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {

        User authUser = getUserByLoginAndPassword(login, password);

        if (authUser.getName() == null) {
            clientHandler.sendMessage("Некорректный логин/пароль");
            return false;
        }

        if (server.isNameBusy(authUser.getName())) {
            clientHandler.sendMessage("Учетная запись уже занята");
            return false;
        }

        clientHandler.setUser(authUser);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authUser.getName());
        return true;
    }

    private boolean isLoginAlreadyExist(String login) {

        for (User user : users) {
            if (user.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNameAlreadyExist(String name) {

        for (User user : users) {
            if (user.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public int getMaxUserIdFromUsers() {

        int maxUserId = -1;
        for (User user : users) {
            if (maxUserId < user.getId()) {
                maxUserId = user.getId();
            }
        }
        return maxUserId;
    }

    @Override
    public synchronized boolean registration(ClientHandler clientHandler, String login, String password, String name, EnumRole role) {

        List<Role> roles = new ArrayList<>();
        Role newRole = new Role(role == EnumRole.ADMIN ? 1 : 2, role);
        roles.add(newRole);
        User newUser = new User(getMaxUserIdFromUsers() + 1, login, password, name, roles);

        if (login.trim().length() < 3 || password.trim().length() < 6 || name.trim().length() < 2) {
            clientHandler.sendMessage("Требования логин 3+ символа, пароль 6+ символа," +
                    "имя пользователя 2+ символа не выполнены");
            return false;
        }

        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }

        if (isNameAlreadyExist(name)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }

        databaseManager.addUser(login, password, name, EnumRole.USER);
        users.add(newUser);
        clientHandler.setUser(newUser);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/regok " + newUser.getName());

        return true;
    }
}
