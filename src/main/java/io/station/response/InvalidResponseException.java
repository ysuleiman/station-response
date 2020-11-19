package io.station.response;

import java.util.List;

import io.station.validation.StationException;

public class InvalidResponseException extends StationException {

	private List<io.station.validation.Error> errors;

	public InvalidResponseException(String message) {
		super(message);
	}

	public InvalidResponseException(List<io.station.validation.Error> errors) {
		this("Invalid Response");
		this.errors = errors;
	}

	public List<io.station.validation.Error> getErrors() {
		return this.errors;
	}
}
