package com.cityblock.sockdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main {

    public static final int PORT = 8080;
    public static void main(String[] args) {
        
        ExecutorService executor = Executors.newCachedThreadPool();

        try (var serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);
            FileManager.loadData();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());
    
                executor.execute(() -> processClient(clientSocket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processClient(Socket socket) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.equals("exit")) break;
                if (line.isEmpty()) continue;

                String[] parts = line.split(" ", 2);
                String method = parts[0];
                String rest = parts.length > 1 ? parts[1] : "";
                String collection = rest.trim().split(" ", 2)[0].split("/", 2)[0];

                String result = "";

                switch (method) {
                    // methods on collections
                    case "create":
                        result = Database.getInstance().createCollection(rest);
                        FileManager.updateCollection(collection);
                        break;
                    case "drop":
                        result = Database.getInstance().dropCollection(rest);
                        FileManager.updateCollection(collection);
                        break;
                    // methods on documents
                    case "read":
                        result = Database.getInstance().readDocument(rest);
                        break;
                    case "insert":
                        result = Database.getInstance().insertDocument(rest);
                        if (result.startsWith("OK ")) {
                            FileManager.updateDocument(collection, result.substring(3));
                        }
                        break;
                    case "write":
                        result = Database.getInstance().writeDocument(rest);
                        FileManager.updateDocument(collection, rest.split(" ", 2)[0].split("/", 2)[1]);
                        break;
                    case "delete":
                        result = Database.getInstance().deleteDocument(rest);
                        FileManager.updateDocument(collection, rest.split(" ", 2)[0].split("/", 2)[1]);
                        break;
                    default:
                        result = "ERROR INVALID METHOD " + method;
                        break;
                }
                out.println(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
