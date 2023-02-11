package com.hubsante;
import com.hubsante.Shipto;
import java.util.Map;
public class ShipOrder {
  private String orderid;
  private String orderperson;
  private Shipto shipto;
  private Map<String, Object> additionalProperties;

  public String getOrderid() { return this.orderid; }
  public void setOrderid(String orderid) { this.orderid = orderid; }

  public String getOrderperson() { return this.orderperson; }
  public void setOrderperson(String orderperson) { this.orderperson = orderperson; }

  public Shipto getShipto() { return this.shipto; }
  public void setShipto(Shipto shipto) { this.shipto = shipto; }

  public Map<String, Object> getAdditionalProperties() { return this.additionalProperties; }
  public void setAdditionalProperties(Map<String, Object> additionalProperties) { this.additionalProperties = additionalProperties; }
}