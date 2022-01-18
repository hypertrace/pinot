package org.apache.pinot.plugin.minion.tasks;

import static org.apache.pinot.plugin.minion.tasks.CustomerBasedRetentionConstants.TASK_TYPE;

import org.I0Itec.zkclient.exception.ZkException;
import org.apache.helix.AccessOption;
import org.apache.helix.ZNRecord;
import org.apache.helix.store.HelixPropertyStore;
import org.apache.pinot.common.metadata.ZKMetadataProvider;

public class CustomerBasedRetentionTaskUtils {
  public static void setCustomerBasedRetentionTaskMetadata(CustomerBasedRetentionTaskMetadata customerBasedRetentionTaskMetadata,
      HelixPropertyStore<ZNRecord> propertyStore, int expectedVersion) {
    String path = ZKMetadataProvider.constructPropertyStorePathForMinionTaskMetadata(TASK_TYPE,
        customerBasedRetentionTaskMetadata.getTableNameWithType());
    if (!propertyStore.set(path, customerBasedRetentionTaskMetadata.toZNRecord(), expectedVersion, AccessOption.PERSISTENT)) {
      throw new ZkException(
          "Failed to persist minion CustomerBasedRetentionTask metadata: " + customerBasedRetentionTaskMetadata);
    }
  }
}
