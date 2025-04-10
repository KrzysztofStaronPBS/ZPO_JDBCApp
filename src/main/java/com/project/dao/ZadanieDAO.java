package com.project.dao;

import com.project.model.Zadanie;

import java.time.LocalDateTime;

import java.util.List;

public interface ZadanieDAO {

    Zadanie getZadanie(Integer zadanieId);
    
    List<Zadanie> getZadania();

    void setZadanie(Zadanie zadanie);

    void deleteZadanie(Integer zadanieId);

    List<Zadanie> getZadaniaByProjekt(Integer projektId, Integer offset, Integer limit);

    List<Zadanie> getZadaniaWhereNazwaLike(String nazwa, Integer projektId, Integer offset, Integer limit);
    
    public List<Zadanie> getZadaniaWhereDataUtworzeniaIs(LocalDateTime dateTime, Integer projektId, Integer offset, Integer limit);

    int getRowsNumber(Integer projektId);

    int getRowsNumberWhereNazwaLike(String nazwa, Integer projektId);
    
    public int getRowsNumberWhereDataUtworzeniaIs(LocalDateTime dateTime, Integer projektId);
}
