package br.edu.utfpr.td.tsi.medical.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.utfpr.td.tsi.medical.model.MedicoRawDocument;
import br.edu.utfpr.td.tsi.medical.repository.MedicoRawRepository;

@Service
public class MedicoImportService {

    private final ObjectMapper leitorJson;
    private final MedicoRawRepository repositorio;

    public MedicoImportService(
            ObjectMapper leitorJson,
            MedicoRawRepository repositorio
    ) {
        this.leitorJson = leitorJson;
        this.repositorio = repositorio;
    }

    public int importar(JsonNode resposta, String ufInformada) {
        String uf = ufInformada.trim().toUpperCase(Locale.ROOT);
        if (!uf.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("Informe uma UF com duas letras: " + ufInformada);
        }

        JsonNode registros = resposta.path("dados");
        if (!registros.isArray() || registros.isEmpty()) {
            throw new IllegalArgumentException("A consulta do CFM nao retornou medicos para " + uf);
        }

        List<MedicoRawDocument> medicos = new ArrayList<>();

        for (int i = 0; i < registros.size(); i++) {

            JsonNode registro = registros.get(i);
            String estado = texto(registro, "SG_UF").toUpperCase(Locale.ROOT);
            String crm = texto(registro, "NU_CRM_NATURAL");

            if (crm.isBlank()) {
                crm = texto(registro, "NU_CRM");
            }

            if (!uf.equals(estado) || crm.isBlank() || texto(registro, "NM_MEDICO").isBlank()) {
                throw new IllegalArgumentException("Registro " + (i + 1) + " sem nome/CRM ou com UF diferente de " + uf);
            }

            Map<String, Object> campos = leitorJson.convertValue(registro, new TypeReference<LinkedHashMap<String, Object>>() { });
            campos.remove("SECURITYHASH");

            MedicoRawDocument medico = new MedicoRawDocument();
            medico.setId(estado + ":" + crm);
            medico.setCrm(crm);
            medico.setEstado(estado);
            medico.setOrigem("https://portal.cfm.org.br/busca-medicos");
            medico.setDataImportacao(LocalDateTime.now());
            medico.setCamposOriginais(campos);

            medicos.add(medico);
        }

        this.repositorio.saveAll(medicos);
        return medicos.size();
    }

    private String texto(JsonNode registro, String campo) {
        JsonNode valor = registro.path(campo);

        return valor.isMissingNode() || valor.isNull() ? "" : valor.asText().trim();
    }
}
