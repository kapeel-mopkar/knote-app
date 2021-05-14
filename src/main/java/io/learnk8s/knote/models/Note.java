package io.learnk8s.knote.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "notes")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Note {
    @Id
    private String id;
    private String description;
    private String fileName;

    @Override
    public String toString() {
        return description;
    }
}
