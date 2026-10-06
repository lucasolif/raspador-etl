package br.edu.utfpr.td.tsi.medical.model;

public class MedicoEducation {
    private String instituicao;
    private String campus;
    private Integer anoGraduacao;

    public String getInstituicao() { return instituicao; }
    public void setInstituicao(String instituicao) { this.instituicao = instituicao; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public Integer getAnoGraduacao() { return anoGraduacao; }
    public void setAnoGraduacao(Integer anoGraduacao) { this.anoGraduacao = anoGraduacao; }
}
