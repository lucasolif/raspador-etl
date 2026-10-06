package br.edu.utfpr.td.tsi.medical.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.utfpr.td.tsi.medical.model.MedicoRawDocument;
import br.edu.utfpr.td.tsi.medical.repository.MedicoRawRepository;

class MedicoImportServiceTest {
    @TempDir
    Path pasta;

    @Test
    void importaRespostaPublicaEGuardaCamposOriginais() throws Exception {
        Path arquivo = pasta.resolve("pr.json");
        Files.writeString(arquivo, """
                {"status":"sucesso","dados":[{"NM_MEDICO":"EXEMPLO FICTICIO",
                "NU_CRM_NATURAL":"7312","SG_UF":"PR","ESPECIALIDADE":"CARDIOLOGIA",
                "SECURITYHASH":"nao-persistir"}]}
                """);
        MedicoRawRepository repositorio = mock(MedicoRawRepository.class);
        MedicoImportService importador = new MedicoImportService(new ObjectMapper(), repositorio);

        int quantidade = importador.importar(arquivo, "PR");

        assertEquals(1, quantidade);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<MedicoRawDocument>> captura = ArgumentCaptor.forClass(Iterable.class);
        verify(repositorio).saveAll(captura.capture());
        MedicoRawDocument bruto = captura.getValue().iterator().next();
        assertEquals("PR:7312", bruto.getId());
        assertEquals("CARDIOLOGIA", bruto.getCamposOriginais().get("ESPECIALIDADE"));
        assertEquals(false, bruto.getCamposOriginais().containsKey("SECURITYHASH"));
    }

    @Test
    void rejeitaArquivoComUfIncorretaAntesDeGravar() throws Exception {
        Path arquivo = pasta.resolve("sc.json");
        Files.writeString(arquivo, """
                {"dados":[{"NM_MEDICO":"EXEMPLO FICTICIO","NU_CRM":"1","SG_UF":"SC"}]}
                """);
        MedicoRawRepository repositorio = mock(MedicoRawRepository.class);
        MedicoImportService importador = new MedicoImportService(new ObjectMapper(), repositorio);

        assertThrows(IllegalArgumentException.class, () -> importador.importar(arquivo, "PR"));
        verify(repositorio, never()).saveAll(any());
    }
}
