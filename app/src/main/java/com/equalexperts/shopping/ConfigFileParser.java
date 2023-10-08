package com.equalexperts.shopping;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigFileParser {

  private static ConfigFileParser instance = null;
  private Properties properties = null;

  private static ConfigFileParser getInstance() throws IOException {
    if (instance == null) {
      instance = new ConfigFileParser();
      instance.initialize();
    }
    return instance;
  }

  private void initialize() throws IOException {
    InputStream input = Cart.class.getClassLoader().getResourceAsStream("config.properties");
    properties = new Properties();

    properties.load(input);
  }


  public static Properties getProperties() throws IOException {
    return getInstance().properties;
  }
}
