package io.station.response.dsp;

import java.util.Objects;

import org.apache.commons.math3.util.FastMath;

import io.station.math.ComplexType;
import io.station.model.PoleZero;
import io.station.model.PolesZeros;
import io.station.model.PzTransferFunctionType;
import io.station.model.StageGain;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Laplace implements TransferFunction {

	PolesZeros polesZeros;
	StageGain stageGain;

	public Laplace(PolesZeros polesZeros, StageGain stageGain) {
		this.polesZeros = polesZeros;
		this.stageGain = stageGain;
	}

	@Override
	public ComplexType transform(double frequency) {
		return transform(frequency,
				polesZeros.getNormalizationFactor() == null ? 1.0 : polesZeros.getNormalizationFactor());
	}

	@Override
	public ComplexType transform(double frequency, double factor) {
		Objects.requireNonNull(polesZeros, "polesZeros cannot be null.");
		if (!polesZeros.isLaplace()) {
			throw new IllegalArgumentException(
					"Expected Laplace, but received " + polesZeros.getPzTransferFunctionType());
		}

		if (PzTransferFunctionType.LAPLACE_RADIANS_SECOND == polesZeros.getPzTransferFunctionType()) {
			frequency *= 2 * Math.PI;
		}
		if (log.isDebugEnabled()) {
			log.debug("Running laplace with [normalizationFactor:{}], and [frequency:{}]",
					factor, frequency);
		}
		final ComplexType omega = new ComplexType(0.0, frequency);
		ComplexType numerator = new ComplexType(1.0, 1.0);
		ComplexType denominator = new ComplexType(1.0, 1.0);

		double zeros = 1.0;
		for (PoleZero zero : polesZeros.getZero()) {
			ComplexType ct = omega.subtract(zero);
			numerator = numerator.multiply(ct);
			zeros *= ct.getImaginary() / ct.getReal();
		}

		double poles = 1.0;
		for (PoleZero pole : polesZeros.getPole()) {
			ComplexType ct = omega.subtract(pole);
			denominator = denominator.multiply(ct);
			poles *= ct.getImaginary() / ct.getReal();
		}
		return new ComplexType(factor * numerator.divide(denominator.conjugate()).abs(),
				FastMath.atan(zeros) - FastMath.atan(poles));
	}
	
}