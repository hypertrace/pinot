package org.apache.pinot.scalar.function.hypertrace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hypertrace.core.attribute.service.projection.functions.Hash;
import org.junit.jupiter.api.Test;

public class HypertraceScalarFunctionsTest {
  @Test
  public void testHash() {
    assertEquals(Hash.hash("foo"), HypertraceScalarFunctions.hash("foo"));
    assertEquals("null", HypertraceScalarFunctions.hash("null"));
    assertEquals("null", HypertraceScalarFunctions.hash(null));
  }

  @Test
  public void testDefaultString() {
    assertEquals("foo", HypertraceScalarFunctions.defaultString("foo", "bar"));
    assertEquals("bar", HypertraceScalarFunctions.defaultString("", "bar"));
    assertEquals("bar", HypertraceScalarFunctions.defaultString("null", "bar"));
    assertEquals("null", HypertraceScalarFunctions.defaultString("", "null"));
  }

  @Test
  public void testStringEquals() {
    assertEquals("true", HypertraceScalarFunctions.stringEquals("foo", "foo"));
    assertEquals("false", HypertraceScalarFunctions.stringEquals("foo", ""));
  }

  @Test
  public void testConditional() {
    assertEquals("foo", HypertraceScalarFunctions.conditional("true", "foo", "bar"));
    assertEquals("foo", HypertraceScalarFunctions.conditional("TRuE", "foo", "bar"));
    assertEquals("bar", HypertraceScalarFunctions.conditional("FALSE", "foo", "bar"));
    assertEquals("null", HypertraceScalarFunctions.conditional("null", "foo", "bar"));
    assertEquals("bar", HypertraceScalarFunctions.conditional(null, "foo", "bar"));
    assertEquals("null", HypertraceScalarFunctions.conditional("false", "foo", null));
  }
}
