package com.project.dao;

import com.project.datasource.DataSource;
import com.project.model.Zadanie;
import com.project.model.Projekt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ZadanieDAOImpl implements ZadanieDAO {
	private final ProjektDAO projektDAO = new ProjektDAOImpl();

	/**
	 * Zwraca pojedyncze zadanie na podstawie jego identyfikatora.
	 * @param zadanieId Identyfikator zadania
	 * @return Obiekt klasy {@link Zadanie}, jeśli istnieje; w przeciwnym wypadku null
	 */
    @Override
    public Zadanie getZadanie(Integer zadanieId) {
        String query = "SELECT * FROM zadanie WHERE zadanie_id = ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setInt(1, zadanieId);
            ResultSet rs = prepStmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToZadanie(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    
    /**
     * Zwraca listę wszystkich zadań w bazie danych.
     * @return Lista wszystkich zadań jako obiekty klasy Zadanie
     */
    @Override
    public List<Zadanie> getZadania() {
        String query = "SELECT * FROM zadanie";
        List<Zadanie> zadania = new ArrayList<>();

        try (Connection conn = DataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                zadania.add(mapResultSetToZadanie(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return zadania;
    }

    /**
     * Wstawia nowe zadanie lub aktualizuje istniejące. Jeżeli identyfikator zadania
     * jest null, zostanie wykonany INSERT; w przeciwnym razie UPDATE.
     * @param zadanie Obiekt zadania do zapisania lub aktualizacji
     */
    @Override
    public void setZadanie(Zadanie zadanie) {
        boolean isInsert = zadanie.getZadanieID() == null;
        String query = isInsert ?
                "INSERT INTO zadanie (nazwa, opis, kolejnosc, projekt_id,"
                + " dataczas_utworzenia) VALUES (?, ?, ?, ?, ?)"
                : "UPDATE zadanie SET nazwa = ?, opis = ?, kolejnosc = ?,"
                		+ " projekt_id = ? WHERE zadanie_id = ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(
            		 query, Statement.RETURN_GENERATED_KEYS)) {

            prepStmt.setString(1, zadanie.getNazwa());
            prepStmt.setString(2, zadanie.getOpis());
            prepStmt.setInt(3, zadanie.getKolejnosc());
            prepStmt.setInt(4, zadanie.getProjekt().getProjektId());

            if (isInsert) {
                prepStmt.setObject(5, LocalDateTime.now());
            } else {
                prepStmt.setInt(5, zadanie.getZadanieID());
            }

            int rowsAffected = prepStmt.executeUpdate();

            if (isInsert && rowsAffected > 0) {
                ResultSet keys = prepStmt.getGeneratedKeys();
                if (keys.next()) {
                    zadanie.setZadanieID(keys.getInt(1));
                }
                keys.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Usuwa zadanie na podstawie jego identyfikatora.
     * @param zadanieId Identyfikator zadania do usunięcia
     */
    @Override
    public void deleteZadanie(Integer zadanieId) {
        String query = "DELETE FROM zadanie WHERE zadanie_id = ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setInt(1, zadanieId);
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Zwraca listę zadań przypisanych do danego projektu.
     * Wyniki są posortowane według kolejności.
     * @param projektId Identyfikator projektu, którego dotyczą zadania
     * @param offset Liczba rekordów do pominięcia (paginacja); może być null
     * @param limit Maksymalna liczba rekordów do zwrócenia; może być null
     * @return Lista zadań przypisanych do projektu
     */
    @Override
    public List<Zadanie> getZadaniaByProjekt(Integer projektId, Integer offset, Integer limit) {
        List<Zadanie> zadania = new ArrayList<>();
        String query = "SELECT * FROM zadanie WHERE projekt_id = ? ORDER BY kolejnosc" +
                (offset != null ? " OFFSET ?" : "") +
                (limit != null ? " LIMIT ?" : "");

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            int i = 1;
            prepStmt.setInt(i++, projektId);
            if (offset != null) prepStmt.setInt(i++, offset);
            if (limit != null) prepStmt.setInt(i, limit);

            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    zadania.add(mapResultSetToZadanie(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return zadania;
    }

    /**
     * Zwraca listę zadań, których nazwa zawiera podany ciąg znaków,
     * przypisanych do danego projektu.
     * @param nazwa Fragment nazwy zadania do wyszukania
     * @param projektId Identyfikator projektu
     * @param offset Liczba rekordów do pominięcia (paginacja); może być null
     * @param limit Maksymalna liczba rekordów do zwrócenia; może być null
     * @return Lista pasujących zadań
     */
    @Override
    public List<Zadanie> getZadaniaWhereNazwaLike(String nazwa,
    		Integer projektId, Integer offset, Integer limit) {
        List<Zadanie> zadania = new ArrayList<>();
        String query = "SELECT * FROM zadanie WHERE projekt_id = ?"
        		+ " AND nazwa LIKE ? ORDER BY kolejnosc" +
                (offset != null ? " OFFSET ?" : "") +
                (limit != null ? " LIMIT ?" : "");

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            int i = 1;
            prepStmt.setInt(i++, projektId);
            prepStmt.setString(i++, "%" + nazwa + "%");
            if (offset != null) prepStmt.setInt(i++, offset);
            if (limit != null) prepStmt.setInt(i, limit);

            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    zadania.add(mapResultSetToZadanie(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return zadania;
    }
    
    /**
     * Zwraca listę zadań przypisanych do projektu, utworzonych w określonym dniu.
     * Filtruje na podstawie daty utworzenia (bez uwzględnienia czasu).
     * @param dateTime  Data (bez czasu), która ma zostać dopasowana.
     * @param projektId Identyfikator projektu
     * @param offset Liczba rekordów do pominięcia (paginacja); może być null
     * @param limit Maksymalna liczba rekordów do zwrócenia; może być null
     * @return Lista zadań utworzonych w danym dniu
     */
    @Override
    public List<Zadanie> getZadaniaWhereDataUtworzeniaIs(LocalDateTime dateTime,
    		Integer projektId, Integer offset, Integer limit) {
        List<Zadanie> zadania = new ArrayList<>();
        String query = "SELECT * FROM zadanie WHERE projekt_id = ?"
        		+ " AND DATE(dataczas_utworzenia) = ?" +
                (offset != null ? " OFFSET ?" : "") +
                (limit != null ? " LIMIT ?" : "");

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            int i = 1;
            prepStmt.setInt(i++, projektId);
            prepStmt.setDate(i++, java.sql.Date.valueOf(dateTime.toLocalDate()));
            if (offset != null) prepStmt.setInt(i++, offset);
            if (limit != null) prepStmt.setInt(i, limit);

            try (ResultSet rs = prepStmt.executeQuery()) {
                while (rs.next()) {
                    zadania.add(mapResultSetToZadanie(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return zadania;
    }

    /**
     * Zwraca liczbę wszystkich zadań przypisanych do konkretnego projektu.
     * @param projektId Identyfikator projektu
     * @return Liczba zadań w projekcie
     */
    @Override
    public int getRowsNumber(Integer projektId) {
        String query = "SELECT COUNT(*) FROM zadanie WHERE projekt_id = ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setInt(1, projektId);
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
     * Zwraca liczbę zadań, których nazwa zawiera podany ciąg znaków
     * i są przypisane do danego projektu.
     * @param nazwa Fragment nazwy zadania do dopasowania (LIKE)
     * @param projektId Identyfikator projektu
     * @return Liczba pasujących zadań
     */
    @Override
    public int getRowsNumberWhereNazwaLike(String nazwa, Integer projektId) {
        String query = "SELECT COUNT(*) FROM zadanie WHERE projekt_id = ? AND nazwa LIKE ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setInt(1, projektId);
            prepStmt.setString(2, "%" + nazwa + "%");

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
     * Zwraca liczbę zadań przypisanych do projektu, 
     * które zostały utworzone w określonym dniu
     * @param dateTime Data (bez czasu), dla której mają zostać policzone zadania.
     * @param projektId Identyfikator projektu
     * @return Liczba zadań utworzonych w danym dniu
     */
    @Override
    public int getRowsNumberWhereDataUtworzeniaIs(
    		LocalDateTime dateTime, Integer projektId) {
        String query = "SELECT COUNT(*) FROM zadanie WHERE projekt_id = ?"
        		+ " AND DATE(dataczas_utworzenia) = ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query)) {

            prepStmt.setInt(1, projektId);
            prepStmt.setDate(2, java.sql.Date.valueOf(dateTime.toLocalDate()));

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
     * Prywatna metoda pomocnicza konwertująca rekord z ResultSet na obiekt Zadanie.
     * @param rs ResultSet zawierający dane jednego rekordu
     * @return Obiekt klasy Zadanie
     * @throws SQLException W przypadku problemów z odczytem danych
     */
    private Zadanie mapResultSetToZadanie(ResultSet rs) throws SQLException {
        int projektId = rs.getInt("projekt_id");
        Projekt projekt = new Projekt();
        projekt.setProjektId(projektId);

        return new Zadanie(
            rs.getInt("zadanie_id"),
            rs.getString("nazwa"),
            rs.getString("opis"),
            rs.getInt("kolejnosc"),
            rs.getObject("dataczas_utworzenia", LocalDateTime.class),
            projekt
        );
    }
}
