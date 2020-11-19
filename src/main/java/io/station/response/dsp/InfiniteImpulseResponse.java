package io.station.response.dsp;

import java.util.List;
import java.util.Objects;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import org.apache.commons.math3.util.FastMath;

import io.station.math.ComplexType;
import io.station.model.Coefficients;
import io.station.model.Decimation;
import io.station.model.Denominator;
import io.station.model.Numerator;
import io.station.model.PoleZero;
import io.station.model.PolesZeros;
import io.station.model.PzTransferFunctionType;
import io.station.model.StageGain;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InfiniteImpulseResponse implements TransferFunction {

	private PolesZeros polesZeros;
	private Coefficients coefficients;
	private StageGain stageGain;
	private Decimation decimation;

	public InfiniteImpulseResponse(Coefficients coefficients, StageGain stageGain, Decimation decimation) {
		Objects.requireNonNull(coefficients, "coefficients cannot be null.");
		this.coefficients = coefficients;
		this.stageGain = stageGain;
		this.decimation = decimation;
	}

	public InfiniteImpulseResponse(PolesZeros polesZeros, StageGain stageGain, Decimation decimation) {
		Objects.requireNonNull(polesZeros, "polesZeros cannot be null.");
		if (PzTransferFunctionType.DIGITAL_Z_TRANSFORM != polesZeros.getPzTransferFunctionType()) {
			throw new IllegalArgumentException("Expected " + PzTransferFunctionType.DIGITAL_Z_TRANSFORM.name()
					+ ", but received " + polesZeros.getPzTransferFunctionType());
		}
		this.polesZeros = polesZeros;
		this.stageGain = stageGain;
		this.decimation = decimation;
	}

	@Override
	public ComplexType transform(double frequency) {
		return transform(frequency, (stageGain == null || stageGain.getValue() == null) ? 1.0 : stageGain.getValue());
	}

	@Override
	public ComplexType transform(double frequency, double factor) {
		if (polesZeros == null && coefficients == null) {

		}
		if (stageGain.getValue() == null) {

		}

		if (decimation == null) {

		}

		if (polesZeros != null) {
			return polesZeros(polesZeros, decimation, stageGain, frequency, factor);
		} else {
			return coefficients(coefficients, decimation, stageGain, frequency, factor);
		}
	}

	private ComplexType coefficients(Coefficients coefficients, Decimation decimation, StageGain stageGain,
			double frequency, double factor) {
		if (log.isDebugEnabled()) {
			log.debug("Running iir with [factor:stageGain:{}], and [frequency:{}]", factor, frequency);
		}
		// field 4 b058
		if (stageGain.getValue() == null) {

		}

		if (decimation == null) {

		}
		Quantity<Time> i = decimation.getInputSampleRate().calculateSamplingInterval();
		System.out.println("i the interval: " + i.getValue());
		// s = i 2 π f
		double w = i.getValue().doubleValue() * 2 * Math.PI * frequency;

		List<Numerator> numerators = coefficients.getNumerators();

		ComplexType numerator = new ComplexType(numerators.get(0).getValue(), 0.0);
		System.out.println("numerator[0]" + numerator);
		double zeros = 1.0;
		for (int index = 1; index < numerators.size(); index++) {
			ComplexType ct = new ComplexType(w, w).cis(index);// z.reciprocal()
			numerator = numerator.add(new ComplexType(numerators.get(index).getValue(), 0.0).multiply(ct));
			zeros *= ct.getImaginary() / ct.getReal();
		}

		double poles = 1.0;
		List<Denominator> denominators = coefficients.getDenominators();
		ComplexType denominator = new ComplexType(1.0, 0.0);
		if (denominators != null && !denominators.isEmpty()) {

			denominator = new ComplexType(numerators.get(0).getValue(), 0.0);
			for (int index = 1; index < denominators.size(); index++) {
				ComplexType ct = new ComplexType(w, w).cis(index);// z.reciprocal()
				denominator = denominator.add(new ComplexType(denominators.get(index).getValue(), 0.0).multiply(ct));
				poles *= ct.getImaginary() / ct.getReal();
			}
		}
		double magnitude = numerator.divide(denominator).abs();

		magnitude *= factor;
		double phase = FastMath.atan(zeros) - FastMath.atan(poles);
		return new ComplexType(magnitude, Math.sin(phase));
	}

	private ComplexType polesZeros(PolesZeros polesZeros, Decimation decimation, StageGain stageGain, double frequency,
			double factor) {

		if (log.isDebugEnabled()) {
			log.debug("Running iir with [normalizationFactor:{}], and [frequency:{}]", factor, frequency);
		}
		// field 4 b058
		if (stageGain.getValue() == null) {

		}
		double sd = stageGain.getValue();

		if (decimation == null) {

		}
		Quantity<Time> i = decimation.getInputSampleRate().calculateSamplingInterval();
		// s = i 2 π f
		double w = i.getValue().doubleValue() * 2 * Math.PI * polesZeros.getNormalizationFrequency().getValue();// frequency;

		ComplexType z = new ComplexType(Math.cos(w), Math.sin(w));

		ComplexType numerator = new ComplexType(0.0, 0.0);
		double zeros = 1.0;
		for (PoleZero zero : polesZeros.getZero()) {
			ComplexType ct = z.subtract(zero);
			numerator = numerator.add(ct);
			zeros *= ct.getImaginary() / ct.getReal();
		}

		ComplexType denominator = new ComplexType(0.0, 0.0);
		double poles = 1.0;
		for (PoleZero pole : polesZeros.getPole()) {
			ComplexType ct = z.subtract(pole);
			denominator = denominator.add(ct);
			poles *= ct.getImaginary() / ct.getReal();
		}

		double amplitude = numerator.divide(denominator.conjugate()).abs();
		amplitude *= factor * sd;
		double phase = FastMath.atan(zeros) - FastMath.atan(poles);
		return new ComplexType(amplitude, Math.sin(phase));
		// return new ComplexType(Math.cos(phase), Math.sin(phase)).multiply(amplitude);

	}
}
