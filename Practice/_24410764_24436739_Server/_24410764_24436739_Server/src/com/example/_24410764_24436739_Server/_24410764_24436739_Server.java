package com.example._24410764_24436739_Server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Collections;


public class _24410764_24436739_Server {
    private static final int PORT = 5000;
    private ServerSocket serverSocket;
    private ScheduleModel model = new ScheduleModel();

    public void startServer() {
        try{
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("New Client connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable{
        private Socket clientSocket;
        private  BufferedReader input;
        private PrintWriter output;

        ClientHandler(Socket clientSocket){
            this.clientSocket = clientSocket;
        }
        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                output = new PrintWriter(clientSocket.getOutputStream(), true);

                String message;
                while((message = input.readLine()) != null){
                    if(message.startsWith("STOP")){
                        output.println("TERMINATE|Server stopped");
                        break;
                    }
                    if(message.startsWith("EARLY")){
                        new Thread(() -> {
                            try {
                                String response = model.earlyLectures();
                                output.println(response);
                            } catch(Exception e){
                                output.println("ERROR|Early lectures failed: " + e.getMessage());
                            }
                        }).start();
                    } else {
                        try {
                            String response = processRequest(message);
                            output.println(response);
                        } catch (ScheduleModel.IncorrectActionException e){
                            output.println("ERROR|" + e.getMessage());
                        }
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        private void closeConnection() throws IOException {
            input.close();
            output.close();
            clientSocket.close();
            System.out.println("Client disconnected");
        }
    }

    private synchronized String processRequest(String message) throws ScheduleModel.IncorrectActionException {
        String[] parts = message.split("\\|");
        String action = parts[0];
        if(parts.length < 1 || parts[0].isEmpty()){
            throw new ScheduleModel.IncorrectActionException("Invalid action");
        }
        switch (action.toLowerCase()) {

            case "add":
                return model.addLecture(parts);

            case "remove":
                return model.removeLecture(parts);

            case "display":
                return model.displaySchedule();

            case "early":
                return model.earlyLectures();

            case "other":
                throw new ScheduleModel.IncorrectActionException("Action 'OTHER' is not supported by this server");

            default:
                throw new ScheduleModel.IncorrectActionException("Action not supported: " + action);
        }
    }

    public static void main(String[] args){
        _24410764_24436739_Server server = new _24410764_24436739_Server();
        server.startServer();
    }
}
