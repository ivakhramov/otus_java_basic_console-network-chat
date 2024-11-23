package ru.ivakhramov.java.basic.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private int port;
    private List<ClientHandler> clients;
    private AuthenticatedProvider authenticatedProvider;
    private Connection connection;

    public List<ClientHandler> getClients() {
        return clients;
    }

    public Connection getConnection() {
        return connection;
    }

    public Server(int port, Connection connection) {

        this.port = port;
        this.connection = connection;
        clients = new ArrayList<>();
        authenticatedProvider = new InDatabaseAuthenticationProvider(this);
        authenticatedProvider.initialize();
    }

    public AuthenticatedProvider getAuthenticatedProvider() {
        return authenticatedProvider;
    }

    public void start() {

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {

        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {

        clients.remove(clientHandler);
    }

    public synchronized void kickUser(String name) {

        for (ClientHandler client : clients) {
            if (client.getUser().getName().equals(name)) {
                client.sendMessage("Вы были отключены от сервера администратором");
                client.disconnect();
                break;
            }
        }
    }

    public synchronized int getUserIdByName(String name) {

        for (ClientHandler client : clients) {
            if (client.getUser().getName().equals(name)) {
                return client.getUser().getId();
            }
        }
        return -1;
    }

    public synchronized ClientHandler getClientByName(String name) {
        for (ClientHandler client : clients) {
            if (client.getUser().getName().equals(name)) {
                return client;
            }
        }
        return null;
    }

    public synchronized boolean isNameBusy(String name) {
        for (ClientHandler client : clients) {
            if (client.getUser().getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastMessage(String message) {

        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void privateMessage(String message, String toName, String fromName) {

        boolean hasToName = false;

        for (ClientHandler client : clients) {
            if (client.getUser().getName().equals(toName)) {
                client.sendMessage(message);
                hasToName = true;
                break;
            }
        }

        if (hasToName) {
            for (ClientHandler client : clients) {
                if (client.getUser().getName().equals(fromName)) {
                    client.sendMessage(message);
                    break;
                }
            }
        } else {
            for (ClientHandler client : clients) {
                if (client.getUser().getName().equals(fromName)) {
                    client.sendMessage("Пользователя с ником " + toName + " не существует");
                    break;
                }
            }
        }
    }
}