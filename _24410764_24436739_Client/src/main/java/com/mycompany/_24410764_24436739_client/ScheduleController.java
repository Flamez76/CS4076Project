package com.mycompany._24410764_24436739_client;

import java.io.*;
import java.net.*;
import javafx.application.Platform;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import javafx.concurrent.Task;

public class ScheduleController {
    private _24410764_24436739_Client view;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean stopped = false;

    public ScheduleController(_24410764_24436739_Client view) {
        this.view = view;
    }

    public void connectToServer() {
        view.sendBtn.setDisable(true);
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 5000);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                Platform.runLater(() -> {
                    view.log("Connected to server");
                    view.sendBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> view.log("Connection failed: " + e.getMessage()));
            }
        }).start();
    }

    public void onStop() {
        if (stopped) return;
        String request = "STOP||||";
        view.log("CLIENT> " + request);
        out.println(request);
        try {
            String response = in.readLine();
            view.log("SERVER> " + response);
        }  catch(Exception e){
            view.log("Error recieving response");
        }
        stopped = true;
        view.sendBtn.setDisable(true);
        view.statusLabel.setText("Status: TERMINATED (STOP pressed)");
    }

    public void reset(){
        stopped = false;
        if(out != null){
            view.sendBtn.setDisable(false);
        }
    }

    private String buildRequest(String action, LocalDate date, String time, String room, String module) {

        String d = (date == null) ? "" : date.toString();
        String t = (time == null) ? "" : time.trim();
        String r = (room == null) ? "" : room.trim();
        String m = (module == null) ? "" : module.trim();

        if ("DISPLAY".equals(action)) return "DISPLAY||||";
        if ("OTHER".equals(action)) return "OTHER||||";
        if("EARLY".equals(action)) return "EARLY||||";
        if ("ADD".equals(action)) {
            if (d.isEmpty() || t.isEmpty() || r.isEmpty() || m.isEmpty()) {
                view.alertWarn("ADD needs Date, Time, Room and Module.");
                return null;
            }
            return "ADD|" + d + "|" + t + "|" + r + "|" + m;
        }

        if ("REMOVE".equals(action)) {
            if (d.isEmpty() || t.isEmpty()) {
                view.alertWarn("REMOVE needs Date and Time.");
                return null;
            }
            return "REMOVE|" + d + "|" + t + "||";
        }
        return null;
    }

    public void onSend() {
        if (stopped) {
            view.alertInfo("The connection is stopped. Press Clear to reset.");
            return;
        }

        String action = view.actionBox.getValue();
        LocalDate date = view.datePicker.getValue();
        String time = view.timeBox.getValue();
        String room = view.roomField.getText();
        String module = view.moduleField.getText();

        String request = buildRequest(action, date, time, room, module);
        if(request == null) return;
        if("EARLY".equals(action)) {
            view.statusLabel.setText("Status: Processing...");
            view.sendBtn.setDisable(true);

            Task<String> earlyTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    out.println(request);
                    return in.readLine();
                }
            };

            earlyTask.setOnSucceeded(e -> {
                String response = earlyTask.getValue();
                view.log("SERVER> " + response);
                view.sendBtn.setDisable(false);
                if(response.startsWith("OK|")) {
                    view.statusLabel.setText("Status: OK");
                    parseAndDisplaySchedule(response.substring(3));
                } else if(response.startsWith("ERROR|")) {
                    view.statusLabel.setText("Status: ERROR");
                    view.alertWarn("Server Error: " + response.substring(6));
                }
            });

            earlyTask.setOnFailed(e -> {
                view.log("Communication error: " + earlyTask.getException().getMessage());
                view.sendBtn.setDisable(false);
                view.statusLabel.setText("Status: ERROR");
            });

            view.log("CLIENT> " + request);
            new Thread(earlyTask).start();
        } else {
            try {
                view.log("CLIENT> " + request);
                out.println(request);
                String response = in.readLine();
                view.log("SERVER> " + response);
                if(response.startsWith("OK|")){
                    String payload = response.substring(3);
                    view.statusLabel.setText("Status: OK|");
                    if("DISPLAY".equals(action)) {
                        parseAndDisplaySchedule(payload);
                    } else {
                        view.alertInfo("Success: " + payload);
                    }
                } else if (response.startsWith("ERROR|")) {
                    String errorMsg = response.substring(6);
                    view.statusLabel.setText("Status: ERROR");
                    view.alertWarn("Server Error: " + errorMsg);
                } else if(response.startsWith("TERMINATE|")) {
                    view.statusLabel.setText("Status: TERMINATED");
                    stopped = true;
                    view.sendBtn.setDisable(true);
                }
            } catch(Exception e) {
                view.log("Communication error: " + e.getMessage());
            }
        }
    }
    void parseAndDisplaySchedule(String payload) {
        for(String[] row: view.table.getItems()){
            for(int i = 1; i < 6; i++) row[i] = "";
        }
        view.table.refresh();
        if(payload.equals("No schedule found")) {
            view.alertInfo("No schedule found");
            return;
        }
        String[] entries = payload.split(";");
        for(String entry : entries){
            if(entry.isEmpty()) continue;
            String[] fields = entry.split(",");
            if(fields.length == 4){
                String date  = fields[0];
                String time = fields[1];
                String room = fields[2];
                String module = fields[3];

                LocalDate localDate = LocalDate.parse(date);
                String day = localDate.getDayOfWeek().getDisplayName(
                        TextStyle.FULL,
                        Locale.ENGLISH
                );
                int colIndex = switch (day){
                    case "Monday" -> 1;
                    case "Tuesday" -> 2;
                    case "Wednesday" -> 3;
                    case "Thursday" -> 4;
                    case "Friday" -> 5;
                    default -> -1;
                };

                if(colIndex == -1) continue;
                for(String[] row: view.table.getItems()){
                    if(row[0].equals(time)){
                        row[colIndex] = module + "\n" + room;
                        break;
                    }
                }
            }
        }
        view.table.refresh();
    }
}
