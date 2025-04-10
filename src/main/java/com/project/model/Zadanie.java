package com.project.model;

import java.time.LocalDateTime;

public class Zadanie {
	private Integer zadanieID;
	private String nazwa;
	private String opis;
	private Integer kolejnosc;
	private LocalDateTime dataCzasUtworzenia;
	private Projekt projekt;

	public Zadanie() {
	}
	
	public Zadanie(Integer zadanieID, String nazwa, String opis, Integer kolejnosc, LocalDateTime dataCzasUtworzenia) {
		super();
		this.zadanieID = zadanieID;
		this.nazwa = nazwa;
		this.opis = opis;
		this.kolejnosc = kolejnosc;
		this.dataCzasUtworzenia = dataCzasUtworzenia;
	}
	public Zadanie(Integer zadanieID, String nazwa, String opis, Integer kolejnosc, LocalDateTime dataCzasUtworzenia,
			Projekt projekt) {
		super();
		this.zadanieID = zadanieID;
		this.nazwa = nazwa;
		this.opis = opis;
		this.kolejnosc = kolejnosc;
		this.dataCzasUtworzenia = dataCzasUtworzenia;
		this.projekt = projekt;
	}

	public Integer getZadanieID() {
		return zadanieID;
	}
	public void setZadanieID(Integer zadanieID) {
		this.zadanieID = zadanieID;
	}
	public String getNazwa() {
		return nazwa;
	}
	public void setNazwa(String nazwa) {
		this.nazwa = nazwa;
	}
	public String getOpis() {
		return opis;
	}
	public void setOpis(String opis) {
		this.opis = opis;
	}
	public Integer getKolejnosc() {
		return kolejnosc;
	}
	public void setKolejnosc(Integer kolejnosc) {
		this.kolejnosc = kolejnosc;
	}
	public LocalDateTime getDataCzasUtworzenia() {
		return dataCzasUtworzenia;
	}
	public void setDataCzasUtworzenia(LocalDateTime dataCzasUtworzenia) {
		this.dataCzasUtworzenia = dataCzasUtworzenia;
	}
	public Projekt getProjekt() {
		return projekt;
	}
	public void setProjekt(Projekt projekt) {
		this.projekt = projekt;
	}
}