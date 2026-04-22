package com.example.cs4076lecturescheduler;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javafx.application.Platform;

public class _24410764_24436739_Client extends Application {
    private ComboBox<String> actionBox;
    private DatePicker datePicker;
    private ComboBox<String> timeBox;
    private TextField roomField;
    private TextField moduleField;
    private Button sendBtn;
    private Button clearBtn;
    private Button stopBtn;
    private TextArea logArea;
    private Label statusLabel;
    private TableView<String[]> table;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean stopped = false;


    public void connectToServer() {
        sendBtn.setDisable(true);
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 5000);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                Platform.runLater(() -> {
                    log("Connected to server");
                    sendBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> log("Connection failed: " + e.getMessage()));
            }
        }).start();
    }

    private Node buildHeader() {
        Label title = new Label("Lecture Scheduler Client ");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        HBox header = new HBox(title);
        header.setPadding(new Insets(12));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #f2f2f2;");
        return header;
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        root.setTop(buildHeader());
        root.setLeft(buildForm());
        root.setBottom(buildTable());
        root.setCenter(buildLog());

        Scene scene = new Scene(root, 980, 650);
        stage.setTitle("Lecture Scheduler Client ");
        stage.setScene(scene);
        stage.show();
        connectToServer();
    }

    private Node buildForm() {
        actionBox = new ComboBox<>(FXCollections.observableArrayList("ADD", "REMOVE", "DISPLAY","EARLY","OTHER"));
        actionBox.getSelectionModel().selectFirst();
        datePicker = new DatePicker();
        timeBox = new ComboBox<>(FXCollections.observableArrayList(
                "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
                "14:00-15:00", "15:00-16:00", "16:00-17:00", "17:00-18:00"
        ));
        timeBox.getSelectionModel().selectFirst();

        roomField = new TextField();
        roomField.setPromptText("e.g., CSG001");
        moduleField = new TextField();
        moduleField.setPromptText("e.g., CS4706");

        sendBtn = new Button("Send Request");
        stopBtn = new Button("STOP");
        clearBtn = new Button("Clear");

        statusLabel = new Label("Status: Ready");

        sendBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setPadding(new Insets(12));
        form.setHgap(10);
        form.setVgap(10);

        int r = 0;
        form.add(new Label("Action:"), 0, r);
        form.add(actionBox, 1, r++);
        form.add(new Label("Date:"), 0, r);
        form.add(datePicker, 1, r++);
        form.add(new Label("TimeSlot:"), 0, r);
        form.add(timeBox, 1, r++);
        form.add(new Label("Room:"), 0, r);
        form.add(roomField, 1, r++);
        form.add(new Label("Module:"), 0, r);
        form.add(moduleField, 1, r++);

        VBox buttons = new VBox(8, sendBtn, stopBtn, clearBtn, statusLabel);
        buttons.setPadding(new Insets(12, 0, 0, 0));

        VBox left = new VBox(8, new Label("Request Builder"), form, buttons);
        left.setPadding(new Insets(12));
        left.setPrefWidth(360);
        left.setStyle("-fx-border-color: #dddddd; -fx-border-width: 0 1 0 0;");


        sendBtn.setOnAction(e -> onSend());
        stopBtn.setOnAction(e -> onStop());
        clearBtn.setOnAction(e -> onClear());


        actionBox.valueProperty().addListener((obs, oldV, newV) -> {
            boolean needsDateTime = "ADD".equals(newV) || "REMOVE".equals(newV);
            datePicker.setDisable(!needsDateTime);
            timeBox.setDisable(!needsDateTime);
            roomField.setDisable(!"ADD".equals(newV));
            moduleField.setDisable(!"ADD".equals(newV));
        });

        return left;
    }

    private Node buildLog() {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(6);

        VBox bottom = new VBox(6, new Label("Conversation Log (Client to Server)"), logArea);
        bottom.setPadding(new Insets(12));
        bottom.setStyle("-fx-background-color: #fafafa; -fx-border-color: #dddddd; -fx-border-width: 1 0 0 0;");
        return bottom;
    }

    private Node buildTable() {
        table = new TableView<>();
        TableColumn<String[], String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[0]));
        timeCol.setPrefWidth(120);
        table.getColumns().add(timeCol);

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        for(int i = 0; i < days.length; i++){
            int col = i + 1;
            TableColumn<String[], String> dayCol = new TableColumn<>(days[i]);
            dayCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[col]));
            dayCol.setPrefWidth(150);
            table.getColumns().add(dayCol);
        }

        String[] slots = {
                "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
                "14:00-15:00",  "15:00-16:00", "16:00-17:00", "17:00-18:00"
        };
        for(String slot : slots){
            String[] row = new String[6];
            row[0] = slot;
            for(int i = 1; i < 6; i++) row[i] = "";
            table.getItems().add(row);
        }
        table.setMaxHeight(180);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox center = new VBox (8, new Label("Schedule (Lm110)"),  table);
        center.setPadding(new Insets(12));
        return center;
    }


    private void onSend() {
        if (stopped) {
            alertInfo("The connection is stopped. Press Clear to reset.");
            return;
        }

        String action = actionBox.getValue();
        LocalDate date = datePicker.getValue();
        String time = timeBox.getValue();
        String room = roomField.getText();
        String module = moduleField.getText();

        String request = buildRequest(action, date, time, room, module);
        if(request == null) return;
        if("EARLY".equals(action)) {
            statusLabel.setText("Status: Processing...");
            sendBtn.setDisable(true);
            new Thread(() -> {
                try {
                    log("CLIENT> " + request);
                    out.println(request);
                    String response = in.readLine();
                    Platform.runLater(() -> {
                        log("SERVER> " + response);
                        sendBtn.setDisable(false);
                        if (response.startsWith("OK|")) {
                            String payload = response.substring(3);
                            statusLabel.setText("Status: OK");
                            parseAndDisplaySchedule(payload);
                        } else if (response.startsWith("ERROR|")) {
                            statusLabel.setText("Status: ERROR|");
                            alertWarn("Server Error: " + response.substring(6));
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        log("Communication error: " + e.getMessage());
                        sendBtn.setDisable(false);
                    });
                }
            }).start();
        } else {
            try {
                log("CLIENT> " + request);
                out.println(request);
                String response = in.readLine();
                log("SERVER> " + response);
                if(response.startsWith("OK|")){
                    String payload = response.substring(3);
                    statusLabel.setText("Status: OK|");
                    if("DISPLAY".equals(action) || "EARLY".equals(action)) {
                        parseAndDisplaySchedule(payload);
                    } else {
                        alertInfo("Success: " + payload);
                    }
                } else if (response.startsWith("ERROR|")) {
                    String errorMsg = response.substring(6);
                    statusLabel.setText("Status: ERROR");
                    alertWarn("Server Error: " + errorMsg);
                } else if(response.startsWith("TERMINATE|")) {
                    statusLabel.setText("Status: TERMINATED");
                    stopped = true;
                    sendBtn.setDisable(true);
                }
            } catch(Exception e) {
                log("Communication error: " + e.getMessage());
            }
        }
    }

    private void parseAndDisplaySchedule(String payload) {
        for(String[] row: table.getItems()){
            for(int i = 1; i < 6; i++) row[i] = "";
        }
        table.refresh();
        if(payload.equals("No schedule found")) {
            alertInfo("No schedule found");
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
                for(String[] row: table.getItems()){
                    if(row[0].equals(time)){
                        row[colIndex] = module + "\n" + room;
                        break;
                    }
                }
            }
        }
        table.refresh();
    }

    private void onClear() {
        roomField.clear();
        moduleField.clear();
        datePicker.setValue(null);
        actionBox.getSelectionModel().selectFirst();
        timeBox.getSelectionModel().selectFirst();
        stopped = false;
        if(out != null) {
            sendBtn.setDisable(false);
        }
        statusLabel.setText("Status: Ready");

        log("--- cleared ---");
    }

    private void onStop() {
        if (stopped) return;
        String request = "STOP||||";
        log("CLIENT> " + request);
        out.println(request);
        try {
            String response = in.readLine();
            log("SERVER> " + response);
        }  catch(Exception e){
            log("Error recieving response");
        }
        stopped = true;
        sendBtn.setDisable(true);
        statusLabel.setText("Status: TERMINATED (STOP pressed)");
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
                alertWarn("ADD needs Date, Time, Room and Module.");
                return null;
            }
            return "ADD|" + d + "|" + t + "|" + r + "|" + m;
        }

        if ("REMOVE".equals(action)) {
            if (d.isEmpty() || t.isEmpty()) {
                alertWarn("REMOVE needs Date and Time.");
                return null;
            }
            return "REMOVE|" + d + "|" + t + "||";
        }
        return null;
    }

    private void log(String msg) {
        logArea.appendText(msg + System.lineSeparator());
    }

    private void alertWarn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText("Validation");
        a.showAndWait();
    }

    private void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText("Info");
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
