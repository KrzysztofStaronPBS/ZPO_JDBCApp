package com.project.dao;
import java.util.List;
import com.project.model.Projekt;
import java.time.LocalDate;
public interface ProjektDAO {
    Projekt getProjekt(Integer projektId);
    void setProjekt(Projekt projekt);
    void deleteProjekt(Integer projektId);
    List<Projekt> getProjekty(Integer offset, Integer limit);
    List<Projekt> getProjektyWhereNazwaLike(String nazwa, Integer offset, Integer limit);
    List<Projekt> getProjektyWhereDataOddaniaIs(LocalDate dataOddania, Integer offset, Integer limit);
    int getRowsNumber();
    int getRowsNumberWhereNazwaLike(String nazwa);
    int getRowsNumberWhereDataOddaniaIs(LocalDate dataOddania);
}