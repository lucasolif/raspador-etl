package br.edu.utfpr.td.tsi.medical.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import br.edu.utfpr.td.tsi.medical.model.MedicoDocument;

public interface MedicoRepository extends MongoRepository<MedicoDocument, String> { }
