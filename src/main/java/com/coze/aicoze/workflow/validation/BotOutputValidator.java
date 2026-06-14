package com.coze.aicoze.workflow.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class BotOutputValidator {

    private static final String SCHEMA_PREFIX = "docs/coze-prompt/schemas/";

    private final ObjectMapper objectMapper;
    private final Map<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    public BotOutputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean validate(String schemaFileName, String jsonContent) {
        if (jsonContent == null || jsonContent.isBlank()) {
            return false;
        }
        try {
            JsonSchema schema = schemaCache.computeIfAbsent(schemaFileName, this::loadSchema);
            JsonNode node = objectMapper.readTree(jsonContent);
            Set<ValidationMessage> errors = schema.validate(node);
            if (!errors.isEmpty()) {
                log.debug("Schema validation failed {}: {}", schemaFileName, errors);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.debug("Schema validation error {}: {}", schemaFileName, e.getMessage());
            return false;
        }
    }

    private JsonSchema loadSchema(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(SCHEMA_PREFIX + fileName);
            if (!resource.exists()) {
                resource = new ClassPathResource("workflow/schemas/" + fileName);
            }
            try (InputStream in = resource.getInputStream()) {
                return factory.getSchema(in);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load schema: " + fileName, e);
        }
    }
}
