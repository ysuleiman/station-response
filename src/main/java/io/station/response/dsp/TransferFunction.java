package io.station.response.dsp;

import java.util.Objects;

import io.station.math.ComplexType;
import io.station.model.Coefficients;
import io.station.model.FIR;
import io.station.model.FIRType;
import io.station.model.PolesZeros;
import io.station.model.ResponseList;
import io.station.model.ResponseStage;

public interface TransferFunction {

	public ComplexType transform(double frequency);

	public ComplexType transform(double frequency, double factor);

	// public ComplexType phase(double frequency);

	/**
	 * Blockettes (54) and (44) have traditionally been used to represent FIR filter
	 * coefficients in the SEED format. These blockettes require that all
	 * coefficients are specified and that an error for each coefficient be given.
	 * In practice most FIR filters possess some symmetry properties and the error
	 * for the coefficients is not used. For this reason blockette 61 was introduced
	 * so that the FIR filter specification would require less space.
	 * 
	 * @param responseStage
	 * @return
	 */
	public static TransferFunction of(ResponseStage responseStage) {
		Objects.requireNonNull(responseStage, "responseStage cannot be null.");
		if (responseStage.isPolynomial()) {
			throw new IllegalArgumentException("Polynomial types are not supported!");
		}
		if (responseStage.isEmpty()) {
			throw new IllegalArgumentException("responseStage is empty!");
		}
		PolesZeros polesZeros = responseStage.getPolesZeros();
		if (polesZeros != null) {
			if (polesZeros.isLaplace()) {
				return new Laplace(polesZeros, responseStage.getStageGain());
			} else {
				return new InfiniteImpulseResponse(polesZeros, responseStage.getStageGain(),
						responseStage.getDecimation());
			}
		}

		FIRType firType = responseStage.getCoefficients() != null ? responseStage.getCoefficients()
				: responseStage.getFIR();

		if (firType != null) {
			if (firType instanceof Coefficients) {
				return new InfiniteImpulseResponse((Coefficients) firType, responseStage.getStageGain(),
						responseStage.getDecimation());
			} else {
				return new FiniteImpulseResponse((FIR) firType, responseStage.getDecimation(),
						responseStage.getStageGain());
			}
		}

		ResponseList responseList = responseStage.getResponseList();

		throw new IllegalArgumentException("unable to find a correct function to apply!");

	}
}
