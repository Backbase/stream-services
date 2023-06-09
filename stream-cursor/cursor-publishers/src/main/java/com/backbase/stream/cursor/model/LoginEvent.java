package com.backbase.stream.cursor.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Login Event. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginEvent implements Serializable {

  private String userId;
  private Date dateTime;
  private String visitorId;
  private Map<String, String> requestParameters;
  private Map<String, String> requestHeaders;
  private Map<String, String> requestCookies;
}
