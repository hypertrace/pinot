package org.apache.pinot.scalar.function.hypertrace;

import static java.util.Objects.isNull;

import org.apache.pinot.spi.annotations.ScalarFunction;
import org.hypertrace.core.attribute.service.projection.functions.Concatenate;
import org.hypertrace.core.attribute.service.projection.functions.ConcatenateOrNull;
import org.hypertrace.core.attribute.service.projection.functions.Conditional;
import org.hypertrace.core.attribute.service.projection.functions.DefaultValue;
import org.hypertrace.core.attribute.service.projection.functions.Equals;
import org.hypertrace.core.attribute.service.projection.functions.Hash;

public class HypertraceScalarFunctions {

  private static final String NULL_STRING = "null";
  public static final String HASH_FUNCTION_NAME = "hash";
  public static final String DEFAULT_STRING_FUNCTION_NAME = "defaultString";
  public static final String CONDITIONAL_FUNCTION_NAME = "conditional";
  public static final String CONCAT_OR_NULL_FUNCTION_NAME = "concatOrNull";
  public static final String CONCAT_SKIP_NULL_FUNCTION_NAME = "concatSkipNull";
  public static final String STRING_EQUALS_FUNCTION_NAME = "stringEquals";

  @ScalarFunction(names = {HASH_FUNCTION_NAME})
  public static String hash(String value) {
    return replaceNullWithNullString(Hash.hash(replaceNullStringWithNull(value)));
  }

  @ScalarFunction(names = {DEFAULT_STRING_FUNCTION_NAME})
  public static String defaultString(String value, String defaultValue) {
    return replaceNullWithNullString(
        DefaultValue.defaultString(
            replaceNullStringWithNull(value), replaceNullStringWithNull(defaultValue)));
  }

  @ScalarFunction(names = {STRING_EQUALS_FUNCTION_NAME})
  public static String stringEquals(String s1, String s2) {
    return String.valueOf(Equals.stringEquals(s1, s2));
  }

  @ScalarFunction(names = {CONDITIONAL_FUNCTION_NAME})
  public static String conditional(String condition, String s1, String s2) {
    // Pinot should never pass in a null condition but in case it does, make sure that it behaves
    // the same as passing in "null" condition.
    Boolean conditionBooleanVal =
        NULL_STRING.equals(replaceNullWithNullString(condition))
            ? null
            : Boolean.parseBoolean(condition);
    return replaceNullWithNullString(Conditional.getValue(conditionBooleanVal, s1, s2));
  }

  /**
   * This version of concat returns the null string if either or both args are null. This differs
   * from pinot's out of the box implementation which concatenates the null string with the defined
   * arg, or concatenates two null strings if both are.
   */
  @ScalarFunction(names = {CONCAT_OR_NULL_FUNCTION_NAME})
  public static String concatOrNull(String s1, String s2) {
    return replaceNullWithNullString(
        ConcatenateOrNull.concatenate(
            replaceNullStringWithNull(s1), replaceNullStringWithNull(s2)));
  }

  /**
   * This version of concat returns just one arg if it is defined and the other arg is null. If both
   * args are null, it returns the null string. This differs from pinot's out of the box
   * implementation which concatenates the null string with the defined arg, or concatenates two
   * null strings if both are.
   */
  @ScalarFunction(names = {CONCAT_SKIP_NULL_FUNCTION_NAME})
  public static String concatSkipNull(String s1, String s2) {
    return replaceNullWithNullString(
        Concatenate.concatenate(replaceNullStringWithNull(s1), replaceNullStringWithNull(s2)));
  }

  private static String replaceNullStringWithNull(String value) {
    return NULL_STRING.equals(value) ? null : value;
  }

  private static String replaceNullWithNullString(String value) {
    return isNull(value) ? NULL_STRING : value;
  }
}
