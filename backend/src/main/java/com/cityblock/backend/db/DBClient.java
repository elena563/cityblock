package com.cityblock.backend.db;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;

@Component
public class DBClient {

    @Value("${db.host:127.0.0.1}")
    private String host;

    @Value("${db.port:5555}")
    private int port;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    @PostConstruct
    public void connect() throws IOException {
        socket = new Socket(host, port);
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    public synchronized String send(String command) {
        try {
            writer.println(command);
            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("DB error: " + e.getMessage(), e);
        }
    }
}
