package ru.ivakhramov.java.basic.chat.server;


public class ServerApplication {

    public static void main(String[] args) {

        int port = 8189;
        String url = "jdbc:postgresql://localhost:5432/otus_java_basic_console-network-chat";
        String user = "postgres";
        String password = "qwerty";

        new Server(port, new Database(url, user, password).getConnection()).start();
    }
}