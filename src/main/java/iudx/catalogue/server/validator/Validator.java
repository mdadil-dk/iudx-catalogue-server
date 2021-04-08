package iudx.catalogue.server.validator;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import java.io.IOException;

public final class Validator {

  private static final String PKGBASE;
  private final JsonSchema schema;
  private static CircuitBreaker breaker;

  static {
    final String pkgName = Validator.class.getPackage().getName();
    PKGBASE = '/' + pkgName.replace(".", "/");

    breaker = CircuitBreaker.create("my-circuit-breaker", Vertx.vertx(),
        new CircuitBreakerOptions().setMaxFailures(0).setTimeout(500).setFallbackOnFailure(true)
            .setResetTimeout(10000)
    );
  }

  public Validator(String schemaPath) throws IOException, ProcessingException {
    final JsonNode schemaNode = loadResource(schemaPath);
    final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    schema = factory.getJsonSchema(schemaNode);
  }

  /**
   * Load one resource from the current package as a {@link JsonNode}.
   *
   * @param name name of the resource (<b>MUST</b> start with {@code /}
   * @return a JSON document
   * @throws IOException resource not found
   */
  public static JsonNode loadResource(final String name) throws IOException {
    return JsonLoader.fromResource(PKGBASE + name);
  }

  /**
   * Load one resource from a string {@link JsonNode}.
   *
   * @param obj Json encoded object
   * @return a JSON document
   * @throws IOException resource not found
   */
  public static JsonNode loadString(final String obj) throws IOException {
    return JsonLoader.fromString(obj);
  }

  /**
   * Check validity of json encoded string.
   *
   * @param obj Json encoded string object
   * @return isValid boolean
   */
  public boolean validate(String obj) {
    // breaker.execute(command)
    boolean isValid;
    try {
      JsonNode jsonobj = loadString(obj);
      isValid = schema.validInstance(jsonobj);
    } catch (IOException | ProcessingException e) {
      isValid = false;
    }
    return isValid;
  }
}
