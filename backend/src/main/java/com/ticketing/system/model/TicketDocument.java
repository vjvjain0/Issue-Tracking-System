package com.ticketing.system.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "tickets")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketDocument {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String priority;

    @Field(type = FieldType.Keyword)
    private String assignedAgentId;

    @Field(type = FieldType.Keyword)
    private String assignedAgentName;

    @Field(type = FieldType.Keyword)
    private String customerEmail;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String customerName;

    @Field(type = FieldType.Keyword)
    private String createdAt;

    @Field(type = FieldType.Keyword)
    private String updatedAt;

    @Field(type = FieldType.Keyword)
    private String closedAt;

    @Field(type = FieldType.Boolean)
    private boolean autoAssigned;
}