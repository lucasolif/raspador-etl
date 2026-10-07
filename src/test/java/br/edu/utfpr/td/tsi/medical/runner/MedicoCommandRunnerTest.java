package br.edu.utfpr.td.tsi.medical.runner;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.utfpr.td.tsi.medical.model.MedicoRawDocument;
import br.edu.utfpr.td.tsi.medical.service.CfmScrapingService;
import br.edu.utfpr.td.tsi.medical.service.MedicoImportService;

class MedicoCommandRunnerTest {
    private final CfmScrapingService raspador = mock(CfmScrapingService.class);
    private final MedicoImportService importador = mock(MedicoImportService.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final JobLauncher executor = mock(JobLauncher.class);
    private final Job job = mock(Job.class);
    private final MedicoCommandRunner runner = new MedicoCommandRunner(
            raspador, importador, mongo, executor, job);

    @Test
    void naoAbreONavegadorSemMongoDb() throws Exception {
        when(mongo.executeCommand("{ ping: 1 }"))
                .thenThrow(new DataAccessResourceFailureException("Conexao recusada"));

        IllegalStateException erro = assertThrows(IllegalStateException.class, runner::run);

        assertTrue(erro.getMessage().contains("MongoDB indisponivel"));
        verify(raspador, never()).buscar(any());
    }

    @Test
    void executaBuscaImportacaoEEtlSemArgumentos() throws Exception {
        JsonNode resposta = new ObjectMapper().createObjectNode();
        when(raspador.buscar(List.of("MG", "PR"))).thenReturn(Map.of("MG", resposta, "PR", resposta));
        when(mongo.findDistinct(any(Query.class), eq("estado"), eq(MedicoRawDocument.class), eq(String.class)))
                .thenReturn(List.of("MG", "PR"));
        JobExecution execucao = mock(JobExecution.class);
        when(execucao.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(execucao.getStepExecutions()).thenReturn(Set.of());
        when(executor.run(eq(job), any(JobParameters.class))).thenReturn(execucao);

        runner.run();

        verify(importador).importar(resposta, "MG");
        verify(importador).importar(resposta, "PR");
        verify(executor).run(eq(job), any(JobParameters.class));
    }

    @Test
    void naoProcessaQuandoFaltaUmEstado() throws Exception {
        JsonNode resposta = new ObjectMapper().createObjectNode();
        when(raspador.buscar(List.of("MG", "PR"))).thenReturn(Map.of("MG", resposta, "PR", resposta));
        when(mongo.findDistinct(any(Query.class), eq("estado"), eq(MedicoRawDocument.class), eq(String.class)))
                .thenReturn(List.of("MG"));

        assertThrows(IllegalStateException.class, runner::run);
        verify(executor, never()).run(eq(job), any(JobParameters.class));
    }
}
