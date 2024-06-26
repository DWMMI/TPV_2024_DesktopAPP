package com.example.tpv_2024.Controladores.Cliente;

import com.example.tpv_2024.Controladores.SessionManager;
import com.example.tpv_2024.Servicio.ProductoService;
import com.example.tpv_2024.Modelos.Ventas;
import com.example.tpv_2024.Servicio.VentasService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class VentasControlador implements Initializable {
    @FXML
    public TableView<Ventas> productTable;
    @FXML
    public TableColumn<Ventas, String> barcodeColumn;
    @FXML
    public TableColumn<Ventas, Integer> quantityColumn;
    @FXML
    public TableColumn<Ventas, Double> productPriceColumn;
    @FXML
    public TableColumn<Ventas, String> productNameColumn;
    @FXML
    public TableColumn<Ventas, Double> totalColumn;
    @FXML
    public Label totalQuantityLabel;
    @FXML
    public Label totalLabel;
    @FXML
    public TextField TF_Cantidad;
    @FXML
    public TextField TF_Cod_Barra;
    @FXML
    public TextField TF_Nombre;
    @FXML
    public TextField TF_Precio;
    @FXML
    public TextField TF_ID_Cliente;
    @FXML
    public Button agregar_btn;
    @FXML
    public Button pagar_btn;
    @FXML
    private RadioButton rbt_efectivo;
    @FXML
    private RadioButton rbt_visa;
    @FXML
    private TextField tf_EnEfectivo;
    @FXML
    private Label l_cambios;
    @FXML
    private CheckBox chBox_socio;

    public ObservableList<Ventas> productos;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        productos = FXCollections.observableArrayList();

        this.productNameColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        this.barcodeColumn.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        this.productPriceColumn.setCellValueFactory(new PropertyValueFactory<>("precio"));
        this.quantityColumn.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        this.totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));

        this.quantityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        this.productPriceColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        this.totalColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));

        this.productTable.setItems(productos);

        // En javaFX, los radioButtons no tienen un grupo por defecto, por lo que se debe hacer manualmente
        ToggleGroup group = new ToggleGroup();
        this.rbt_efectivo.setToggleGroup(group);
        this.rbt_visa.setToggleGroup(group);

        // Seleccionar efectivo por defecto
        this.rbt_efectivo.setSelected(true);

        // Configurar TextFormatter para tf_EnEfectivo
        Pattern validEditingState = Pattern.compile("-?((\\d*)|(\\d+\\.\\d*))");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (validEditingState.matcher(text).matches()) {
                return change;
            } else {
                return null;
            }

        };
        TextFormatter<Double> textFormatter = new TextFormatter<>(new DoubleStringConverter(), null, filter);
        tf_EnEfectivo.setTextFormatter(textFormatter);

        // Añadir evento para Enter
        tf_EnEfectivo.setOnAction(this::calcularCambios);

        // Añadir listener para el campo del código de barra
        TF_Cod_Barra.setOnAction(event -> comprobarProducto(TF_Cod_Barra.getText()));

        // Agregar listener para la columna de cantidad
        quantityColumn.setOnEditCommit(event -> {
            Ventas producto = event.getRowValue();
            int nuevaCantidad = event.getNewValue();
            producto.setCantidad(nuevaCantidad);
            double nuevoTotal = producto.getPrecio() * nuevaCantidad;
            producto.setTotal(nuevoTotal);
            productTable.refresh(); // Actualizar la tabla
            updateTotal();
            updateCantidad();
        });

        // Agregar listener para la columna de precio
        productPriceColumn.setOnEditCommit(event -> {
            Ventas producto = event.getRowValue();
            double nuevoPrecio = event.getNewValue();
            producto.setPrecio(nuevoPrecio);
            double nuevoTotal = nuevoPrecio * producto.getCantidad();
            producto.setTotal(nuevoTotal);
            productTable.refresh(); // Actualizar la tabla
            updateTotal();
        });

        onSocio();
    }

    private void calcularCambios(ActionEvent actionEvent) {
        try {
            double total = Double.parseDouble(this.totalLabel.getText().replace("€", "").trim());
            double enEfectivo = Double.parseDouble(this.tf_EnEfectivo.getText());
            double cambios = enEfectivo - total;
            this.l_cambios.setText(String.format("€ %.2f", cambios));
        } catch (NumberFormatException e) {
            // Salir primero de la pantalla completa
            Stage stage = (Stage) this.pagar_btn.getScene().getWindow();
            stage.setFullScreen(false);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al calcular cambios");
            alert.setContentText("Por favor, ingrese una cantidad válida");
            alert.showAndWait();
            stage.setFullScreen(true);
        }
    }

    @FXML
    public void onAgregar(ActionEvent event) {
        try {
            System.out.println("Agregando producto");
            String nombre = this.TF_Nombre.getText();
            String codigo = this.TF_Cod_Barra.getText();
            double precio = Double.parseDouble(this.TF_Precio.getText());
            int cantidad = Integer.parseInt(this.TF_Cantidad.getText());
            double total = precio * cantidad;

            // Verificar si ya existe un producto con el mismo código de barra en la tabla
            Ventas productoExistente = buscarProductoEnTabla(codigo);
            if (productoExistente != null) {
                // Si existe, aumentar la cantidad y actualizar el total
                int nuevaCantidad = productoExistente.getCantidad() + cantidad;
                productoExistente.setCantidad(nuevaCantidad);
                double nuevoTotal = productoExistente.getPrecio() * nuevaCantidad;
                productoExistente.setTotal(nuevoTotal);
                productTable.refresh(); // Actualizar la tabla
            } else {
                // Si no existe, crear un nuevo producto y agregarlo a la tabla
                Ventas producto = new Ventas(nombre, codigo, precio, cantidad, total);
                productos.add(producto);
            }

            this.TF_Nombre.clear();
            this.TF_Cod_Barra.clear();
            this.TF_Precio.clear();
            this.TF_Cantidad.clear();
            updateTotal();
            updateCantidad();
            System.out.println("Producto agregado");
        } catch (NumberFormatException e) {
            Stage stage = (Stage) this.pagar_btn.getScene().getWindow();
            stage.setFullScreen(false);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al agregar producto");
            alert.setContentText("Por favor, ingrese los datos correctamente");
            alert.showAndWait();
            stage.setFullScreen(true);
        }
    }
    public void onBorrar(ActionEvent event) {
        Ventas producto = productTable.getSelectionModel().getSelectedItem();
        productos.remove(producto);
        updateTotal();
        updateCantidad();
    }
//TODO
public void onPagar(ActionEvent event) {
    System.out.println("Pagando");

    // Obtener el ID del empleado (puedes obtenerlo de alguna variable o sesión)
    int idEmpleado = SessionManager.getInstance().getIDEmpleadoLogueado();
    String idEmpleadoString = String.valueOf(idEmpleado);

    // Obtener el ID del cliente si el checkbox está seleccionado
    String idCliente = "";
    if (chBox_socio.isSelected()) {
        idCliente = TF_ID_Cliente.getText();
    }else {
        idCliente = "2"; /* ID de cliente por defecto si no es socio (puedes cambiarlo según tus necesidades)
        Lo ideal sería que el cliente se registre en la base de datos o hacer uno al principio que sea cliente por defecto*/
    }

    // Crear una lista para almacenar los códigos de barras de los productos
    List<String> codigosBarras = new ArrayList<>();

    // Obtener los códigos de barras y cantidades de los productos en la tabla
    for (Ventas producto : productos) {
        String codigoBarra = producto.getCodigo();
        int cantidad = producto.getCantidad();

        // Agregar el código de barras a la lista tantas veces como la cantidad
        for (int i = 0; i < cantidad; i++) {
            codigosBarras.add(codigoBarra);
        }
    }

    // Llamar al método registrarVenta del servicio VentasService con la lista de códigos de barras
    VentasService.registrarVenta(idEmpleadoString, idCliente, codigosBarras);

    // Limpiar la tabla y los campos después de registrar la venta
    productos.clear();
    updateTotal();
    updateCantidad();
    TF_ID_Cliente.clear();
    chBox_socio.setSelected(false);
    onSocio();
    tf_EnEfectivo.clear();
    l_cambios.setText("€ 0.00");
    System.out.println("Venta registrada");

    }

    public void updateTotal() {
        double total = 0;
        for (Ventas producto : productos) {
            total += producto.getTotal();
        }
        this.totalLabel.setText(String.format("€ %.2f",total));
    }

    public  void updateCantidad(){
        int cantidad = 0;
        for (Ventas producto : productos) {
            cantidad += producto.getCantidad();
        }
        this.totalQuantityLabel.setText(String.valueOf(cantidad));
    }

    @FXML
    private void metodoPagoVisa(ActionEvent event) {
        if (this.rbt_visa.isSelected()) {
            tf_EnEfectivo.setDisable(true);
        }
    }
    @FXML
    private void metodoPagoEfectivo(ActionEvent event) {
        if (this.rbt_efectivo.isSelected()) {
            tf_EnEfectivo.setDisable(false);
        }
    }

    private void comprobarProducto(String codigo) {
        // Crear una tarea asíncrona para llamar al servidor
        CompletableFuture.runAsync(() -> {
            try {
                Ventas producto = ProductoService.verificarProducto(codigo);
                // Actualizar UI en el hilo de JavaFX
                javafx.application.Platform.runLater(() -> {
                    TF_Nombre.setText(producto.getNombre());
                    TF_Precio.setText(String.valueOf(producto.getPrecio()));
                    TF_Cantidad.setText(String.valueOf(producto.getCantidad()));
                    TF_Cantidad.setText("1");

                    // Agregar el producto a la lista de productos simulando un click en el botón de agregar
                    agregar_btn.fire();
                });
            } catch (Exception e) {
                // Manejar cualquier excepción
                javafx.application.Platform.runLater(() -> {
                    // Salir primero de la pantalla completa
                    Stage stage = (Stage) this.TF_Cod_Barra.getScene().getWindow();
                    stage.setFullScreen(false);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Producto no encontrado");
                    alert.setContentText("El producto con el código de barra " + codigo + " no se encontró en la base de datos.");
                    alert.showAndWait();
                    stage.setFullScreen(true);
                });
                e.printStackTrace();
            }
        });

    }
    private Ventas buscarProductoEnTabla(String codigo) {
        for (Ventas producto : productTable.getItems()) {
            if (producto.getCodigo().equals(codigo)) {
                return producto;
            }
        }
        return null;
    }

    // Si el checkbox está seleccionado, se habilita el campo de texto para el número de socio en caso contrario no
    public void onSocio() {
        if (chBox_socio.isSelected()) {
            TF_ID_Cliente.setDisable(false);
        } else {
            TF_ID_Cliente.setDisable(true);
        }
    }

}
