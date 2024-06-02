package com.example.tpv_2024.Controladores;

import com.example.tpv_2024.Modelos.Modelo;
import com.example.tpv_2024.Servicio.RestClientService;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class LoginControlador implements Initializable {


    public Label usuario_lbl;
    public TextField usuario_fld;
    public Label pwd_lbl;
    public TextField pwd_fld;
    public Button login_btn;
    public Label error_lbl;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        login_btn.setOnAction(e -> onLogin());
    }

    private void onLogin(){
        if (usuario_fld.getText().isEmpty() || pwd_fld.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error al iniciar sesión", "Por favor, rellene los campos vacíos");
            return;
        }

        String hashedPassword = hashPassword(pwd_fld.getText());

        try {
            // Crear el objeto JSON para la petición
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("idEmpleado", usuario_fld.getText());
            loginRequest.put("contrasena", hashedPassword);

            // Realizar la petición POST para la autenticación
            String response = RestClientService.post("login", loginRequest);

            // Procesar la respuesta
            if (response.equals("success")) {
                // Lógica para iniciar sesión exitosa
                System.out.println("Usuario y contraseña correctos");
                Stage stage = (Stage) error_lbl.getScene().getWindow();
                Modelo.getInstance().getViewFactory().CloseStage(stage);
                Modelo.getInstance().getViewFactory().showClientWindow();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error al iniciar sesión", "Usuario y/o contraseña incorrectos");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Ha ocurrido un error: " + e.getMessage());
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Pestaña con el contacto del soporte
    public void soporte() {
        showAlert(Alert.AlertType.INFORMATION, "Soporte", "Contacte con el soporte técnico por email: dichaowang107@gmail.com");
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
