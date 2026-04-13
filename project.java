package database;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class project extends Application {

    private Stage primaryStage;
    private TextField unitsField = new TextField();
    private TextField emailField = new TextField();
    private TableView<BillRecord> table = new TableView<>();
    private ObservableList<BillRecord> data = FXCollections.observableArrayList();
    
    private final String DB_URL = "jdbc:mysql://localhost:3306/eb_system";
    private final String DB_USER = "root"; 
    private final String DB_PASS = "Sara";

    // Admin Credentials
    private final String ADMIN_USER = "admin";
    private final String ADMIN_PASS = "12345";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showLoginScreen();
    }

    // --- 1. LOGIN SCREEN ---
    private void showLoginScreen() {
        VBox loginBox = new VBox(15);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(50));
        loginBox.setStyle("-fx-background-color: #ecf0f1;");

        Label loginTitle = new Label("ADMIN LOGIN");
        loginTitle.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        TextField userIn = new TextField();
        userIn.setPromptText("Username");
        userIn.setMaxWidth(200);

        PasswordField passIn = new PasswordField();
        passIn.setPromptText("Password");
        passIn.setMaxWidth(200);

        Button loginBtn = new Button("LOGIN");
        loginBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;");
        
        loginBtn.setOnAction(e -> {
            if (userIn.getText().equals(ADMIN_USER) && passIn.getText().equals(ADMIN_PASS)) {
                showDashboard();
            } else {
                showAlert("Access Denied", "Incorrect Username or Password!", Alert.AlertType.ERROR);
            }
        });

        loginBox.getChildren().addAll(loginTitle, userIn, passIn, loginBtn);
        primaryStage.setScene(new Scene(loginBox, 400, 300));
        primaryStage.setTitle("EB System - Authentication");
        primaryStage.show();
    }

    // --- 2. MAIN DASHBOARD ---
    private void showDashboard() {
        Label title = new Label("EB BILLING SYSTEM");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        
        unitsField.setPromptText("Units Consumed");
        emailField.setPromptText("Email Address");

        Button btn = new Button("GENERATE & SEND BILL");
        btn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btn.setOnAction(e -> generateBill());

        // Logout Button
        Button logoutBtn = new Button("LOGOUT");
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        logoutBtn.setOnAction(e -> showLoginScreen());

        VBox inputArea = new VBox(15, title, new Label("Enter Details:"), unitsField, emailField, btn, logoutBtn);
        inputArea.setPadding(new Insets(20));
        inputArea.setPrefWidth(300);

        TableColumn<BillRecord, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<BillRecord, Integer> colUnits = new TableColumn<>("Units");
        colUnits.setCellValueFactory(new PropertyValueFactory<>("units"));

        TableColumn<BillRecord, String> colUsage = new TableColumn<>("Usage Status");
        colUsage.setCellValueFactory(new PropertyValueFactory<>("usage"));

        table.getColumns().clear(); // Reset columns
        table.getColumns().addAll(colEmail, colUnits, colUsage);

        HBox mainLayout = new HBox(20, inputArea, table);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #f4f7f6;");

        primaryStage.setScene(new Scene(mainLayout, 850, 500));
        primaryStage.setTitle("EB Management Dashboard");
    }

    // --- (Keep generateBill, saveToDatabase, sendEmail, showAlert methods the same as your original code) ---
    
    private void generateBill() {
        String unitStr = unitsField.getText();
        String toEmail = emailField.getText();

        if (unitStr.isEmpty() || toEmail.isEmpty()) {
            showAlert("Error", "All fields required!", Alert.AlertType.ERROR);
            return;
        }

        try {
            int units = Integer.parseInt(unitStr);
            double rate = (units <= 100) ? 0 : (units <= 300) ? 4.5 : 7.0;
            double total = units * rate;

            String usageStatus = (units > 500) ? "HIGH" : "NORMAL";

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dueDate = now.plusDays(7);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            String formattedDueDate = dueDate.format(formatter);

            String billDetails = "Dear Consumer,\n\n" +
                                 "Your EB Bill is generated.\n" +
                                 "Units: " + units + "\n" +
                                 "Total: Rs. " + total + "\n" +
                                 "Usage: " + usageStatus + "\n" +
                                 "DUE DATE: " + formattedDueDate + "\n\n" +
                                 "Regards,\nElectricity Dept.";

            if (sendEmail(toEmail, billDetails, usageStatus)) {
                saveToDatabase(toEmail, units, total);
                data.add(new BillRecord(toEmail, units, usageStatus));
                table.setItems(data);
                showAlert(usageStatus, "Bill Sent! Status: " + usageStatus, Alert.AlertType.INFORMATION);
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid units!", Alert.AlertType.ERROR);
        }
    }

    private void saveToDatabase(String email, int units, double cost) {
        String sql = "INSERT INTO bills (citizen_email, units, total_cost) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setInt(2, units);
            pstmt.setDouble(3, cost);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean sendEmail(String toEmail, String messageText, String usage) {
        final String fromEmail = "mpreethi19782006@gmail.com";
        final String password = "dmqdtlfkgxizgrog"; 
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("EB Bill Notification - Usage: " + usage);
            message.setText(messageText);
            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    public static class BillRecord {
        private final String email;
        private final int units;
        private final String usage;

        public BillRecord(String email, int units, String usage) {
            this.email = email;
            this.units = units;
            this.usage = usage;
        }
        public String getEmail() { return email; }
        public int getUnits() { return units; }
        public String getUsage() { return usage; }
    }
}