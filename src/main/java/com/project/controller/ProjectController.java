package com.project.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.project.dao.ZadanieDAO;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.project.model.Projekt;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import com.project.dao.ProjektDAO;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProjectController {
	private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
	//Zmienne do obsługi stronicowania i wyszukiwania
	private String search4;
	private Integer pageNo;
	private Integer pageSize;
	//Automatycznie wstrzykiwane komponenty GUI
	@FXML
	private ChoiceBox<Integer> cbPageSizes;
	@FXML
	private TableView<Projekt> tblProjekt;
	@FXML
	private TableColumn<Projekt, Integer> colId;
	@FXML
	private TableColumn<Projekt, String> colNazwa;
	@FXML
	private TableColumn<Projekt, String> colOpis;
	@FXML
	private TableColumn<Projekt, LocalDateTime> colDataCzasUtworzenia;
	@FXML
	private TableColumn<Projekt, LocalDate> colDataOddania;
	@FXML
	private TextField txtSzukaj;
	@FXML
	private Button btnDalej;
	@FXML
	private Button btnWstecz;
	@FXML
	private Button btnPierwsza;
	@FXML
	private Button btnOstatnia;
    @FXML
    private Button btnOtworzZadania;
    @FXML
    private Label labelPageNo;

	private ExecutorService wykonawca;
    private final ZadanieDAO zadanieDAO;
	private ProjektDAO projektDAO;
	

	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter dateTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private ObservableList<Projekt> projekty;

	public ProjectController(ProjektDAO projektDAO, ZadanieDAO zadanieDAO) {
		this.zadanieDAO = zadanieDAO;
		this.projektDAO = projektDAO;
		wykonawca = Executors.newFixedThreadPool(1);
		// W naszej aplikacji wystarczy jeden wątek do pobierania danych.
		// Przekazanie większej ilości takich zadań do puli jednowątkowej
		// powoduje ich kolejkowanie i sukcesywne wykonywanie.
	}

	//Metoda automatycznie wywoływana przez JavaFX zaraz po wstrzyknięciu wszystkich komponentów.
	// Uwaga! Wszelkie modyfikacje komponentów (np. cbPageSizes) trzeba realizować wewnątrz tej metody.
	// Nigdy nie używaj do tego celu konstruktora.
	@FXML
	public void initialize() {
		search4 = "";
		pageNo = 0;
		pageSize = 10;

		cbPageSizes.getItems().addAll(5, 10, 20, 50, 100);
		cbPageSizes.setValue(pageSize);

		colId.setCellValueFactory(new PropertyValueFactory<Projekt, Integer>("projektId"));
		colNazwa.setCellValueFactory(new PropertyValueFactory<Projekt, String>("nazwa"));
		colOpis.setCellValueFactory(new PropertyValueFactory<Projekt, String>("opis"));
		colDataCzasUtworzenia.setCellValueFactory(new PropertyValueFactory<Projekt, LocalDateTime>
				("dataCzasUtworzenia"));
		colDataOddania.setCellValueFactory(new PropertyValueFactory<Projekt, LocalDate>("dataOddania"));
		projekty = FXCollections.observableArrayList();

		//Powiązanie tabeli z listą typu ObservableList przechowującą projekty
		tblProjekt.setItems(projekty);

		wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));

		colDataCzasUtworzenia.setCellFactory(column -> new TableCell<Projekt, LocalDateTime>() {
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

		//Utworzenie nowej kolumny
		TableColumn<Projekt, Void> colEdit = new TableColumn<>("Edycja");
		colEdit.setCellFactory(column -> new TableCell<Projekt, Void>() {
			private final GridPane pane;
			{
				// Blok inicjalizujący w anonimowej klasie wewnętrznej
				Button btnEdit = new Button("Edycja");
				Button btnRemove = new Button("Usuń");
				Button btnTask = new Button("Zadania");
				btnEdit.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
				btnRemove.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
				btnTask.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
				btnEdit.setOnAction(event -> {
					edytujProjekt(getCurrentProjekt());
				});
				btnRemove.setOnAction(event -> {
					usunProjekt(getCurrentProjekt());
				});
				btnTask.setOnAction(event -> {
					openZadanieFrame(getCurrentProjekt());
				});
				pane = new GridPane();
				pane.setAlignment(Pos.CENTER);
				pane.setHgap(10);
				pane.setVgap(10);
				pane.setPadding(new Insets(5, 5, 5, 5));
				pane.add(btnTask, 0, 0);
				pane.add(btnEdit, 0, 1);
				pane.add(btnRemove, 0, 2);
			}
			private Projekt getCurrentProjekt() {
				int index = this.getTableRow().getIndex();
				return this.getTableView().getItems().get(index);
			}
			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(empty ? null : pane);
			}
		});

		//Dodanie kolumny do tabeli
		tblProjekt.getColumns().add(colEdit);
		//Ustawienie względnej szerokości poszczególnych kolumn (liczą się proporcje)
		colId.setMaxWidth(5000);
		colNazwa.setMaxWidth(10000);
		colOpis.setMaxWidth(10000);
		colDataCzasUtworzenia.setMaxWidth(9000);
		colDataOddania.setMaxWidth(7000);
		colEdit.setMaxWidth(7000);

		cbPageSizes.setOnAction(event -> onPageSizeChange());
	}
	
	private Projekt getCurrentProjekt() {
	    return tblProjekt.getSelectionModel().getSelectedItem();
	}

	/**
	 * Odświeża widok projektów, stosując paginację oraz opcjonalnie stosuje filtr
	 * wyszukiwania na podstawie frazy wyszukiwania.
	 * @param search4 Fraza, względem której wyszukiwane są projekty
	 * @param pageNo Numer obecnie wyświetlanej strony
	 * @param pageSize Liczba projektów wyświetlanych na jednej stronie
	 */
	private void loadPage(String search4, Integer pageNo, Integer pageSize) {
	    try {
	        final List<Projekt> projektList = new ArrayList<>();
	        
	        if (search4 != null && !search4.isEmpty()) {
	            if (search4.matches("[0-9]+")) { // wyszukiwanie po ID
	                Projekt projekt = projektDAO.getProjekt(Integer.valueOf(search4));
	                if (projekt != null) projektList.add(projekt);
	            } else if (search4.matches(
	            		// wyszukiwanie po dacie
	            		"^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) { 
	                projektList.addAll(projektDAO.getProjektyWhereDataOddaniaIs(
	                		LocalDate.parse(search4), pageNo, pageSize));
	            } else { // wyszukiwanie po nazwie
	                projektList.addAll(projektDAO.getProjektyWhereNazwaLike(
	                		search4, pageNo * pageSize, pageSize));
	            }
	        } else { // brak filtra - pobieranie wszystkich projektów
	            projektList.addAll(projektDAO.getProjekty(pageNo * pageSize, pageSize));
	        }
	        updateLabelPageNo();
	        Platform.runLater(() -> {
	            projekty.clear();
	            projekty.addAll(projektList);
	        });
	        
	    } catch (RuntimeException e) {
	        String errMsg = "Błąd podczas pobierania listy projektów.";
	        logger.error(errMsg, e);
	        String errDetails = e.getCause() != null ? e.getMessage() + 
	        		"\n" + e.getCause().getMessage() : e.getMessage();
	        Platform.runLater(() -> showError(errMsg, errDetails));
	    }
	}
	private void updateLabelPageNo() {
		labelPageNo.setText("Strona " + (pageNo + 1));
	}

	/**
	 * Zamyka pulę wątków w sposób kontrolowany.
	 * Jeśli `wykonawca` (executor) nie jest `null`, w pierwszej kolejności wywoływane jest `shutdown()`,
	 * które oznacza, że nowe zadania nie będą przyjmowane, ale bieżące będą kontynuowane.
	 * Następnie metoda czeka do 5 sekund na zakończenie wszystkich zadań.
	 * Jeśli w tym czasie zadania nie zakończą się, wymuszone zostaje ich natychmiastowe przerwanie
	 * za pomocą `shutdownNow()`.
	 * W przypadku przerwania oczekiwania na zakończenie (`InterruptedException`), również wywoływane jest `shutdownNow()`.
	 * Dzięki takiemu podejściu zapewniona jest poprawna deaktywacja puli wątków, bez pozostawiania
	 * wiszących zadań w tle.
	 */
	public void shutdown() {
		// Wystarczyłoby tylko samo wywołanie metody wykonawca.shutdownNow(), ale można również, tak jak poniżej,
		// zaimplementować wersję z oczekiwaniem na zakończenie wszystkich zadań wykonywanych w puli wątków.
		if(wykonawca != null) {
			wykonawca.shutdown();
			try {
				if(!wykonawca.awaitTermination(5, TimeUnit.SECONDS))
					wykonawca.shutdownNow();
			} catch (InterruptedException e) {
				wykonawca.shutdownNow();
			}
		}
	}

	//Grupa metod do obsługi przycisków
	@FXML
	private void onActionBtnSzukaj(ActionEvent event) {
	    String searchTerm = txtSzukaj.getText().trim();
	    if (!searchTerm.isEmpty()) {
	        search4 = searchTerm;
	    } else {
	        search4 = ""; // resetowanie wyszukiwania, jeśli pole jest puste
	    }
	    pageNo = 0; // resetowanie na pierwszą stronę wyników
	    wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
	}

	@FXML
	private void onActionBtnDalej(ActionEvent event) {
		int lastPage = getLastPage();
		if (pageNo < lastPage) {
			pageNo ++;
			loadPage(null, pageNo, pageSize);
		}
	}
	@FXML
	private void onActionBtnWstecz(ActionEvent event) {
		if (pageNo > 0) {
			pageNo --;
			loadPage(null, pageNo, pageSize);
		}
	}
	@FXML
	private void onActionBtnPierwsza() {
		if (pageNo > 0) {
			pageNo = 0;
			loadPage(null, 0, pageSize);
		}
	}

	@FXML
	private void onActionBtnOstatnia() {
		pageNo = getLastPage();
		loadPage(null, pageNo, pageSize);
	}

	@FXML
	public void onActionBtnDodaj(ActionEvent event) {
		edytujProjekt(new Projekt());
	}

	/**
	 * Wyświetla nowe okno, w którym można dodać nowy projekt lub edytować istniejący projekt.
	 * @param projekt Edytowany projekt, eśli wybrano jakiś lub null, co oznacza stworzenie nowego projektu
	 */
	private void edytujProjekt(Projekt projekt) {
		Dialog<Projekt> dialog = new Dialog<>();
		dialog.setTitle("Edycja");

		if (projekt.getProjektId() != null) {
			dialog.setHeaderText("Edycja danych projektu");
		} else {
			dialog.setHeaderText("Dodawanie projektu");
		}

		dialog.setResizable(true);

		Label lblId = getRightLabel("Id: ");
		Label lblNazwa = getRightLabel("Nazwa: ");
		Label lblOpis = getRightLabel("Opis: ");
		Label lblDataCzasUtworzenia = getRightLabel("Data utworzenia: ");
		Label lblDataOddania = getRightLabel("Data oddania: ");

		Label txtId = new Label();
		if (projekt.getProjektId() != null) {
			txtId.setText(projekt.getProjektId().toString());
		}

		TextField txtNazwa = new TextField();
		if (projekt.getNazwa() != null) {
			txtNazwa.setText(projekt.getNazwa());
		}

		TextArea txtOpis = new TextArea();
		txtOpis.setPrefRowCount(6);
		txtOpis.setPrefColumnCount(40);
		txtOpis.setWrapText(true);
		if (projekt.getOpis() != null) {
			txtOpis.setText(projekt.getOpis());
		}

		Label txtDataUtworzenia = new Label();
		if (projekt.getDataCzasUtworzenia() != null) {
			txtDataUtworzenia.setText(dateTimeFormater.format(projekt.getDataCzasUtworzenia()));
		} 

		DatePicker dtDataOddania = new DatePicker();
		dtDataOddania.setPromptText("RRRR-MM-DD");
		dtDataOddania.setConverter(new StringConverter<LocalDate>() {
			@Override
			public String toString(LocalDate date) {
				return date != null ? dateFormatter.format(date) : null;
			}

			@Override
			public LocalDate fromString(String text) {
				return text == null || text.trim().isEmpty() ? null : LocalDate.parse(text, dateFormatter);
			}
		});

		dtDataOddania.getEditor().focusedProperty().addListener((obsValue, oldFocus, newFocus) -> {
			if (!newFocus) {
				try {
					dtDataOddania.setValue(dtDataOddania.getConverter().fromString(
							dtDataOddania.getEditor().getText()));
				} catch (DateTimeParseException e) {
					dtDataOddania.getEditor().setText(dtDataOddania.getConverter()
							.toString(dtDataOddania.getValue()));
				}
			}
		});

		if (projekt.getDataOddania() != null) {
			dtDataOddania.setValue(projekt.getDataOddania());
		}

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
		grid.add(lblDataOddania, 0, 4);
		grid.add(dtDataOddania, 1, 4);

		dialog.getDialogPane().setContent(grid);

		ButtonType buttonTypeOk = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
		ButtonType buttonTypeCancel = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
		dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
		dialog.getDialogPane().getButtonTypes().add(buttonTypeCancel);

		dialog.setResultConverter(new Callback<ButtonType, Projekt>() {
			@Override
			public Projekt call(ButtonType buttonType) {
				if (buttonType == buttonTypeOk) {
					projekt.setNazwa(txtNazwa.getText().trim());
					projekt.setOpis(txtOpis.getText().trim());
					projekt.setDataOddania(dtDataOddania.getValue());
					return projekt;
				}
				return null;
			}
		});

		Optional<Projekt> result = dialog.showAndWait();
		if (result.isPresent()) {
			wykonawca.execute(() -> {
				try {
					projektDAO.setProjekt(projekt);
					Platform.runLater(() -> {
						if (tblProjekt.getItems().contains(projekt)) {
							tblProjekt.refresh();
						} else {
							tblProjekt.getItems().add(0, projekt);
						}
					});
				} catch (RuntimeException e) {
					String errMsg = "Błąd podczas zapisywania danych projektu!";
					logger.error(errMsg, e);
					String errDetails = e.getCause() != null ?
							e.getMessage() + "\n" + e.getCause().getMessage()
							: e.getMessage();
					Platform.runLater(() -> showError(errMsg, errDetails));
				}
			});
		}
	}

	private Label getRightLabel(String text) {
		Label lbl = new Label(text);
		lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		lbl.setAlignment(Pos.CENTER_RIGHT);
		return lbl;
	}

	private void usunProjekt(Projekt projekt) {
	if (projekt == null) {
		return;
	}

	Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
	alert.setTitle("Potwierdzenie usunięcia");
	alert.setHeaderText("Czy na pewno chcesz usunąć projekt?");
	alert.setContentText("Projekt: " + projekt.getNazwa());

	Optional<ButtonType> result = alert.showAndWait();
	if (result.isPresent() && result.get() == ButtonType.OK) {
		try {
			projektDAO.deleteProjekt(projekt.getProjektId());
			loadPage(null, 0, pageSize);
		} catch (Exception e) {
			showError("Błąd podczas usuwania projektu.", e.getMessage());
		}
	}
}

	/**
	 * Metoda wywoływana przy wyborze wartości innej niż dotychczasowej z listy
	 * rozwijanej z możliwymi ilościami projektów wyświetlanych na stronę.
	 */
	@FXML
	private void onPageSizeChange() {
		Integer newSize = cbPageSizes.getValue();
		if (newSize != null && !newSize.equals(pageSize)) {
			pageSize = newSize;
			pageNo = 0;
			loadPage(null, pageNo, pageSize);
		}
	}

	/**
	 * Oblicza ilość stron przy obecnie przechowywanej ilości projektów w bazie danych.
	 * @return Numer ostatniej strony z projektami 
	 * (jednocześnie to jest ilość wszystkich stron z projektami przy obecnej paginacji)
	 */
	private int getLastPage() {
		int totalProjects = projektDAO.getRowsNumber();
		if (totalProjects == 0) return 0;
        return (totalProjects - 1) / pageSize;
	}
	
	/**
	 * Otwiera widok zadań przypisanych do danego projektu.
	 * @param projekt Projekt, przy którym wciśnięto przycisk "Zadania"
	 */
	public void openZadanieFrame(Projekt projekt) {
	    try {
	        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ZadanieFrame.fxml"));
	        loader.setControllerFactory(controllerClass -> new ZadanieController(projekt, zadanieDAO, wykonawca));

	        Stage stage = new Stage(StageStyle.DECORATED);
	        stage.initModality(Modality.APPLICATION_MODAL);
	        stage.setTitle("Zadania");
	        Scene scene = new Scene(loader.load());
	        scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
	        stage.setScene(scene);
	        stage.show();
	    } catch (IOException e) {
	        showError("Błąd", "Nie udało się otworzyć okna zadań: " + e.getMessage());
	    }
	}

	/** Metoda pomocnicza do prezentowania błędów */
	private void showError(String header, String content) {
	    Alert alert = new Alert(Alert.AlertType.ERROR);
	    alert.setTitle("Błąd");
	    alert.setHeaderText(header);
	    alert.setContentText(content);
	    alert.showAndWait();
	}

	/**
	 * Metoda otwierająca nowe okienko z zadaniami przypisanymi do danego projektu. 
	 * Metoda uruchamiana po wciśnięciu przycisku "Zadania" w kolumnie "Edycja" przy danym projekcie.
	 */
	@FXML
	private void onActionBtnOtworzZadania() {
	    Projekt aktualnyProjekt = getCurrentProjekt();
	    if (aktualnyProjekt != null) {
	        ZadanieController zadanieController = new ZadanieController(aktualnyProjekt, zadanieDAO, wykonawca);
	        zadanieController.openZadanieFrame();
	    }
	}
}