package io.station.response;

import io.station.validation.StationError;

public class EvalRespError extends StationError {



	public EvalRespError(String message) {
		super(message);

	}


	public static EvalRespError create(String message) {
		return new EvalRespError(message);
	}
}
