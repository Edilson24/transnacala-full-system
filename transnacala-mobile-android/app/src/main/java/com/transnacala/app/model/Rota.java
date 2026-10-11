package com.transnacala.app.model;

import java.io.Serializable;

public class Rota implements Serializable {

    private int id;
    private String nome;
    private String origem;
    private String destino;
    private double tarifa;
    private String descricao;

    public Rota() {
    }

    public Rota(int id, String nome, String origem, String destino,
                double tarifa, String descricao) {
        this.id = id;
        this.nome = nome;
        this.origem = origem;
        this.destino = destino;
        this.tarifa = tarifa;
        this.descricao = descricao;
    }

    public Rota(String nome, String origem, String destino,
                double tarifa, String descricao) {
        this.nome = nome;
        this.origem = origem;
        this.destino = destino;
        this.tarifa = tarifa;
        this.descricao = descricao;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public double getTarifa() {
        return tarifa;
    }

    public void setTarifa(double tarifa) {
        this.tarifa = tarifa;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}