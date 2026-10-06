package br.edu.utfpr.td.tsi.medical.model;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "medicos_brutos")
public class MedicoRawDocument {
    @Id
    private String id;
    private String crm;
    private String estado;
    private String arquivoOrigem;
    private LocalDateTime dataImportacao;
    private Map<String, Object> camposOriginais;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCrm() { return crm; }
    public void setCrm(String crm) { this.crm = crm; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getArquivoOrigem() { return arquivoOrigem; }
    public void setArquivoOrigem(String arquivoOrigem) { this.arquivoOrigem = arquivoOrigem; }
    public LocalDateTime getDataImportacao() { return dataImportacao; }
    public void setDataImportacao(LocalDateTime dataImportacao) { this.dataImportacao = dataImportacao; }
    public Map<String, Object> getCamposOriginais() { return camposOriginais; }
    public void setCamposOriginais(Map<String, Object> camposOriginais) { this.camposOriginais = camposOriginais; }
}
