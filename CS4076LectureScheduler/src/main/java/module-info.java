module com.example.cs4076lecturescheduler {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.cs4076lecturescheduler to javafx.fxml;
    exports com.example.cs4076lecturescheduler;
}