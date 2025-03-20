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
     * @param projektId 
     * @return
     */
    @Override
    public Projekt getProjekt(Integer projektId) {
        return null;
    }

    /**
     * @param projekt 
     */
    @Override
    public void setProjekt(Projekt projekt){
        boolean isInsert = projekt.getProjektId() == null;
        String query = isInsert ?
                "INSERT INTO projekt(nazwa, opis, dataczas_utworzenia, data_oddania) VALUES (?, ?, ?, ?)"
                : "UPDATE projekt SET nazwa = ?, opis = ?, dataczas_utworzenia = ?, data_oddania = ?"
                + " WHERE projekt_id = ?";

        try (Connection connect = DataSource.getConnection();
             PreparedStatement prepStmt = connect.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            //Wstawianie do zapytania odpowiednich wartości w miejsce znaków '?'
            //Uwaga! Indeksowanie znaków '?' zaczyna się od 1!
            prepStmt.setString(1, projekt.getNazwa());

            prepStmt.setString(2, projekt.getOpis());

            if(projekt.getDataCzasUtworzenia() == null)
                projekt.setDataCzasUtworzenia(LocalDateTime.now());

            prepStmt.setObject(3,projekt.getDataCzasUtworzenia());
            prepStmt.setObject(4, projekt.getDataOddania());

            if(!isInsert) prepStmt.setInt(5, projekt.getProjektId());

            //Wysyłanie zapytania i pobieranie danych
            int liczbaDodanychWierszy = prepStmt.executeUpdate();

            //Pobieranie kluczy głównych, tylko dla nowo utworzonych projektów
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
     * @param projektId 
     */
    @Override
    public void deleteProjekt(Integer projektId) {

    }

    /**
     * @param offset 
     * @param limit
     * @return
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
                        projekt.setDataCzasUtworzenia(rs.getObject("dataczas_utworzenia", LocalDateTime.class));
                        projekt.setDataOddania(rs.getObject("data_oddania", LocalDate.class));
                        projekty.add(projekt);
                    }
                }
            }catch(SQLException e) {
                throw new RuntimeException(e);
            }
        return projekty;
    }

    /**
     * @param nazwa 
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public List<Projekt> getProjektyWhereNazwaLike(String nazwa, Integer offset, Integer limit) {
        return List.of();
    }

    /**
     * @param dataOddania 
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public List<Projekt> getProjektyWhereDataOddaniaIs(LocalDate dataOddania, Integer offset, Integer limit) {
        return List.of();
    }

    /**
     * @return 
     */
    @Override
    public int getRowsNumber() {
        return 0;
    }

    /**
     * @param nazwa 
     * @return
     */
    @Override
    public int getRowsNumberWhereNazwaLike(String nazwa) {
        return 0;
    }

    /**
     * @param dataOddania 
     * @return
     */
    @Override
    public int getRowsNumberWhereDataOddaniaIs(LocalDate dataOddania) {
        return 0;
    }
}
