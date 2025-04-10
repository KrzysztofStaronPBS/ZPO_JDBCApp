package com.project.dao;

import com.project.datasource.DataSource;
import com.project.model.Projekt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjektDAOImpl implements ProjektDAO {
    /**
     * Pobiera pojedynczy projekt z bazy danych na podstawie otrzymanego ID projektu.
     * @param projektId ID projektu, który mma zostać pobrany z bazy danych
     * @return Projekt posiadający wskazane ID, w przeciwnym razie null
     */
    @Override
    public Projekt getProjekt(Integer projektId) {
        String query = "SELECT * FROM projekt WHERE projekt_id = ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setInt(1, projektId);
            ResultSet rs = prepStmt.executeQuery();

            if (rs.next()) {
                return new Projekt(
                        rs.getInt("projekt_id"),
                        rs.getString("nazwa"),
                        rs.getString("opis"),
                        rs.getObject("dataczas_utworzenia", LocalDateTime.class),
                        rs.getObject("data_oddania", LocalDate.class)
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null; // zwracamy null, jeśli projekt nie istnieje
    }

    /**
     * Pozwala dodać nowy projekt lub zmodyfikować istniejący projekt.
     * @param projekt Projekt do dodania lub zmodyfikowania
     */
    @Override
    public void setProjekt(Projekt projekt){
        boolean isInsert = projekt.getProjektId() == null;
        String query = isInsert ?
                "INSERT INTO projekt(nazwa, opis, dataczas_utworzenia, data_oddania)"
                + " VALUES (?, ?, ?, ?)" : "UPDATE projekt SET nazwa = ?, opis = ?,"
                		+ " dataczas_utworzenia = ?, data_oddania = ? "
                		+ "WHERE projekt_id = ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query, isInsert ?
                     Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {

            // Wstawianie do zapytania odpowiednich wartości w miejsce znaków '?'
            // Uwaga! Indeksowanie znaków '?' zaczyna się od 1!
            prepStmt.setString(1, projekt.getNazwa());

            prepStmt.setString(2, projekt.getOpis());
            prepStmt.setObject(3, projekt.getDataCzasUtworzenia() != null ?
                    projekt.getDataCzasUtworzenia() : LocalDateTime.now());
            prepStmt.setObject(4, projekt.getDataOddania());

            if(!isInsert) prepStmt.setInt(5, projekt.getProjektId());

            // Wysyłanie zapytania i pobieranie danych
            int liczbaDodanychWierszy = prepStmt.executeUpdate();

            // Pobieranie kluczy głównych, tylko dla nowo utworzonych projektów
            if (isInsert && liczbaDodanychWierszy > 0) {
                ResultSet keys = prepStmt.getGeneratedKeys();
                if (keys.next()) {
                    projekt.setProjektId(keys.getInt(1));
                }
                keys.close();
            }
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Usuwa projekt o wskazanym ID z bazy danych.
     * @param projektId ID projektu do usunięcia
     */
    @Override
    public void deleteProjekt(Integer projektId) {
        String query = "DELETE FROM projekt WHERE projekt_id = ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setInt(1, projektId);
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * @param offset Liczba wierszy do pominięcia w wyniku (dla paginacji); może być null
	 * @param limit Maksymalna liczba wierszy do zwrócenia (dla paginacji); może być null
     * @return Lista wszystkich projektów
     */
    @Override
    public List<Projekt> getProjekty(Integer offset, Integer limit){
        List<Projekt> projekty = new ArrayList<>();

        String query = "SELECT * FROM projekt ORDER BY dataczas_utworzenia DESC"
                + (offset != null ? " OFFSET ?" : "")
                + (limit != null ? " LIMIT ?" : "");

        try (Connection connect = DataSource.getConnection();
             PreparedStatement preparedStmt = connect.prepareStatement(query)) {
                int i = 1;
                if (offset != null) {
                    preparedStmt.setInt(i, offset);
                    i += 1;
                }
                if (limit != null) {
                    preparedStmt.setInt(i, limit);
                }
                try (ResultSet rs = preparedStmt.executeQuery()) {
                    while (rs.next()) {
                        Projekt projekt = new Projekt();
                        projekt.setProjektId(rs.getInt("projekt_id"));
                        projekt.setNazwa(rs.getString("nazwa"));
                        projekt.setOpis(rs.getString("opis"));
                        projekt.setDataCzasUtworzenia(rs.getObject(
                        		"dataczas_utworzenia", LocalDateTime.class));
                        projekt.setDataOddania(rs.getObject(
                        		"data_oddania", LocalDate.class));
                        projekty.add(projekt);
                    }
                }
            }catch(SQLException e) {
                throw new RuntimeException(e);
            }
        return projekty;
    }

    /**
     * Zwraca listę projektów, których nazwa zawiera podany ciąg znaków.
     * Wyniki są posortowane malejąco według daty utworzenia.
     * @param nazwa Częściowa lub pełna nazwa projektu do wyszukania (używane z operatorem LIKE).
     * @param offset Liczba wierszy do pominięcia w wyniku (dla paginacji); może być null.
     * @param limit Maksymalna liczba wierszy do zwrócenia (dla paginacji); może być null.
     * @return Lista projektów spełniających warunki wyszukiwania.
     */
    @Override
    public List<Projekt> getProjektyWhereNazwaLike(
    		String nazwa, Integer offset, Integer limit) {
        List<Projekt> projekty = new ArrayList<>();
        String query = "SELECT * FROM projekt WHERE nazwa LIKE ?"
        		+ " ORDER BY dataczas_utworzenia DESC" +
                (offset != null ? " OFFSET ?" : "") +
                (limit != null ? " LIMIT ?" : "");

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            int i = 1;
            prepStmt.setString(i++, "%" + nazwa + "%");

            if (offset != null) prepStmt.setInt(i++, offset);
            if (limit != null) prepStmt.setInt(i, limit);

            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    projekty.add(new Projekt(
                            rs.getInt("projekt_id"),
                            rs.getString("nazwa"),
                            rs.getString("opis"),
                            rs.getObject("dataczas_utworzenia",
                            		LocalDateTime.class),
                            rs.getObject("data_oddania", LocalDate.class)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return projekty;
    }

    /**
     * Zwraca listę projektów, których data oddania jest równa podanej wartości.
     * Wyniki są posortowane malejąco według daty utworzenia.
     * @param dataOddania Data oddania projektu, którą mają spełniać wyniki.
     * @param offset Liczba wierszy do pominięcia w wyniku (dla paginacji); może być null.
     * @param limit Maksymalna liczba wierszy do zwrócenia (dla paginacji); może być null.
     * @return Lista projektów z konkretną datą oddania.
     */
    @Override
    public List<Projekt> getProjektyWhereDataOddaniaIs(
    		LocalDate dataOddania, Integer offset, Integer limit) {
        List<Projekt> projekty = new ArrayList<>();
        String query = "SELECT * FROM projekt WHERE data_oddania = ?"
        		+ " ORDER BY dataczas_utworzenia DESC" +
                (offset != null ? " OFFSET ?" : "") +
                (limit != null ? " LIMIT ?" : "");

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            int i = 1;
            prepStmt.setObject(i++, dataOddania);

            if (offset != null) prepStmt.setInt(i++, offset);
            if (limit != null) prepStmt.setInt(i, limit);

            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    projekty.add(new Projekt(
                            rs.getInt("projekt_id"),
                            rs.getString("nazwa"),
                            rs.getString("opis"),
                            rs.getObject("dataczas_utworzenia", LocalDateTime.class),
                            rs.getObject("data_oddania", LocalDate.class)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return projekty;
    }

    /**
     * Zwraca ilość projektów przechowywanych w bazie danych.
     * @return Ilość projektów przechowywanych w bazie danych
     */
    @Override
    public int getRowsNumber() {
        String query = "SELECT COUNT(*) FROM projekt";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query);
             ResultSet rs = prepStmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    /**
     * Zwraca ilość projektów o nazwie zgodnej z podaną.
     * @param nazwa Nazwa projektu
     * @return Ilość projektów o nazwie zgodnej z podaną w parametrze
     */
    @Override
    public int getRowsNumberWhereNazwaLike(String nazwa) {
        String query = "SELECT COUNT(*) FROM projekt WHERE nazwa LIKE ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setString(1, "%" + nazwa + "%");
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    /**
     * Zwraca ilość projektów o dacie oddania projektu zgodnej z podaną.
     * @param dataOddania Data oddania projektu
     * @return Ilość projektów o dacie oddania projektu zgodnej z podaną
     */
    @Override
    public int getRowsNumberWhereDataOddaniaIs(LocalDate dataOddania) {
        String query = "SELECT COUNT(*) FROM projekt WHERE data_oddania = ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setObject(1, dataOddania);
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}