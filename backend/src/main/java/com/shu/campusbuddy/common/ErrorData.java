package com.shu.campusbuddy.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorData(String field, Boolean retryable, Integer retryAfterSec) {}
