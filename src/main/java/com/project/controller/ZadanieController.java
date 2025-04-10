package com.project.controller;

import com.project.dao.ZadanieDAO;
import com.project.model.Projekt;
import com.project.model.Zadanie;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZadanieController {
	private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter dateTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	private String search4;
	private Integer pageNo;
	private Integer pageSize;
	
    @FXML
    private Button btnPierwsza, btnWstecz, btnDalej, btnOstatnia, btnPowrot, btnSzukaj, btnDodaj;
    @FXML
    private TextField txtSzukaj;
    @FXML
    private TableView<Zadanie> tblZadanie;
    @FXML
    private TableColumn<Zadanie, Integer> colId, colKolejnosc;
    @FXML
    private TableColumn<Zadanie, String> colNazwa, colOpis;
    @FXML
    private TableColumn<Zadanie, LocalDateTime> colDataCzasUtworzenia;

    private final ExecutorService wykonawca;
    private final ZadanieDAO zadanieDAO;
    private final Projekt projekt;
    
    

    public ZadanieController(Projekt projekt, ZadanieDAO zadanieDAO, ExecutorService wykonawca) {
        this.projekt = projekt;
        this.zadanieDAO = zadanieDAO;
        this.wykonawca = wykonawca;
    }

    @FXML
    private void initialize() {
        // Inicjalizacja kolumn
        colId.setCellValueFactory(new PropertyValueFactory<>("zadanieID"));
        colNazwa.setCellValueFactory(new PropertyValueFactory<>("nazwa"));
        colOpis.setCellValueFactory(new PropertyValueFactory<>("opis"));
        colKolejnosc.setCellValueFactory(new PropertyValueFactory<>("kolejnosc"));
        colDataCzasUtworzenia.setCellValueFactory(new PropertyValueFactory<>("dataCzasUtworzenia"));

        search4 = "";
        pageNo = 0;
        pageSize = 10;

        // inicjalizacja danych
        wykonawca.execute(() -> loadZadania(search4, pageNo, pageSize));

        // formatowanie daty
        colDataCzasUtworzenia.setCellFactory(column -> new TableCell<Zadanie, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(dateTimeFormater.format(item));
                }
            }
        });

        // dodanie kolumny edycji
        TableColumn<Zadanie, Void> colEdit = new TableColumn<>("Edycja");
        colEdit.setCellFactory(column -> new TableCell<Zadanie, Void>() {
            private final GridPane pane;
            {
                Button btnEdit = new Button("Edytuj");
                Button btnRemove = new Button("Usuń");

                btnEdit.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                btnRemove.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                btnEdit.setOnAction(event -> edytujZadanie(getCurrentZadanie()));
                btnRemove.setOnAction(event -> usunZadanie(getCurrentZadanie()));

                pane = new GridPane();
                pane.setAlignment(Pos.CENTER);
                pane.setHgap(10);
                pane.setVgap(10);
                pane.setPadding(new Insets(5, 5, 5, 5));
                pane.add(btnEdit, 0, 0);
                pane.add(btnRemove, 1, 0);
            }

            private Zadanie getCurrentZadanie() {
                int index = this.getTableRow().getIndex();
                return this.getTableView().getItems().get(index);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        tblZadanie.getColumns().add(colEdit);
        colEdit.setMaxWidth(7000);
    }

    public void openZadanieFrame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ZadanieFrame.fxml"));
            loader.setControllerFactory(controllerClass -> new ZadanieController(projekt, zadanieDAO, wykonawca));

            Stage stage = new Stage(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Zadania");
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            stage.setScene(scene);

            ExecutorService lokalnyWykonawca = Executors.newSingleThreadExecutor();
            loader.setControllerFactory(controllerClass -> new ZadanieController(projekt, zadanieDAO, lokalnyWykonawca));


            stage.show();
        } catch (IOException e) {
            showError("Błąd", "Nie udało się otworzyć okna zadań: " + e.getMessage());
        }
    }

    /**
     * Ładuje listę zadań z opcjonalnym filtrem i paginacją.
     * @param search4 Fraza wyszukiwania (ID, nazwa lub data)
     * @param pageNo Numer strony (0-indeksowane)
     * @param pageSize Liczba elementów na stronę
     */
    private void loadZadania(String search4, Integer pageNo, Integer pageSize) {
        try {
            final List<Zadanie> zadanieList = new ArrayList<>();

            if (search4 != null && !search4.isEmpty()) {
                if (search4.matches("\\d+")) { // wyszukiwanie po ID
                    Zadanie zadanie = zadanieDAO.getZadanie(Integer.valueOf(search4));
                    if (zadanie != null) zadanieList.add(zadanie);
                } else if (search4.matches("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) { // data
                    LocalDate parsedDate = LocalDate.parse(search4, dateFormatter);
                    LocalDateTime dateTimeStart = parsedDate.atStartOfDay();
                    zadanieList.addAll(zadanieDAO.getZadaniaWhereDataUtworzeniaIs(
                            dateTimeStart,
                            projekt.getProjektId(),
                            pageNo != null ? pageNo * pageSize : 0,
                            pageSize));
                } else { // wyszukiwanie po nazwie
                    zadanieList.addAll(zadanieDAO.getZadaniaWhereNazwaLike(search4, projekt.getProjektId(), pageNo * pageSize, pageSize));
                }
            } else { // brak filtra
                zadanieList.addAll(zadanieDAO.getZadaniaByProjekt(projekt.getProjektId(), pageNo * pageSize, pageSize));
            }

            Platform.runLater(() -> {
                tblZadanie.getItems().clear();
                tblZadanie.getItems().addAll(zadanieList);
            });

        } catch (RuntimeException e) {
            String errMsg = "Błąd podczas pobierania listy zadań.";
            logger.error(errMsg, e);
            String errDetails = e.getCause() != null ? e.getMessage() + "\n" + e.getCause().getMessage() : e.getMessage();
            Platform.runLater(() -> showError(errMsg, errDetails));
        }
    }


    /** Metoda pomocnicza do prezentowania użytkownikowi informacji o błędach */
    private void showError(String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Błąd");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void onActionBtnPierwsza() {
        tblZadanie.getSelectionModel().selectFirst();
    }

    @FXML
    private void onActionBtnWstecz() {
        int index = tblZadanie.getSelectionModel().getSelectedIndex();
        if (index > 0) {
            tblZadanie.getSelectionModel().select(index - 1);
        }
    }

    @FXML
    private void onActionBtnDalej() {
        int index = tblZadanie.getSelectionModel().getSelectedIndex();
        if (index < tblZadanie.getItems().size() - 1) {
            tblZadanie.getSelectionModel().select(index + 1);
        }
    }

    @FXML
    private void onActionBtnOstatnia() {
        tblZadanie.getSelectionModel().selectLast();
    }

    @FXML
    private void onActionBtnPowrot(ActionEvent event) {
        Stage stage = (Stage) btnPowrot.getScene().getWindow();
        wykonawca.shutdown(); // Zamknięcie ExecutorService przed zamknięciem okna
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

	@FXML
	private void onActionBtnSzukaj(ActionEvent event) {
	    String searchTerm = txtSzukaj.getText().trim();
	    if (!searchTerm.isEmpty()) {
	        search4 = searchTerm;
	    } else {
	        search4 = ""; // Resetowanie wyszukiwania, jeśli pole jest puste
	    }
	    pageNo = 0; // Resetowanie na pierwszą stronę wyników
	    wykonawca.execute(() -> loadZadania(search4, pageNo, pageSize));
	}

	@FXML
	private void onActionBtnDodaj(ActionEvent event) {
	    Zadanie noweZadanie = new Zadanie();
	    noweZadanie.setProjekt(projekt); // <-- to zapobiega NullPointerException
	    edytujZadanie(noweZadanie);
	}

    private void edytujZadanie(Zadanie zadanie) {
        Dialog<Zadanie> dialog = new Dialog<>();
        dialog.setTitle("Edycja zadania");

        if (zadanie.getZadanieID() != null) {
            dialog.setHeaderText("Edycja danych zadania");
        } else {
            dialog.setHeaderText("Dodawanie zadania");
        }

        dialog.setResizable(true);

        Label lblId = getRightLabel("Id: ");
        Label lblNazwa = getRightLabel("Nazwa: ");
        Label lblOpis = getRightLabel("Opis: ");
        Label lblKolejnosc = getRightLabel("Kolejność: ");
        Label lblDataCzasUtworzenia = getRightLabel("Data utworzenia: ");
        Label lblProjekt = getRightLabel("Projekt: ");

        Label txtId = new Label(zadanie.getZadanieID() != null ? zadanie.getZadanieID().toString() : "");

        TextField txtNazwa = new TextField(zadanie.getNazwa() != null ? zadanie.getNazwa() : "");
        TextArea txtOpis = new TextArea(zadanie.getOpis() != null ? zadanie.getOpis() : "");
        txtOpis.setPrefRowCount(6);
        txtOpis.setPrefColumnCount(40);
        txtOpis.setWrapText(true);

        TextField txtKolejnosc = new TextField(zadanie.getKolejnosc() != null ? zadanie.getKolejnosc().toString() : "");

        Label txtDataUtworzenia = new Label(zadanie.getDataCzasUtworzenia() != null
                ? dateTimeFormater.format(zadanie.getDataCzasUtworzenia()) : "");

        Label txtProjekt = new Label(zadanie.getProjekt() != null ? zadanie.getProjekt().getNazwa() : "Brak");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.add(lblId, 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(lblDataCzasUtworzenia, 0, 1);
        grid.add(txtDataUtworzenia, 1, 1);
        grid.add(lblNazwa, 0, 2);
        grid.add(txtNazwa, 1, 2);
        grid.add(lblOpis, 0, 3);
        grid.add(txtOpis, 1, 3);
        grid.add(lblKolejnosc, 0, 4);
        grid.add(txtKolejnosc, 1, 4);
        grid.add(lblProjekt, 0, 5);
        grid.add(txtProjekt, 1, 5);

        dialog.getDialogPane().setContent(grid);

        ButtonType buttonTypeOk = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk, buttonTypeCancel);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == buttonTypeOk) {
                zadanie.setNazwa(txtNazwa.getText().trim());
                zadanie.setOpis(txtOpis.getText().trim());
                try {
                    zadanie.setKolejnosc(Integer.parseInt(txtKolejnosc.getText().trim()));
                } catch (NumberFormatException e) {
                    showError("Błąd", "Kolejność musi być liczbą.");
                    return null;
                }
                return zadanie;
            }
            return null;
        });

        Optional<Zadanie> result = dialog.showAndWait();
        result.ifPresent(z -> wykonawca.execute(() -> {
            try {
                zadanieDAO.setZadanie(z);
                Platform.runLater(() -> {
                    if (tblZadanie.getItems().contains(z)) {
                        tblZadanie.refresh();
                    } else {
                        tblZadanie.getItems().add(0, z);
                    }
                });
            } catch (RuntimeException e) {
                String errMsg = "Błąd podczas zapisywania zadania!";
                logger.error(errMsg, e);
                String errDetails = e.getCause() != null ? e.getMessage() + "\n" + e.getCause().getMessage() : e.getMessage();
                Platform.runLater(() -> showError(errMsg, errDetails));
            }
        }));
    }

    private Label getRightLabel(String text) {
        Label lbl = new Label(text);
        lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER_RIGHT);
        return lbl;
    }
    
    private void usunZadanie(Zadanie zadanie) {
        if (zadanie == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Potwierdzenie usunięcia");
        alert.setHeaderText("Czy na pewno chcesz usunąć zadanie?");
        alert.setContentText("Zadanie: " + zadanie.getNazwa());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                zadanieDAO.deleteZadanie(zadanie.getZadanieID());
                loadZadania(search4, pageNo, pageSize); // odświeżenie danych po usunięciu
            } catch (Exception e) {
                showError("Błąd podczas usuwania zadania.", e.getMessage());
            }
        }
    }
}
