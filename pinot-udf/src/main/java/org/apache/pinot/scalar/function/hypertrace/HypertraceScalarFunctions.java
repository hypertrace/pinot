package org.apache.pinot.scalar.function.hypertrace;

import static java.util.Objects.isNull;

import org.apache.pinot.common.function.annotations.ScalarFunction;
import org.hypertrace.core.attribute.service.projection.functions.DefaultValue;
import org.hypertrace.core.attribute.service.projection.functions.Hash;

public class HypertraceScalarFunctions {

  private static final String NULL_STRING = "null";
  public static final String HASH_FUNCTION_NAME = "hash";
  public static final String DEFAULT_STRING_FUNCTION_NAME = "defaultString";

  @ScalarFunction(name = HASH_FUNCTION_NAME)
  public static String hash(String value) {
    return replaceNullWithNullString(Hash.hash(replaceNullStringWithNull(value)));
  }

  @ScalarFunction(name = DEFAULT_STRING_FUNCTION_NAME)
  public static String defaultString(String value, String defaultValue) {
    return replaceNullWithNullString(
        DefaultValue.defaultString(
            replaceNullStringWithNull(value), replaceNullStringWithNull(defaultValue)));
  }

  private static String replaceNullStringWithNull(String value) {
    return NULL_STRING.equals(value) ? null : value;
  }

  private static String replaceNullWithNullString(String value) {
    return isNull(value) ? NULL_STRING : value;
  }
}
