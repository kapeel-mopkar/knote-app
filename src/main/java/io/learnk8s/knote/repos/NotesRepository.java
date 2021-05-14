package io.learnk8s.knote.repos;

import org.springframework.data.mongodb.repository.MongoRepository;
import io.learnk8s.knote.models.Note;

public interface NotesRepository extends MongoRepository<Note, String> {}
