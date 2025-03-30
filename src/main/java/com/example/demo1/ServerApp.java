package com.example.demo1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ServerApp extends Application {
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final TextArea logArea = new TextArea();
    private final Map<String, Socket> clients = new HashMap<>();
    private final Label clientCountLabel = new Label("Connected clients: 0");

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("AES Transfer Server");
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/server.png")));
        } catch (Exception e) {
            System.err.println("Could not load logo: " + e.getMessage());
        }

        Scene scene = createLoginScene(primaryStage);
        applyCss(scene);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private Scene createLoginScene(Stage stage) {
        // Header with logo and title
        ImageView logoView = new ImageView();
        try {
            Image logo = new Image(getClass().getResourceAsStream("/logo.png"));
            logoView.setImage(logo);
            logoView.setFitHeight(60);
            logoView.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Could not load logo: " + e.getMessage());
        }

        Label titleLabel = new Label("AES TRANSFER SERVER");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#2c3e50"));

        VBox headerBox = new VBox(5, logoView, titleLabel);
        headerBox.setAlignment(Pos.CENTER);

        // Form fields
        Label ipLabel = new Label("Server IP:");
        ipLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        TextField ipField = new TextField("127.0.0.1");
        ipField.setPromptText("Enter server IP address");
        ipField.setPrefHeight(35);

        Label portLabel = new Label("Server Port:");
        portLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        TextField portField = new TextField("5000");
        portField.setPromptText("Enter port number");
        portField.setPrefHeight(35);

        Button startButton = new Button("START SERVER");
        startButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        startButton.setPrefHeight(40);
        startButton.setPrefWidth(200);
        startButton.setOnAction(e -> {
            try {
                String ip = ipField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());
                startServer(ip, port);

                Scene logScene = createLogScene();
                applyCss(logScene);
                stage.setScene(logScene);
                stage.setResizable(true);
            } catch (NumberFormatException ex) {
                showAlert("Error", "Port must be a valid integer!");
            }
        });

        VBox formBox = new VBox(10, ipLabel, ipField, portLabel, portField);
        formBox.setPadding(new Insets(20, 20, 10, 20));

        VBox layout = new VBox(20, headerBox, formBox, startButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f5f6fa;");

        return new Scene(layout, 400, 450);
    }

    private Scene createLogScene() {
        // Tạo các thành phần chính
        Label titleLabel = new Label("SERVER LOGS");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web("#2c3e50"));

        // Label hiển thị số client
        clientCountLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        clientCountLabel.setTextFill(Color.web("#3498db"));
        clientCountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #3498db; -fx-font-size: 14px;");

        // Tạo HBox để chứa tiêu đề và số client trên cùng 1 hàng
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getChildren().add(titleLabel);

        // Thêm khoảng cách co giãn để đẩy clientCountLabel sang phải
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(spacer, clientCountLabel);
        headerBox.setPadding(new Insets(0, 10, 5, 10)); // Thêm padding nếu cần

        // Log area với styling
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-control-inner-background: #2c3e50; -fx-text-fill: #ecf0f1; -fx-font-family: 'Consolas';");
        logArea.setPrefHeight(400);

        ScrollPane scrollPane = new ScrollPane(logArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: #2c3e50; -fx-border-color: #2c3e50;");

        // Các nút
        Button clearLogButton = new Button("Clear Logs");
        clearLogButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        clearLogButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/clear.png"), 16, 16, true, true)));
        clearLogButton.setOnAction(e -> logArea.clear());

        Button stopButton = new Button("Stop Server");
        stopButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        stopButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/stop.png"), 16, 16, true, true)));
        stopButton.setOnAction(e -> stopServer());

        HBox buttonBox = new HBox(15, clearLogButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        // Status bar
        Label statusLabel = new Label("Server running...");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        statusLabel.setTextFill(Color.web("#27ae60"));
        statusLabel.setPadding(new Insets(5));

        BorderPane statusPane = new BorderPane();
        statusPane.setCenter(statusLabel);
        statusPane.setStyle("-fx-background-color: #ecf0f1;");

        // Main layout
        VBox layout = new VBox(10, headerBox, scrollPane, buttonBox, statusPane);
        layout.setPadding(new Insets(10));
        layout.setStyle("-fx-background-color: #f5f6fa;");

        return new Scene(layout, 600, 500);
    }
    private void startServer(String ip, int port) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                isRunning = true;
                appendLog("Server started successfully on " + ip + ":" + port);
                appendLog("Waiting for client connections...");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                appendLog("Server error: " + e.getMessage());
            }
        }).start();
    }

    private void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            clients.clear();
            updateClientCount();
        } catch (IOException e) {
            appendLog("Error closing server: " + e.getMessage());
        }
        appendLog("Server stopped");

        Platform.runLater(() -> {
            Stage stage = (Stage) logArea.getScene().getWindow();
            Scene loginScene = createLoginScene(stage);
            applyCss(loginScene);
            stage.setScene(loginScene);
            stage.setResizable(false);
        });
    }

    private void updateClientCount() {
        Platform.runLater(() -> {
            clientCountLabel.setText("Connected clients: " + clients.size());
        });
    }

    private void handleClient(Socket socket) {
        String username = null;
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            String message = input.readUTF().trim();
            if (message.startsWith("LOGIN:")) {
                username = message.substring(6).trim();

                clients.put(username, socket);
                appendLog("Client connected: " + username + " (" + socket.getInetAddress() + ")");
                updateClientCount();
                output.writeUTF("OK: Login successful!");

                // Thêm phần theo dõi kết nối
                while (true) {
                    try {
                        // Đọc tin nhắn từ client để phát hiện ngắt kết nối
                        message = input.readUTF();

                        // Nếu yêu cầu danh sách client
                        if (message.equals("LIST_CLIENTS")) {
                            String clientList = String.join(",", clients.keySet());
                            output.writeUTF(clientList.isEmpty() ? "NO_CLIENTS" : clientList);
                        }

                    } catch (IOException e) {
                        // Client đã ngắt kết nối
                        break;
                    }
                }
            }
        } catch (IOException e) {
            appendLog("Client connection error: " + e.getMessage());
        } finally {
            if (username != null) {
                clients.remove(username);
                appendLog("Client disconnected: " + username);
                updateClientCount();
            }
            try {
                socket.close();
            } catch (IOException e) {
                appendLog("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void appendLog(String text) {
        Platform.runLater(() -> {
            String formattedTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
            logArea.appendText("[" + formattedTime + "] " + text + "\n");
        });
    }

    private void applyCss(Scene scene) {
        try {
            String css = getClass().getResource("/style.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
            // Fallback inline styling
            scene.getRoot().setStyle("-fx-font-family: 'Arial';");
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Custom dialog pane
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: #f5f6fa;");
            dialogPane.setHeader(new Label(title));

            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}