package br.edu.utfpr.td.tsi.medical.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.edu.utfpr.td.tsi.medical.model.MedicoDocument;
import br.edu.utfpr.td.tsi.medical.model.MedicoRawDocument;

class MedicoTransformServiceTest {
    private final MedicoTransformService transformador = new MedicoTransformService();

    @Test
    void transformaCamposDoCfmNoFormatoSolicitado() {
        MedicoRawDocument bruto = new MedicoRawDocument();
        bruto.setId("PR:7312");
        bruto.setCrm("7312");
        bruto.setEstado("PR");
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("NM_MEDICO", "BRUNO HENRIQUE ALVES");
        campos.put("SITUACAO", "Regular");
        campos.put("ESPECIALIDADE", "CIRURGIA GERAL - RQE Nº: 3497&CIRURGIA VASCULAR - RQE N.: 3778");
        campos.put("NM_INSTITUICAO_GRADUACAO", "UNIVERSIDADE FEDERAL DO PARANÁ - Colombo");
        campos.put("DT_GRADUACAO", "2008");
        bruto.setCamposOriginais(campos);

        MedicoDocument medico = transformador.transformar(bruto);

        assertEquals("PR:7312", medico.getId());
        assertEquals("BRUNO HENRIQUE ALVES", medico.getNome());
        assertEquals("7312", medico.getCrm());
        assertEquals("PR", medico.getEstado());
        assertEquals(2, medico.getEspecialidades().size());
        assertEquals("CIRURGIA GERAL", medico.getEspecialidades().get(0).getDescricao());
        assertEquals("3497", medico.getEspecialidades().get(0).getRqe());
        assertEquals("3778", medico.getEspecialidades().get(1).getRqe());
        assertEquals("UNIVERSIDADE FEDERAL DO PARANÁ", medico.getFormacao().getInstituicao());
        assertEquals("Colombo", medico.getFormacao().getCampus());
        assertEquals(2008, medico.getFormacao().getAnoGraduacao());
    }

    @Test
    void mantemCamposDesconhecidosComoAusentes() {
        MedicoRawDocument bruto = new MedicoRawDocument();
        bruto.setId("SC:1");
        bruto.setCrm("1");
        bruto.setEstado("SC");
        bruto.setCamposOriginais(Map.of("NM_MEDICO", "EXEMPLO FICTICIO"));

        MedicoDocument medico = transformador.transformar(bruto);

        assertEquals(0, medico.getEspecialidades().size());
        assertNull(medico.getFormacao().getAnoGraduacao());
        assertNull(medico.getFormacao().getInstituicao());
    }
}
