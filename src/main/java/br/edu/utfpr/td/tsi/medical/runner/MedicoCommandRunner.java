package br.edu.utfpr.td.tsi.medical.runner;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import br.edu.utfpr.td.tsi.medical.model.MedicoRawDocument;
import br.edu.utfpr.td.tsi.medical.service.CfmScrapingService;
import br.edu.utfpr.td.tsi.medical.service.MedicoImportService;

@Component
public class MedicoCommandRunner implements CommandLineRunner {

    private static final List<String> ESTADOS = List.of("MG", "PR");
    private final CfmScrapingService raspador;
    private final MedicoImportService importador;
    private final MongoTemplate mongoTemplate;
    private final JobLauncher executor;
    private final Job medicoJob;

    public MedicoCommandRunner(
            CfmScrapingService raspador,
            MedicoImportService importador,
            MongoTemplate mongoTemplate,
            JobLauncher executor,
            Job medicoJob
    ) {
        this.raspador = raspador;
        this.importador = importador;
        this.mongoTemplate = mongoTemplate;
        this.executor = executor;
        this.medicoJob = medicoJob;
    }

    @Override
    public void run(String... argumentos) throws Exception {
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
        } catch (RuntimeException erro) {
            throw new IllegalStateException("MongoDB indisponivel. Inicie o servidor configurado em MONGODB_URI antes da coleta.", erro);
        }

        Map<String, JsonNode> respostas = this.raspador.buscar(ESTADOS);
        for (Map.Entry<String, JsonNode> consulta : respostas.entrySet()) {
            this.importador.importar(consulta.getValue(), consulta.getKey());
        }

        List<String> estados = this.mongoTemplate.findDistinct(new Query(), "estado", MedicoRawDocument.class, String.class);
        Set<String> estadosValidos = new HashSet<>(estados);
        estadosValidos.remove(null);
        estadosValidos.remove("");
        if (!estadosValidos.containsAll(ESTADOS)) {
            throw new IllegalStateException("A importacao precisa conter medicos de MG e PR antes do ETL.");
        }

        JobParameters parametros = new JobParametersBuilder()
                .addLong("instante", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execucao = this.executor.run(this.medicoJob, parametros);

        if (execucao.getStatus() != BatchStatus.COMPLETED) {
            throw new IllegalStateException("O job falhou: " + execucao.getStatus());
        }
    }
}
