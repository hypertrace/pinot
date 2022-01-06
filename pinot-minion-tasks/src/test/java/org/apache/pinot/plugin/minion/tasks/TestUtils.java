package org.apache.pinot.plugin.minion.tasks;

import com.google.common.base.Function;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

  public static String getFileFromResourceUrl(@Nonnull URL resourceUrl) {
    // For maven cross package use case, we need to extract the resource from jar to a temporary directory.
    String resourceUrlStr = resourceUrl.toString();
    if (resourceUrlStr.contains("jar!")) {
      try {
        String extension = resourceUrlStr.substring(resourceUrlStr.lastIndexOf('.'));
        File tempFile = File.createTempFile("pinot-test-temp", extension);
        String tempFilePath = tempFile.getAbsolutePath();
        LOGGER.info("Extracting from " + resourceUrlStr + " to " + tempFilePath);
        FileUtils.copyURLToFile(resourceUrl, tempFile);
        return tempFilePath;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      return resourceUrl.getFile();
    }
  }

  /**
   * Ensure the given directories exist and are empty.
   *
   * @param dirs Directories to be cleared
   * @throws IOException
   */
  public static void ensureDirectoriesExistAndEmpty(@Nonnull File... dirs)
      throws IOException {
    for (File dir : dirs) {
      FileUtils.deleteDirectory(dir);
      Assertions.assertTrue(dir.mkdirs());
    }
  }

  /**
   * Wait for a condition to be met.
   *
   * @param condition Condition to be met
   * @param checkIntervalMs Check interval in milliseconds
   * @param timeoutMs Timeout in milliseconds
   * @param errorMessage Error message if condition is not met before timed out
   */
  public static void waitForCondition(Function<Void, Boolean> condition, long checkIntervalMs, long timeoutMs,
      @Nullable String errorMessage) {
    waitForCondition(condition, checkIntervalMs, timeoutMs, errorMessage, true);
  }

  public static void waitForCondition(Function<Void, Boolean> condition, long timeoutMs,
      @Nullable String errorMessage) {
    waitForCondition(condition, 100L, timeoutMs, errorMessage);
  }

  public static void waitForCondition(Function<Void, Boolean> condition, long checkIntervalMs, long timeoutMs,
      @Nullable String errorMessage, boolean raiseError) {
    long endTime = System.currentTimeMillis() + timeoutMs;
    String errorMessageSuffix = errorMessage != null ? ", error message: " + errorMessage : "";
    while (System.currentTimeMillis() < endTime) {
      try {
        if (Boolean.TRUE.equals(condition.apply(null))) {
          return;
        }
        Thread.sleep(checkIntervalMs);
      } catch (Exception e) {
        Assertions.fail("Caught exception while checking the condition" + errorMessageSuffix, e);
      }
    }
    if (raiseError) {
      Assertions.fail("Failed to meet condition in " + timeoutMs + "ms" + errorMessageSuffix);
    }
  }
}