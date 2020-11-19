package io.station.response.dsp;

import java.util.List;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import org.apache.commons.math3.util.FastMath;

import io.station.math.ComplexType;
import io.station.model.Coefficients;
import io.station.model.Decimation;
import io.station.model.Denominator;
import io.station.model.FIRType;
import io.station.model.Numerator;
import io.station.model.PoleZero;
import io.station.model.StageGain;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IIRCoefficient implements TransferFunction {
	private FIRType firType;
	private StageGain stageGain;
	private Decimation decimation;

	public IIRCoefficient(FIRType firType, StageGain stageGain, Decimation decimation) {
		this.firType = firType;
		this.stageGain = stageGain;
		this.decimation = decimation;
	}

	@Override
	public ComplexType transform(double frequency) {
		return transform(frequency, (stageGain == null || stageGain.getValue() == null) ? 1.0 : stageGain.getValue());
	}

	@Override
	public ComplexType transform(double frequency, double factor) {
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

		List<Numerator> numerators = firType.getNumerators();

		ComplexType numerator = new ComplexType(numerators.get(0).getValue(), 0.0);
		System.out.println("numerator[0]" + numerator);
		double zeros = 1.0;
		for (int index = 1; index < numerators.size(); index++) {
			ComplexType ct = new ComplexType(w, w).cis(index);// z.reciprocal()
			numerator = numerator.add(new ComplexType(numerators.get(index).getValue(), 0.0).multiply(ct));
			zeros *= ct.getImaginary() / ct.getReal();
		}

		double poles = 1.0;
		List<Denominator> denominators = firType.getDenominators();
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

}
