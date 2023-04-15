package com.hubsante.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Objects;
              
public class Recipients {
  @JsonProperty("recipient")
  @NotNull
  @Size(min=1)
  private AddresseeType[] recipient;

  public Recipients(){
  }

  public Recipients(
    AddresseeType[] recipient
  ) {
  	this.recipient = recipient;
  }

  public AddresseeType[] getRecipient() { return this.recipient; }
  public void setRecipient(AddresseeType[] recipient) { this.recipient = recipient; }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Recipients self = (Recipients) o;
      return 
        Objects.equals(this.recipient, self.recipient);
  }

  @Override
  public int hashCode() {
    return Objects.hash((Object)recipient);
  }

  @Override
  public String toString() {
    return "class Recipients {\n" +   
      "    recipient: " + toIndentedString(recipient) + "\n" +
    "}";
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}