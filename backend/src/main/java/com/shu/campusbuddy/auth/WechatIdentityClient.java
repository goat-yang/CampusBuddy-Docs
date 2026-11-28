package com.shu.campusbuddy.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shu.campusbuddy.common.ApiException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

public interface WechatIdentityClient {
  String openIdForCode(String code);
}

@Component
class HttpWechatIdentityClient implements WechatIdentityClient {

  private static final Logger log = LoggerFactory.getLogger(HttpWechatIdentityClient.class);

  private final CampusBuddyProperties properties;
  private final ObjectMapper mapper;
  private final RestClient client = RestClient.create();

  HttpWechatIdentityClient(CampusBuddyProperties properties, ObjectMapper mapper) {
    this.properties = properties;
    this.mapper = mapper;
  }

  @Override
  public String openIdForCode(String code) {
    String appId = properties.getWechat().getAppId();
    String secret = properties.getWechat().getSecret();
    if (appId == null || appId.isBlank() || secret == null || secret.isBlank()) {
      throw ApiException.bad("code", "微信登录未配置");
    }
    URI uri =
        UriComponentsBuilder.fromUriString("https://api.weixin.qq.com/sns/jscode2session")
            .queryParam("appid", appId)
            .queryParam("secret", secret)
            .queryParam("js_code", code)
            .queryParam("grant_type", "authorization_code")
            .build()
            .encode()
            .toUri();
    String body;
    try {
      body = client.get().uri(uri).retrieve().body(String.class);
    } catch (RuntimeException ex) {
      log.info("wechat jscode2session unreachable");
      throw ApiException.system("系统错误");
    }
    try {
      JsonNode node = mapper.readTree(body == null ? "{}" : body);
      if (node.hasNonNull("openid")) {
        return node.get("openid").asText();
      }
    } catch (Exception ex) {
      log.info("wechat jscode2session unreadable");
    }
    throw ApiException.bad("code", "微信登录码无效");
  }
}
