package br.edu.utfpr.td.tsi.medical.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.utfpr.td.tsi.medical.model.MedicoRawDocument;
import br.edu.utfpr.td.tsi.medical.repository.MedicoRawRepository;

class MedicoImportServiceTest {
    @Test
    void importaRespostaPublicaEGuardaCamposOriginais() throws Exception {
        ObjectMapper leitorJson = new ObjectMapper();
        JsonNode resposta = leitorJson.readTree("""
                {"status":"sucesso","dados":[{"NM_MEDICO":"EXEMPLO FICTICIO",
                "NU_CRM_NATURAL":"7312","SG_UF":"PR","ESPECIALIDADE":"CARDIOLOGIA",
                "SECURITYHASH":"nao-persistir"}]}
                """);
        MedicoRawRepository repositorio = mock(MedicoRawRepository.class);
        MedicoImportService importador = new MedicoImportService(leitorJson, repositorio);

        int quantidade = importador.importar(resposta, "PR");

        assertEquals(1, quantidade);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<MedicoRawDocument>> captura = ArgumentCaptor.forClass(Iterable.class);
        verify(repositorio).saveAll(captura.capture());
        MedicoRawDocument bruto = captura.getValue().iterator().next();
        assertEquals("PR:7312", bruto.getId());
        assertEquals("https://portal.cfm.org.br/busca-medicos", bruto.getOrigem());
        assertEquals("CARDIOLOGIA", bruto.getCamposOriginais().get("ESPECIALIDADE"));
        assertEquals(false, bruto.getCamposOriginais().containsKey("SECURITYHASH"));
    }

    @Test
    void rejeitaArquivoComUfIncorretaAntesDeGravar() throws Exception {
        ObjectMapper leitorJson = new ObjectMapper();
        JsonNode resposta = leitorJson.readTree("""
                {"dados":[{"NM_MEDICO":"EXEMPLO FICTICIO","NU_CRM":"1","SG_UF":"SC"}]}
                """);
        MedicoRawRepository repositorio = mock(MedicoRawRepository.class);
        MedicoImportService importador = new MedicoImportService(leitorJson, repositorio);

        assertThrows(IllegalArgumentException.class, () -> importador.importar(resposta, "PR"));
        verify(repositorio, never()).saveAll(any());
    }
}
