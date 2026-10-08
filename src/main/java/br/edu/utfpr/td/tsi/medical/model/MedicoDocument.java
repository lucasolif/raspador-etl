package br.edu.utfpr.td.tsi.medical.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "medicos")
public class MedicoDocument {

    @Id
    private String id;
    private String nome;
    private String crm;
    private String estado;
    private List<MedicoSpecialty> especialidades;
    private MedicoEducation formacao;
    private String situacao;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCrm() {
        return crm;
    }

    public void setCrm(String crm) {
        this.crm = crm;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<MedicoSpecialty> getEspecialidades() {
        return especialidades;
    }

    public void setEspecialidades(List<MedicoSpecialty> especialidades) {
        this.especialidades = especialidades;
    }

    public MedicoEducation getFormacao() {
        return formacao;
    }

    public void setFormacao(MedicoEducation formacao) {
        this.formacao = formacao;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }
}
