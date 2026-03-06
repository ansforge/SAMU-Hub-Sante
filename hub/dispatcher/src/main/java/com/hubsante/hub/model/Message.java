package com.hubsante.hub.model;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
public class Message {

    @Id
    private String id;

    /** Use case of the message (class name of ContentMessage). */
    private String type;

    /** Message arrival date, automatically populated by Spring Data auditing. */
    @CreatedDate
    private Instant arrivedAt;

    /** Original message in JSON format (stored as a BSON sub-document). */
    private Map<String, Object> payload;
}
