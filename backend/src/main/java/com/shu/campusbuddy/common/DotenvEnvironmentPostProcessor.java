package com.shu.campusbuddy.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Loads a local .env without overriding variables already set in the process. */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    for (Path path : List.of(Path.of(".env"), Path.of("..", ".env"))) {
      if (!Files.isRegularFile(path)) {
        continue;
      }
      Map<String, Object> values = parse(path);
      if (!values.isEmpty()) {
        environment.getPropertySources().addLast(new MapPropertySource("dotenv", values));
      }
      return;
    }
  }

  private static Map<String, Object> parse(Path path) {
    Map<String, Object> values = new LinkedHashMap<>();
    try {
      for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        if (line.startsWith("export ")) {
          line = line.substring(7).trim();
        }
        int split = line.indexOf('=');
        if (split <= 0) {
          continue;
        }
        String key = line.substring(0, split).trim();
        String value = stripQuotes(line.substring(split + 1).trim());
        values.put(key, value);
      }
    } catch (IOException ex) {
      return Map.of();
    }
    return values;
  }

  private static String stripQuotes(String value) {
    if (value.length() >= 2) {
      char first = value.charAt(0);
      char last = value.charAt(value.length() - 1);
      if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
        return value.substring(1, value.length() - 1);
      }
    }
    return value;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
