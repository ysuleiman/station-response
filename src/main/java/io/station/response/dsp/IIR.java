package io.station.response.dsp;

import java.util.List;
import java.util.Objects;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.FastMath;

import io.station.math.ComplexType;
import io.station.model.Coefficients;
import io.station.model.Decimation;
import io.station.model.Frequency;
import io.station.model.Gain;
import io.station.model.PoleZero;
import io.station.model.PolesZeros;
import io.station.model.PzTransferFunctionType;
import io.station.model.ResponseStage;
import io.station.model.StageGain;
import io.station.response.dsp.PolesZerosIIR.PolesZerosIIRBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * Seed Manual: Appendix C pages 158, 159, 160. Normalization: Analog stages are
 * represented by the Laplace transform.
 * 
 * 
 * @author Suleiman
 *
 */
@Slf4j
public class IIR implements TransferFunction {

	private PolesZeros polesZeros;
	private StageGain stageGain;
	private Decimation decimation;

	public IIR(PolesZeros polesZeros, StageGain stageGain, Decimation decimation) {
		Objects.requireNonNull(polesZeros, "polesZeros cannot be null.");
		if (PzTransferFunctionType.DIGITAL_Z_TRANSFORM != polesZeros.getPzTransferFunctionType()) {
			throw new IllegalArgumentException("Expected " + PzTransferFunctionType.DIGITAL_Z_TRANSFORM.name()
					+ ", but received " + polesZeros.getPzTransferFunctionType());
		}
		Objects.requireNonNull(stageGain, "stageGain cannot be null.");
		Objects.requireNonNull(decimation, "decimation cannot be null.");
		this.polesZeros = polesZeros;
		this.stageGain = stageGain;
		this.decimation = decimation;
	}

	@Override
	public ComplexType transform(double frequency) {
		return transform(frequency,
				polesZeros.getNormalizationFactor() == null ? 1.0 : polesZeros.getNormalizationFactor());
	}

	@Override
	public ComplexType transform(double frequency, double factor) {

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

	public ComplexType transform1(double frequency, double factor) {

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
		double w = i.getValue().doubleValue() * 2 * Math.PI * frequency;

		double amplitude = 1.0;
		double phase = 0.0;
		ComplexType s = new ComplexType(Math.cos(w), Math.sin(w));
		List<PoleZero> zeros = polesZeros.getZero();
		for (PoleZero zero : zeros) {
			ComplexType complex = s.subtract(zero);
			amplitude *= complex.abs();// complex.abs();
			if (!zero.isZero()) {
				phase += FastMath.atan2(complex.getImaginary(), complex.getReal());
			}
		}
		List<PoleZero> poles = polesZeros.getPole();
		for (PoleZero pole : poles) {
			ComplexType complex = s.subtract(pole);
			amplitude /= complex.abs();// complex.abs();
			if (!pole.isZero()) {
				phase -= FastMath.atan2(complex.getImaginary(), complex.getReal());
			}
		}
		return new ComplexType(Math.cos(phase), Math.sin(phase)).multiply(factor).multiply(sd).multiply(amplitude);

	}
}
