package com.trinhcong1120.survey_service.dto.question;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class OptionRequestDeserializer extends JsonDeserializer<OptionRequest> {

    @Override
    public OptionRequest deserialize(
            JsonParser parser,
            DeserializationContext context
    ) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        OptionRequest request = new OptionRequest();

        if (node == null || node.isNull()) {
            return request;
        }

        if (node.isTextual()) {
            request.setOptionText(node.asText());
            return request;
        }

        request.setOptionText(textValue(node, "optionText"));

        return request;
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);

        if (field == null || field.isNull()) {
            return null;
        }

        return field.asText();
    }
}
