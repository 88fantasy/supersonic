package dev.ai4j.deepseek4j.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Model {

	@JsonProperty("id")
	private String id;

	@JsonProperty("object")
	private String modelObject;

	@JsonProperty("owned_by")
	private String ownedBy;

}