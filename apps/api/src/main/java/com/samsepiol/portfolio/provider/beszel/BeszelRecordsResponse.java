package com.samsepiol.portfolio.provider.beszel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record BeszelRecordsResponse(@JsonProperty("items") List<BeszelSystemResponse> items) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record BeszelSystemResponse(String id, String name, String status) {
}
