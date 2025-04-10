package com.project.app;

import com.project.controller.ProjectController;
import com.project.dao.ProjektDAO;
import com.project.dao.ProjektDAOImpl;
import com.project.dao.ZadanieDAO;
import com.project.dao.ZadanieDAOImpl;
import com.project.datasource.DbInitializer;
import javafx.application.Platform;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ProjectClientApplication extends Application {
	private Parent root;
	private FXMLLoader loader;
	public static void main(String[] args) {
		DbInitializer.init();
		launch(ProjectClientApplication.class, args);
	}
	@Override
	public void start(Stage primaryStage) throws Exception {
		loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/fxml/ProjectFrame.fxml"));
		
		ProjektDAO projektDAO = new ProjektDAOImpl();
		ZadanieDAO zadanieDAO = new ZadanieDAOImpl();
		
		loader.setControllerFactory(controllerClass ->
				new ProjectController(projektDAO, zadanieDAO));
		root = loader.load();
		primaryStage.setTitle("Projekty");
		Scene scene = new Scene(root);
		scene.getStylesheets()
				.add(getClass().getResource("/css/application.css")
						.toExternalForm());

		ProjectController controller = loader.getController();
		primaryStage.setOnCloseRequest(event -> {
			controller.shutdown();
			Platform.exit();
		});
		primaryStage.setScene(scene);
		primaryStage.sizeToScene();
		primaryStage.show();
	}
}

/*
// test zaimplementowanych metod z ProjektDAOImpl
public class ProjectClientApplication {
	private static final Logger logger = LoggerFactory.
			getLogger(ProjectClientApplication.class);
	public static void main(String[] args) {
		DbInitializer.init();
		ProjektDAO projektDAO = new ProjektDAOImpl();
		Projekt projekt = new Projekt("Projekt testowy", "Opis testowy",
				LocalDate.of(2020, 6, 22).atStartOfDay(),
				LocalDate.of(2020, 8, 30));
		try {
			projektDAO.setProjekt(projekt);
			logger.info("Id utworzonego projektu: {}",
					projekt.getProjektId());

			Integer projektId = projekt.getProjektId();
			Projekt projekt2 = projektDAO.getProjekt(projektId);
			logger.info("Pobrany projekt - Id: {}, nazwa: {}, opis: {}",
					projekt2.getProjektId(), projekt2.getNazwa(),
					projekt2.getOpis());

			Projekt projektPobrany = projektDAO.getProjekt(projektId);
			if (projektPobrany != null) {
				logger.info("getProjekt działa poprawnie: Id: {},"
						+ " nazwa: {}, opis: {}",
						projektPobrany.getProjektId(),
						projektPobrany.getNazwa(),
						projektPobrany.getOpis());
			} else {
				logger.error("Błąd: getProjekt zwrócił null"
						+ " dla projektu o ID {}", projektId);
			}

			List<Projekt> listaProjektow = projektDAO.getProjekty(0, 10);
			logger.info("getProjekty zwróciło {} projektów",
					listaProjektow.size());
			for (Projekt p : listaProjektow) {
				logger.info("Projekt - Id: {}, nazwa: {}, opis: {}",
						p.getProjektId(), p.getNazwa(), p.getOpis());
			}
			List<Projekt> projektyZNazwa = projektDAO.
					getProjektyWhereNazwaLike("test", 0, 5);
			logger.info("getProjektyWhereNazwaLike zwróciło {}" +
					" projektów pasujących do 'test'", projektyZNazwa.size());

			List<Projekt> projektyZData = projektDAO.getProjektyWhereDataOddaniaIs(
					LocalDate.of(2020, 8, 30), 0, 5);
			logger.info("getProjektyWhereDataOddaniaIs zwróciło {} " +
					"projektów na datę 2020-08-30", projektyZData.size());

			int liczbaProjektow = projektDAO.getRowsNumber();
			logger.info("getRowsNumber zwróciło: {} projektów", liczbaProjektow);

			int liczbaTestowychProjektow = projektDAO.
					getRowsNumberWhereNazwaLike("test");
			logger.info("getRowsNumberWhereNazwaLike zwróciło: {} " +
					"projektów pasujących do 'test'", liczbaTestowychProjektow);

			int liczbaProjektowZData = projektDAO.getRowsNumberWhereDataOddaniaIs(
					LocalDate.of(2020, 8, 30));
			logger.info("getRowsNumberWhereDataOddaniaIs zwróciło: {} " +
					"projektów dla daty 2020-08-30", liczbaProjektowZData);

			projektDAO.deleteProjekt(projektId);
			Projekt sprawdzenieUsuniecia = projektDAO.getProjekt(projektId);
			if (sprawdzenieUsuniecia == null) {
				logger.info("deleteProjekt działa poprawnie"
						+ " – projekt został usunięty.");
			} else {
				logger.error("Błąd: deleteProjekt nie usunął"
						+ " projektu o ID {}", projektId);
			}

		} catch (RuntimeException e) {
			logger.error("Błąd operacji bazodanowej!", e);
		}
	}
}
*/