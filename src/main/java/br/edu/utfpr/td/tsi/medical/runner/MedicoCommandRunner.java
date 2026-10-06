package br.edu.utfpr.td.tsi.medical.runner;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import br.edu.utfpr.td.tsi.medical.model.MedicoRawDocument;
import br.edu.utfpr.td.tsi.medical.service.MedicoImportService;

@Component
public class MedicoCommandRunner implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicoCommandRunner.class);
    private final MedicoImportService importador;
    private final MongoTemplate mongoTemplate;
    private final JobLauncher executor;
    private final Job medicoJob;

    public MedicoCommandRunner(MedicoImportService importador, MongoTemplate mongoTemplate,
            JobLauncher executor, Job medicoJob) {
        this.importador = importador;
        this.mongoTemplate = mongoTemplate;
        this.executor = executor;
        this.medicoJob = medicoJob;
    }

    @Override
    public void run(String... argumentos) throws Exception {
        if (argumentos.length == 0 || contem(argumentos, "--ajuda")) {
            LOGGER.info("Uso: java -jar trabalho-01-cfm-1.0.0.jar "
                    + "\"--arquivo=PR=C:\\caminho\\pr.json\" "
                    + "\"--arquivo=SC=C:\\caminho\\sc.json\" --processar");
            return;
        }

        boolean processar = false;
        for (String argumento : argumentos) {
            if (argumento.startsWith("--arquivo=")) {
                String valor = argumento.substring("--arquivo=".length());
                int separador = valor.indexOf('=');
                if (separador <= 0 || separador == valor.length() - 1) {
                    throw new IllegalArgumentException("Use --arquivo=UF=caminho.json");
                }
                String uf = valor.substring(0, separador);
                Path caminho = Path.of(valor.substring(separador + 1));
                int quantidade = importador.importar(caminho, uf);
                LOGGER.info("Importados {} registros de {}", quantidade, uf);
            } else if (argumento.equals("--processar")) {
                processar = true;
            } else {
                throw new IllegalArgumentException("Argumento desconhecido: " + argumento);
            }
        }

        if (processar) {
            List<String> estados = mongoTemplate.findDistinct(new Query(), "estado",
                    MedicoRawDocument.class, String.class);
            Set<String> estadosValidos = new HashSet<>(estados);
            estadosValidos.remove(null);
            estadosValidos.remove("");
            if (estadosValidos.size() < 2) {
                throw new IllegalStateException("Importe dados de pelo menos duas UFs antes do ETL.");
            }

            JobParameters parametros = new JobParametersBuilder()
                    .addLong("instante", System.currentTimeMillis())
                    .toJobParameters();
            JobExecution execucao = executor.run(medicoJob, parametros);
            LOGGER.info("ETL concluido: estado={}, lidos={}, gravados={}",
                    execucao.getStatus(),
                    execucao.getStepExecutions().stream().mapToLong(e -> e.getReadCount()).sum(),
                    execucao.getStepExecutions().stream().mapToLong(e -> e.getWriteCount()).sum());
            if (execucao.getStatus() != BatchStatus.COMPLETED) {
                throw new IllegalStateException("O job falhou: " + execucao.getStatus());
            }
        }
    }

    private boolean contem(String[] argumentos, String valor) {
        for (String argumento : argumentos) {
            if (argumento.equals(valor)) {
                return true;
            }
        }
        return false;
    }
}
