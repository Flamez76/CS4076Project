package com.example._24410764_24436739_server;

import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.Collections;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;




public class _24410764_24436739_Server {
    private static final int PORT = 5000;
    private ServerSocket serverSocket;
    private Map<String, Lecture> schedule = Collections.synchronizedMap(new HashMap<>());
    private Set<String> modules = Collections.synchronizedSet(new HashSet<>());

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
                                    System.out.println("Early thread running");
                                    String response = earlyLectures();
                                    output.println(response);
                                } catch(Exception e){
                                    output.println("ERROR|Early Lectures failed: " + e.getMessage());
                                }
                            }).start();
                    } else {
                        try {
                            String response = processRequest(message);
                            output.println(response);
                        } catch (IncorrectActionException e){
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

    private class DayShiftingTask extends RecursiveAction{
        private final String day;
        private final List<String> earlySlots = Arrays.asList(
                "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00"
        );
        DayShiftingTask(String day){
            this.day = day;
        }
        @Override
        protected void compute() {
            List<Lecture> dayLectures = new ArrayList<>();
            List<String> dayKeys = new ArrayList<>();
            for(Map.Entry<String, Lecture> entry : schedule.entrySet()){
                Lecture lecture = entry.getValue();
                LocalDate date = LocalDate.parse(lecture.date);
                String lectureDay = date.getDayOfWeek().getDisplayName(
                        TextStyle.FULL, Locale.ENGLISH
                );
                if(lectureDay.equals(day)){
                    dayLectures.add(lecture);
                    dayKeys.add(entry.getKey());
                }
            }
            if(dayLectures.isEmpty()) return;
            dayLectures.sort(Comparator.comparing(l -> l.time));
            if(dayLectures.size() > earlySlots.size()) return;
            for(int i = 0; i < dayLectures.size(); i++){
                String targetSlot = earlySlots.get(i);
                for(Lecture existing : schedule.values()){
                    LocalDate date = LocalDate.parse(existing.date);
                    String existingDay = date.getDayOfWeek().getDisplayName(
                            TextStyle.FULL, Locale.ENGLISH
                    );
                    if(existingDay.equals(day) && existing.time.equals(targetSlot) && !dayLectures.contains(existing)){
                        return;
                    }
                }
            }
            synchronized (_24410764_24436739_Server.this) {
                for(String key : dayKeys){
                    schedule.remove(key);
                }
                for(int i = 0; i < dayLectures.size(); i++){
                    Lecture old = dayLectures.get(i);
                    String newTime = earlySlots.get(i);
                    Lecture shifted = new Lecture (old.date, newTime, old.room, old.module);
                    String newKey = old.date + "-" + newTime;
                    schedule.put(newKey, shifted);
                }
            }
        }
    }

    private static class Lecture{
        String date;
        String time;
        String room;
        String module;
        Lecture(String date, String time, String room, String module){
            this.date = date;
            this.time = time;
            this.room = room;
            this.module = module;
        }
        @Override
        public String toString(){
            return module + " in " + room;
        }
    }

    private  String earlyLectures(){
        System.out.println("earlyLectures() called");
        ForkJoinPool pool = new ForkJoinPool();
        System.out.println("Pool created");
        pool.invoke(new RecursiveAction(){
            @Override
            protected void compute(){
                System.out.println("compute() called");
                invokeAll(
                        new DayShiftingTask("Monday"),
                        new DayShiftingTask("Tuesday"),
                        new DayShiftingTask("Wednesday"),
                        new DayShiftingTask("Thursday"),
                        new DayShiftingTask("Friday")
                );
                System.out.println("invokeAll() complete");
            }
        });
        System.out.println("pool.invokeAll() complete");
        pool.shutdown();
        System.out.println("Returning displaySchedule");
        return displaySchedule();
    }

    private synchronized String processRequest(String message) throws IncorrectActionException {
        String[] parts = message.split("\\|");
        String action = parts[0];
        if(parts.length < 1 || parts[0].isEmpty()){
            throw new IncorrectActionException("Invalid action");
        }
        switch (action.toLowerCase()) {

            case "add":
                return addLecture(parts);

            case "remove":
                return removeLecture(parts);

            case "display":
                return displaySchedule();

            case "early":
                return earlyLectures();

            case "other":
                throw new IncorrectActionException("Action 'OTHER' is not supported by this server");

            default:
                throw new IncorrectActionException("Action not supported: " + action);
        }
    }
    private synchronized String addLecture(String[] parts) throws IncorrectActionException {
        if(parts.length < 5) throw new IncorrectActionException("Invalid action");
        String date = parts[1];
        String time = parts[2];
        String room = parts[3];
        String module = parts[4];

        if(!modules.contains(module) && modules.size() >= 5){
            return "ERROR|Module limit reached: maximum of 5 modules allowed per course";
        }

        String key = date + "-" + time;
        if (schedule.containsKey(key)) {
            return "ERROR|Time Clash: Lecture already exists at this time";
        }
        for(Lecture lecture : schedule.values()){
            if(lecture.room.equals(room) && lecture.time.equals(time)){
                return "ERROR|Room Clash: " + room + " is already book at " + time + " by " + lecture.module;
            }
        }
        Lecture lecture = new Lecture(date, time, room, module);
        schedule.put(key, lecture);
        modules.add(module);
        return "OK|Lecture added: " + module + " in " + room + " on " + date + " at " + time;
    }
    private synchronized String removeLecture(String[] parts) throws IncorrectActionException {
        if(parts.length < 3) throw new IncorrectActionException("REMOVE requires Date and Time");
        String date = parts[1];
        String time = parts[2];

        String key = date + "-" + time;
        if (schedule.remove(key) != null) {
            return "OK|Lecture removed";
        }
        return "ERROR|No Lecture found to be removed";
    }
    private synchronized String displaySchedule() {
        if(schedule.isEmpty()){
            return "OK|No schedule found";
        }

        StringBuilder builder = new StringBuilder("OK|");
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
    public static void main(String[] args){
        _24410764_24436739_Server server = new _24410764_24436739_Server();
        server.startServer();
    }
}
