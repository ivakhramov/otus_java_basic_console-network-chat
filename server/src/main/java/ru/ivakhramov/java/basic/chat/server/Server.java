package ru.ivakhramov.java.basic.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private int port;
    private List<ClientHandler> clients;

    public Server(int port) {

        this.port = port;
        clients = new ArrayList<>();
    }

    public void start() {

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                subscribe(new ClientHandler(this, socket));
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

    public synchronized void broadcastMessage(String message) {

        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void privateMessage(String message, String toUsername, String fromUsername) {

        boolean hasToUsername = false;

        for (ClientHandler client : clients) {
            if (client.getUsername().equals(toUsername)) {
                client.sendMessage(message);
                hasToUsername = true;
                break;
            }
        }

        if (hasToUsername) {
            for (ClientHandler client : clients) {
                if (client.getUsername().equals(fromUsername)) {
                    client.sendMessage(message);
                    break;
                }
            }
        } else {
            for (ClientHandler client : clients) {
                if (client.getUsername().equals(fromUsername)) {
                    client.sendMessage("Пользователя с ником " + toUsername + " не существует");
                    break;
                }
            }
        }
    }
}
