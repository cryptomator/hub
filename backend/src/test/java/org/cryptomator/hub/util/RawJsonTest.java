package org.cryptomator.hub.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RawJsonTest {

	record TestEntity(@JsonProperty("str") String str, @JsonProperty("raw") @RawJson String raw) {
	}

	@Test
	@DisplayName("serialize entity with @RawJson field containing JSON string")
	public void testSerialization() throws Exception {
		TestEntity entity = new TestEntity("test", "{\"key\":\"value\"}");
		ObjectMapper objectMapper = new ObjectMapper();

		String json = objectMapper.writeValueAsString(entity);

		MatcherAssert.assertThat(json, Matchers.containsString("""
				"raw":{"key":"value"}\
				"""));
		MatcherAssert.assertThat(json, Matchers.containsString("""
				"str":"test"\
				"""));
	}

	@Test
	@DisplayName("serialize entity with @RawJson field containing null")
	public void testNullSerialization() throws Exception {
		TestEntity entity = new TestEntity("test", null);
		ObjectMapper objectMapper = new ObjectMapper();

		String json = objectMapper.writeValueAsString(entity);

		MatcherAssert.assertThat(json, Matchers.containsString("""
				"raw":null\
				"""));
		MatcherAssert.assertThat(json, Matchers.containsString("""
				"str":"test"\
				"""));
	}

	@Test
	@DisplayName("fail serialization if @RawJson field contains non-JSON string")
	public void testBrokenSerialization() throws Exception {
		TestEntity entity = new TestEntity("test", "NOT JSON");
		ObjectMapper objectMapper = new ObjectMapper();

		Assertions.assertThrows(JsonMappingException.class, () -> {
			objectMapper.writeValueAsString(entity);
		});
	}

	@Test
	@DisplayName("deserialize json with arbitrary data in @RawJson field")
	public void testDeserialization() throws Exception {
		String json = """
				{
					"str": "test",
					"raw": {"key": 42}
				}
				""";
		ObjectMapper objectMapper = new ObjectMapper();

		TestEntity entity = objectMapper.readValue(json, TestEntity.class);

		Assertions.assertEquals("test", entity.str);
		Assertions.assertEquals("{\"key\":42}", entity.raw);
	}

	@Test
	@DisplayName("deserialize json with null in @RawJson field")
	public void testNullDeserialization() throws Exception {
		String json = """
				{
					"str": "test",
					"raw": null
				}
				""";
		ObjectMapper objectMapper = new ObjectMapper();

		TestEntity entity = objectMapper.readValue(json, TestEntity.class);

		Assertions.assertEquals("test", entity.str);
		Assertions.assertNull(entity.raw);
	}

	@Test
	@DisplayName("deserialize json with missing @RawJson field")
	public void testUndefinedDeserialization() throws Exception {
		String json = """
				{
					"str": "test"
				}
				""";
		ObjectMapper objectMapper = new ObjectMapper();

		TestEntity entity = objectMapper.readValue(json, TestEntity.class);

		Assertions.assertEquals("test", entity.str);
		Assertions.assertNull(entity.raw);
	}

}