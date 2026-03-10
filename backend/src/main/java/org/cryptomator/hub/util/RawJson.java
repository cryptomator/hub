package org.cryptomator.hub.util;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * During serialization, this annotation will convert a JSON string into a JSON object.
 * During deserialization, it will convert a JSON object back into a JSON string.
 * <p>
 * This is useful for fields that need to store raw JSON data as a string in the database,
 *
 * @see com.fasterxml.jackson.annotation.JsonRawValue @JsonRawValue has similar functionality, but does not support deserialization.
 */
@JacksonAnnotationsInside
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JsonSerialize(using = RawJson.Serializer.class)
@JsonDeserialize(using = RawJson.Deserializer.class)
public @interface RawJson {

	class Serializer extends JsonSerializer<String> {
		private static final ObjectMapper mapper = new ObjectMapper();

		@Override
		public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			if (value == null) {
				gen.writeNull();
			} else {
				JsonNode node = mapper.readTree(value);
				gen.writeObject(node);
			}
		}
	}

	class Deserializer extends JsonDeserializer<String> {
		@Override
		public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			JsonNode node = p.readValueAsTree();
			return node.toString();
		}
	}
}
