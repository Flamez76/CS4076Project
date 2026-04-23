package com.mycompany._24410764_24436739_client;

import javafx.application.Application;
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
    ComboBox<String> actionBox;
    DatePicker datePicker;
    ComboBox<String> timeBox;
    TextField roomField;
    TextField moduleField;
    Button sendBtn;
    Button clearBtn;
    Button stopBtn;
    TextArea logArea;
    Label statusLabel;
    TableView<String[]> table;
    private ScheduleController controller = new ScheduleController(this);


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
        controller.connectToServer();
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


        sendBtn.setOnAction(e -> controller.onSend());
        stopBtn.setOnAction(e -> controller.onStop());
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
        table.setMaxHeight(160);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox center = new VBox (8, new Label("Schedule (Lm110)"),  table);
        center.setPadding(new Insets(12));
        return center;
    }

    private void onClear() {
        roomField.clear();
        moduleField.clear();
        datePicker.setValue(null);
        actionBox.getSelectionModel().selectFirst();
        timeBox.getSelectionModel().selectFirst();
        controller.reset();
        statusLabel.setText("Status: Ready");
        log("--- cleared ---");
    }



    void log(String msg) {
        logArea.appendText(msg + System.lineSeparator());
    }

    void alertWarn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText("Validation");
        a.showAndWait();
    }

    void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText("Info");
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}