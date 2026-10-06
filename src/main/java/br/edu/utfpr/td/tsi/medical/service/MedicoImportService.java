package br.edu.utfpr.td.tsi.medical.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public MedicoImportService(ObjectMapper leitorJson, MedicoRawRepository repositorio) {
        this.leitorJson = leitorJson;
        this.repositorio = repositorio;
    }

    public int importar(Path caminho, String ufInformada) throws IOException {
        if (!Files.isRegularFile(caminho)) {
            throw new IllegalArgumentException("Arquivo nao encontrado: " + caminho);
        }

        String uf = ufInformada.trim().toUpperCase(Locale.ROOT);
        if (!uf.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("Informe uma UF com duas letras: " + ufInformada);
        }

        JsonNode resposta = leitorJson.readTree(caminho.toFile());
        JsonNode registros = resposta.isArray() ? resposta : resposta.path("dados");
        if (!registros.isArray() || registros.isEmpty()) {
            throw new IllegalArgumentException("O arquivo deve conter uma lista nao vazia em 'dados': " + caminho);
        }

        List<MedicoRawDocument> medicos = new ArrayList<>();
        for (int indice = 0; indice < registros.size(); indice++) {
            JsonNode registro = registros.get(indice);
            String estado = texto(registro, "SG_UF").toUpperCase(Locale.ROOT);
            String crm = texto(registro, "NU_CRM_NATURAL");
            if (crm.isBlank()) {
                crm = texto(registro, "NU_CRM");
            }
            if (!uf.equals(estado) || crm.isBlank() || texto(registro, "NM_MEDICO").isBlank()) {
                throw new IllegalArgumentException("Registro " + (indice + 1)
                        + " sem nome/CRM ou com UF diferente de " + uf + " em " + caminho);
            }

            Map<String, Object> campos = leitorJson.convertValue(registro,
                    new TypeReference<LinkedHashMap<String, Object>>() { });
            // O token usado pelo site para fotos nao faz parte dos dados do trabalho.
            campos.remove("SECURITYHASH");

            MedicoRawDocument medico = new MedicoRawDocument();
            medico.setId(estado + ":" + crm);
            medico.setCrm(crm);
            medico.setEstado(estado);
            medico.setArquivoOrigem(caminho.getFileName().toString());
            medico.setDataImportacao(LocalDateTime.now());
            medico.setCamposOriginais(campos);
            medicos.add(medico);
        }

        repositorio.saveAll(medicos);
        return medicos.size();
    }

    private String texto(JsonNode registro, String campo) {
        JsonNode valor = registro.path(campo);
        return valor.isMissingNode() || valor.isNull() ? "" : valor.asText().trim();
    }
}
