package com.example._24410764_24436739_Server;

import java.io.*;
import java.net.*;
import java.util.*;


public class _24410764_24436739_Server {
private static final int PORT = 5000;
private ServerSocket serverSocket;
private Socket clientSocket;
private BufferedReader input;
private PrintWriter output;
private HashMap<String, Lecture> schedule = new HashMap<>();
private Set<String> modules = new HashSet<>();

public void startServer() {
try{
    serverSocket = new ServerSocket(PORT);
    System.out.println("Server started on port " + PORT);

    clientSocket = serverSocket.accept();
    System.out.println("Client has been accepted");

    input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    output = new PrintWriter(clientSocket.getOutputStream(), true);

    handleClient();
    } catch (IOException e){
    e.printStackTrace();
    }
   }
   private static class Lecture {
    String date;
    String time;
    String room;
    String module;

    Lecture(String date, String time, String room, String module) {
        this.date = date;
        this.time = time;
        this.room = room;
        this.module = module;
    }
    @Override
       public String toString() {
        return module + " in "+ room;
    }
   }
    private void handleClient() {
        try {
            String message;

            while ((message = input.readLine()) != null) {
                if (message.startsWith("STOP")) {
                    output.println("TERMINATE|Server stopped");
                    break;
                }
                try {
                    String response = processRequest(message);
                    output.println(response);
                }
                catch (IncorrectActionException e) {
                    output.println("ERROR| " + e.getMessage());
                }

            }

            closeConnection();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String processRequest(String message) throws IncorrectActionException {
        String[] parts = message.split("\\|");

        if(parts.length < 1 || parts[0].isEmpty()) {
            throw new IncorrectActionException("Invalid action");
        }
        String action = parts[0];
        switch (action.toLowerCase()) {

            case "add":
                return addLecture(parts);

            case "remove":
                return removeLecture(parts);

            case "display":
                return displaySchedule();

            case "other":
                throw new IncorrectActionException("Action 'OTHER' is not supported by this server");
            default:
                throw new IncorrectActionException("Unknown action: " +  action);

        }
       }
       private String addLecture(String[] parts) throws IncorrectActionException {
        if (parts.length < 5) throw new IncorrectActionException("ADD requires Date, Time, Room and Module");
        String date = parts[1];
        String time = parts[2];
        String room = parts[3];
        String module = parts[4];

        if(!modules.contains(module) && modules.size()>= 5){
            return "ERROR| Module limit reached";
        }

        String key = date + "-" + time;
        if (schedule.containsKey(key)) {
            return "ERROR|Clash: Lecture already exists at this time";
        }
        for(Lecture lecture : schedule.values()){
            if(lecture.room.equals(room) && lecture.time.equals(time)){
                return "ERROR|Room clash: " + room + " is already booked at " + time + " by " + lecture.module;
            }
        }
        Lecture newLecture = new Lecture(date, time, room, module);
        schedule.put(key, newLecture);
        modules.add(module);
        return "OK|Lecture added" + module + " in " + room + " on " + date + "at" + time;
       }
       private String removeLecture(String[] parts) throws IncorrectActionException {
            if(parts.length < 3) throw new IncorrectActionException("REMOVE requires Date and Time");

            String date = parts[1];
            String time = parts[2];
            String key = date + "-" + time;

            Lecture lecture = schedule.get(key);
            if (schedule.remove(key) != null) {
                return "OK|Lecture removed - freed slot:" + date + " at " + time + " (" + lecture + ")";
            }
            return "ERROR|No Lecture found at " + date + " at " + time;
       }
       private String displaySchedule(){
            if(schedule.isEmpty()){
                return "OK|No schedule found";
            }
            StringBuilder builder = new StringBuilder();
            for(Lecture lecture: schedule.values()){
              builder.append(lecture.date)
                      .append(",")
                      .append(lecture.time)
                      .append(",")
                      .append(lecture.room)
                      .append(",")
                      .append(lecture.module)
                      .append(";");
            }
            return builder.toString();
      }
      public static class IncorrectActionException extends Exception {
        public IncorrectActionException(String message) {
            super(message);
        }
      }
      private void closeConnection() throws IOException {
            input.close();
            output.close();
            clientSocket.close();
            serverSocket.close();

            System.out.println("Connection closed");
      }
      public static void main(String[] args){
        _24410764_24436739_Server server = new _24410764_24436739_Server();
        server.startServer();
      }
}







