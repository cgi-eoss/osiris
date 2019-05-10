package com.cgi.eoss.osiris.search.osiris;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Data;

@Data
@JsonSerialize(using = RawJsonStringSerializer.class)
public class RawJsonString {

	private final String rawJson;

	

}
