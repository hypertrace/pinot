package org.apache.pinot.plugin.minion.tasks;

import static org.apache.pinot.plugin.minion.tasks.CustomerBasedRetentionConstants.CUSTOMER_ID_KEY;

import java.util.HashMap;
import org.apache.pinot.core.minion.SegmentPurger.RecordPurger;
import org.apache.pinot.spi.data.readers.GenericRow;
import java.util.Map;

public class CustomerBasedRetentionPurger implements RecordPurger {

  private final Map<String, String> filterConfig;

  public CustomerBasedRetentionPurger(Map<String, String> filterConfig) {
    this.filterConfig = filterConfig;
  }

  @Override
  public boolean shouldPurge(GenericRow row) {
    Map<String ,String> customerRetentionConfigMap = getCustomerRetentionConfig();
    String customerId = filterConfig.get(CUSTOMER_ID_KEY);
    /*
      TODO : Add code here
     */
    return false;
  }

  private Map<String,String> getCustomerRetentionConfig(){
    //todo: add code here
    Map<String,String> customerRetentionConfig = new HashMap<>();
    return customerRetentionConfig;
  }
}