package io.station.response;

import java.util.Objects;

import io.station.model.Response;
import io.station.validation.AbstractValidator;

public class EvalRespValidator extends AbstractValidator {

	@Override
	public void validate(Object target) {
		Objects.requireNonNull(target, "target cannot be null.");

		if (!(target instanceof Response)) {

		}

	}
}