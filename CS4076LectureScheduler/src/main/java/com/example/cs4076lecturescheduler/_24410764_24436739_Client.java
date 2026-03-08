package com.example.cs4076lecturescheduler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     private TableView<Row> table;

     private final ServerSim server = new ServerSim("LM021-2026");
     private boolean stopped = false;


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
         root.setCenter(buildTable());
         root.setBottom(buildLog());

         Scene scene = new Scene(root, 980, 650);
         stage.setTitle("Lecture Scheduler Client ");
         stage.setScene(scene);
         stage.show();

         refreshTableFromServer();
     }

     private Node buildForm() {
         actionBox = new ComboBox<>(FXCollections.observableArrayList("ADD", "REMOVE", "DISPLAY", "OTHER"));
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
         TableColumn<Row, String> cDate = new TableColumn<>("Date");
         cDate.setCellValueFactory(d -> d.getValue().date);
         TableColumn<Row, String> cTime = new TableColumn<>("Time");
         cTime.setCellValueFactory(d -> d.getValue().time);
         TableColumn<Row, String> cRoom = new TableColumn<>("Room");
         cRoom.setCellValueFactory(d -> d.getValue().room);

         TableColumn<Row, String> cModule = new TableColumn<>("Module");
         cModule.setCellValueFactory(d -> d.getValue().module);
         table.getColumns().addAll(cDate, cTime, cRoom, cModule);
         table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
         VBox center = new VBox(8, new Label("Schedule (TableView)"), table);
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

         log("CLIENT> " + request);
         String response = server.handle(request);
         log("SERVER> " + response);

         if (response.startsWith("OK|")) {
             statusLabel.setText("Status: OK");
         } else if (response.startsWith("ERROR|")) {
             statusLabel.setText("Status: ERROR");
         } else if (response.startsWith("TERMINATE|")) {
             statusLabel.setText("Status: TERMINATED");
             stopped = true;
             sendBtn.setDisable(true);
         }


         refreshTableFromServer();
     }

     private void onClear() {
         roomField.clear();
         moduleField.clear();
         datePicker.setValue(null);
         actionBox.getSelectionModel().selectFirst();
         timeBox.getSelectionModel().selectFirst();
         stopped = false;
         sendBtn.setDisable(false);
         statusLabel.setText("Status: Ready");

         log("--- cleared ---");
     }

     private void onStop() {
         if (stopped) return;
         String request = "STOP||||";
         log("CLIENT> " + request);
         String response = server.handle(request);
         log("SERVER> " + response);
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
         if ("ADD".equals(action)) {
             if (d.isEmpty() || t.isEmpty() || r.isEmpty() || m.isEmpty()) {
                 alertWarn("ADD needs Date, Time, Room and Module.");
                 return "DISPLAY||||";
             }
             return "ADD|" + d + "|" + t + "|" + r + "|" + m;
         }

         if ("REMOVE".equals(action)) {
             if (d.isEmpty() || t.isEmpty()) {
                 alertWarn("REMOVE needs Date and Time.");
                 return "DISPLAY||||";
             }
             return "REMOVE|" + d + "|" + t + "||";
         }
         return "OTHER||||";
     }

     public static class Lecture {
         final LocalDate date;
         final String time;
         final String room;
         final String module;

         Lecture(LocalDate date, String time, String room, String module) {
             this.date = date;
             this.time = time;
             this.room = room;
             this.module = module;
         }

         String slotKey() {
             return date + "|" + time;
         }

         @Override
         public String toString() {
             return date + " " + time + " Room " + room + " (" + module + ")";
         }
     }

     private void refreshTableFromServer() {
         List<Lecture> lectures = server.getAllLecturesSorted();
         List<Row> rows = new ArrayList<>();
         for (Lecture l : lectures) {
             rows.add(new Row(l.date.toString(), l.time, l.room, l.module));
         }
         table.setItems(FXCollections.observableArrayList(rows));
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


     public static class Row {
         final SimpleStringProperty date;
         final SimpleStringProperty time;
         final SimpleStringProperty room;
         final SimpleStringProperty module;

         Row(String date, String time, String room, String module) {
             this.date = new SimpleStringProperty(date);
             this.time = new SimpleStringProperty(time);
             this.room = new SimpleStringProperty(room);
             this.module = new SimpleStringProperty(module);
         }
     }

     public static class ServerSim {
         private final String courseCode;
         private final Map<String, Lecture> schedule = new HashMap<>();

         public ServerSim(String courseCode) {
             this.courseCode = courseCode;
         }

         public String handle(String request) {
             try {
                 String[] parts = request.split("\\|", -1);
                 String action = parts[0].trim().toUpperCase();
                 if ("STOP".equals(action)) {
                     return "TERMINATE|Server confirms termination.";
                 }
                 if ("DISPLAY".equals(action)) {
                     return "OK|Displayed schedule.";
                 }
                 if ("ADD".equals(action)) {
                     LocalDate date = LocalDate.parse(parts[1]);
                     String time = parts[2].trim();
                     String room = parts[3].trim();
                     String module = parts[4].trim();

                     Lecture newL = new Lecture(date, time, room, module);
                     String key = newL.slotKey();

                     schedule.put(key, newL);
                     return "OK|Added: " + newL;
                 }

                 if ("REMOVE".equals(action)) {
                     LocalDate date = LocalDate.parse(parts[1]);
                     String time = parts[2].trim();
                     String key = date + "|" + time;

                     Lecture removed = schedule.remove(key);
                     if (removed == null) {
                         return "ERROR|No lecture found at " + date + " " + time + " to remove.";
                     }
                     return "OK|Removed: " + removed;
                 }


             } catch (Exception e) {
                 return "ERROR|Bad request: " + e.getMessage();
             }
             return null;
         }

         public List<Lecture> getAllLecturesSorted() {
             List<Lecture> all = new ArrayList<>(schedule.values());
             all.sort(Comparator.comparing((Lecture l) -> l.date).thenComparing(l -> l.time));
             return all;
         }
     }

     public static void main(String[] args) {
         launch(args);
     }
 }

