package br.edu.utfpr.td.tsi.medical.batch;

import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.data.MongoCursorItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import br.edu.utfpr.td.tsi.medical.model.MedicoDocument;
import br.edu.utfpr.td.tsi.medical.model.MedicoRawDocument;
import br.edu.utfpr.td.tsi.medical.repository.MedicoRepository;
import br.edu.utfpr.td.tsi.medical.service.MedicoTransformService;

@Configuration
public class MedicoBatchConfig {
    @Bean(destroyMethod = "")
    @StepScope
    public MongoCursorItemReader<MedicoRawDocument> medicoReader(MongoTemplate mongoTemplate) {
        MongoCursorItemReader<MedicoRawDocument> leitor = new MongoCursorItemReader<>();
        leitor.setName("medicoReader");
        leitor.setTemplate(mongoTemplate);
        leitor.setQuery("{}");
        leitor.setTargetType(MedicoRawDocument.class);
        leitor.setSort(Map.of("_id", Sort.Direction.ASC));

        return leitor;
    }

    @Bean
    public Step medicoStep(
            JobRepository repositorioDeJobs,
            PlatformTransactionManager gerenciadorDeTransacoes,
            MongoCursorItemReader<MedicoRawDocument> leitor,
            MedicoTransformService transformador,
            MedicoRepository repositorioMedicos
    ) {
        return new StepBuilder("normalizarMedicos", repositorioDeJobs)
                .<MedicoRawDocument, MedicoDocument>chunk(100, gerenciadorDeTransacoes)
                .reader(leitor)
                .processor(transformador::transformar)
                .writer(lote -> repositorioMedicos.saveAll(lote.getItems()))
                .build();
    }

    @Bean
    public Job medicoJob(JobRepository repositorioDeJobs, Step medicoStep) {
        return new JobBuilder("medicoJob", repositorioDeJobs)
                .start(medicoStep)
                .build();
    }
}
