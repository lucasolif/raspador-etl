package br.edu.utfpr.td.tsi.medical.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import br.edu.utfpr.td.tsi.medical.model.MedicoDocument;
import br.edu.utfpr.td.tsi.medical.model.MedicoEducation;
import br.edu.utfpr.td.tsi.medical.model.MedicoRawDocument;
import br.edu.utfpr.td.tsi.medical.model.MedicoSpecialty;

@Service
public class MedicoTransformService {
    private static final Pattern PADRAO_ANO = Pattern.compile("\\b(?:19|20)\\d{2}\\b");
    private static final Pattern PADRAO_RQE = Pattern.compile("(?i)\\s*-\\s*RQE\\s*(?:N[º°.]?)?\\s*:?\\s*");

    public MedicoDocument transformar(MedicoRawDocument bruto) {
        Map<String, Object> campos = bruto.getCamposOriginais();
        MedicoDocument medico = new MedicoDocument();
        medico.setId(bruto.getId());
        medico.setNome(texto(campos, "NM_MEDICO"));
        medico.setCrm(bruto.getCrm());
        medico.setEstado(bruto.getEstado());
        medico.setSituacao(texto(campos, "SITUACAO"));
        medico.setEspecialidades(separarEspecialidades(texto(campos, "ESPECIALIDADE")));
        medico.setFormacao(criarFormacao(
                texto(campos, "NM_INSTITUICAO_GRADUACAO"),
                texto(campos, "DT_GRADUACAO")));
        return medico;
    }

    private MedicoEducation criarFormacao(String instituicaoOriginal, String dataGraduacao) {
        MedicoEducation formacao = new MedicoEducation();
        String[] partes = instituicaoOriginal.split("\\s+-\\s+", 2);

        formacao.setInstituicao(partes[0].isBlank() ? null : partes[0].trim());
        if (partes.length > 1 && !partes[1].isBlank()) {
            formacao.setCampus(partes[1].trim());
        }

        Matcher ano = PADRAO_ANO.matcher(dataGraduacao);
        if (ano.find()) {
            formacao.setAnoGraduacao(Integer.parseInt(ano.group()));
        }
        return formacao;
    }

    private List<MedicoSpecialty> separarEspecialidades(String textoOriginal) {
        List<MedicoSpecialty> especialidades = new ArrayList<>();
        if (textoOriginal.isBlank()) {
            return especialidades;
        }

        for (String item : textoOriginal.split("[&;]")) {
            String valor = item.trim();
            if (valor.isBlank()) {
                continue;
            }
            String[] partes = PADRAO_RQE.split(valor, 2);
            String descricao = partes[0].trim();
            String rqe = partes.length > 1 && !partes[1].isBlank() ? partes[1].trim() : null;
            especialidades.add(new MedicoSpecialty(descricao, rqe));
        }
        return especialidades;
    }

    private String texto(Map<String, Object> campos, String nome) {
        Object valor = campos.get(nome);
        return valor == null ? "" : valor.toString().trim();
    }
}
