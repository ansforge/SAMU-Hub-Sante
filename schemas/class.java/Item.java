package com.hubsante;

import java.util.Map;
public class Item {
  private String title;
  private String note;
  private Integer quantity;
  private Double price;
  private Map<String, Object> additionalProperties;

  public String getTitle() { return this.title; }
  public void setTitle(String title) { this.title = title; }

  public String getNote() { return this.note; }
  public void setNote(String note) { this.note = note; }

  public Integer getQuantity() { return this.quantity; }
  public void setQuantity(Integer quantity) { this.quantity = quantity; }

  public Double getPrice() { return this.price; }
  public void setPrice(Double price) { this.price = price; }

  public Map<String, Object> getAdditionalProperties() { return this.additionalProperties; }
  public void setAdditionalProperties(Map<String, Object> additionalProperties) { this.additionalProperties = additionalProperties; }
}