package com.hubsante;

import java.util.Map;
public class Shipto {
  private String name;
  private String address;
  private Map<String, Object> additionalProperties;

  public String getName() { return this.name; }
  public void setName(String name) { this.name = name; }

  public String getAddress() { return this.address; }
  public void setAddress(String address) { this.address = address; }

  public Map<String, Object> getAdditionalProperties() { return this.additionalProperties; }
  public void setAdditionalProperties(Map<String, Object> additionalProperties) { this.additionalProperties = additionalProperties; }
}