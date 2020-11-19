package io.station.response.dsp;

import java.util.List;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import org.apache.commons.math3.util.FastMath;

import io.station.math.ComplexType;
import io.station.model.Coefficients;
import io.station.model.Decimation;
import io.station.model.Denominator;
import io.station.model.FIR;
import io.station.model.FIRType;
import io.station.model.Numerator;
import io.station.model.PoleZero;
import io.station.model.StageGain;
import io.station.model.Symmetry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FIRCoefficient implements TransferFunction {
	private FIR fir;
	private StageGain stageGain;
	private Decimation decimation;

	public FIRCoefficient(FIR fir, StageGain stageGain, Decimation decimation) {
		this.fir = fir;
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
			log.debug("Running fir with [factor:stageGain:{}], and [frequency:{}]", factor, frequency);
		}
		List<Numerator> numerators = fir.getNumerators();
		if (numerators == null || numerators.isEmpty()) {
			return new ComplexType(0.0, 0.0);
		}

		if (decimation == null) {

		}
		Quantity<Time> samplingInterval = decimation.getInputSampleRate().calculateSamplingInterval();

		double interval = samplingInterval.getValue().doubleValue();
		double w = interval * 2 * Math.PI * frequency;

		ComplexType ct = null;
		if (Symmetry.EVEN == fir.getSymmetry()) {
			ct = even(frequency, factor);
		} else if (Symmetry.ODD == fir.getSymmetry()) {
			ct = odd(frequency, factor);
		} else {
			int M = (numerators.size() - 1) / 2;
			int end = 0;
			if (numerators.size() % 2 == 0) {
				end = (numerators.size() / 2) - 1;
			} else {
				end = M - 1;
			}
			ct = none(numerators, w, M, 0, end);
		}
		return ct.multiply(factor);
	}

	private ComplexType none(List<Numerator> numerators, double w, int M, int start, int end) {
		ComplexType ct = new ComplexType(0.0);
		for (int n = start; n < end; n++) {
			ct = ct.add(numerators.get(n).doubleValue() * FastMath.sin((M - n) * w));
		}
		return ct.multiply(2);
	}

	// http://pioneer.netserv.chula.ac.th/~nsuvit/423/Ch7(2)Handouts_3e.pdf
	private ComplexType odd1(double frequency, double factor) {
		Quantity<Time> samplingInterval = decimation.getInputSampleRate().calculateSamplingInterval();

		double interval = samplingInterval.getValue().doubleValue();
		double w = interval * 2 * Math.PI * frequency;

		List<Numerator> numerators = fir.getNumerators();
		ComplexType ct = new ComplexType(0.0);
		int n = numerators.size() - 1;
		for (int index = 0, high = n; index < n; index++, high--) {
			ct = ct.add(2 * numerators.get(index).doubleValue() * FastMath.cos(high * w));
		}
		ct = ct.add(numerators.get(n).doubleValue());

		double angle = -(n / 2) * w;
		ComplexType z = new ComplexType(Math.cos(angle), Math.sin(angle));
		return ct.multiply(z);
	}

	// http://eeweb.poly.edu/iselesni/EL713/zoom/linphase.pdf
	private ComplexType odd(double frequency, double factor) {
		Quantity<Time> samplingInterval = decimation.getInputSampleRate().calculateSamplingInterval();

		double interval = samplingInterval.getValue().doubleValue();
		double w = interval * 2 * Math.PI * frequency;

		List<Numerator> numerators = fir.getNumerators();
		ComplexType ct = new ComplexType(0.0);

		int N = (2 * numerators.size()) - 1;
		int M = (N - 1) / 2;
		for (int index = 0; index < M; index++) {
			ct = ct.add(numerators.get(index).doubleValue() * FastMath.cos((M - index) * w));
		}

		double angle = -M * w;
		ComplexType z = new ComplexType(Math.cos(angle), Math.sin(angle));
		return ct.multiply(2).add(numerators.get(M).doubleValue());// .multiply(z);
	}

	// http://eeweb.poly.edu/iselesni/EL713/zoom/linphase.pdf
	private ComplexType even(double frequency, double factor) {
		Quantity<Time> samplingInterval = decimation.getInputSampleRate().calculateSamplingInterval();

		double interval = samplingInterval.getValue().doubleValue();
		double w = interval * 2 * Math.PI * frequency;

		List<Numerator> numerators = fir.getNumerators();
		ComplexType ct = new ComplexType(0.0);

		int N = 2 * numerators.size();
		// int M = (N - 1) / 2;
		int M = N / 2;
		double angle = -M * w;
		// ComplexType z=new ComplexType(angle,angle);
		for (int index = 0; index < M; index++) {
			ct = ct.add(numerators.get(index).doubleValue() * FastMath.cos((M - index) * w));
			// ct = ct.add(z.multiply(numerators.get(index).doubleValue() * FastMath.cos((M
			// - index) * w)));
			System.out.println("numerators.get(" + index + ").doubleValue() * FastMath.cos(" + (M - index) + " * w)");
		}
		return ct.multiply(2);// .multiply(new ComplexType(FastMath.cos(angle),FastMath.sin(angle)));
	}

	public ComplexType transform1(double frequency, double factor) {
		if (log.isDebugEnabled()) {
			log.debug("Running fir with [factor:stageGain:{}], and [frequency:{}]", factor, frequency);
		}
		List<Numerator> numerators = fir.getNumerators();
		if (numerators == null || numerators.isEmpty()) {
			return new ComplexType(0.0, 0.0);
		}

		if (decimation == null) {

		}
		Quantity<Time> samplingInterval = decimation.getInputSampleRate().calculateSamplingInterval();

		double interval = samplingInterval.getValue().doubleValue();
		double w = interval * 2 * Math.PI * frequency;

		ComplexType numerator = new ComplexType(0.0);
		if (Symmetry.EVEN == fir.getSymmetry()) {
			int numNumerators = numerators.size() / 2;
			int i = 0;
			for (; i < numNumerators; ++i) {
				Numerator n = numerators.get(i);
				ComplexType ct = new ComplexType(w, w).cis((numNumerators - (i + 1)) + 0.5);
				numerator = numerator.add(new ComplexType(n.doubleValue(), 0.0).multiply(ct));
			}

		} else if (Symmetry.ODD == fir.getSymmetry()) {
			int numNumerators = (numerators.size() + 1) / 2;
			for (int i = 0; i < (numNumerators - 1); ++i) {
				Numerator n = numerators.get(i);
				ComplexType ct = new ComplexType(w, w).cis((numNumerators - (i + 1)));
				numerator = numerator.add(new ComplexType(n.doubleValue(), 0.0).multiply(ct));
			}
		} else { // FIR type is asymmetrical
			// check if all coefficients have the same value:
			Numerator n = numerators.get(0);
			double val = n.doubleValue();
			int i = 0;
			do {
				if (++i >= numerators.size()) { // all coefficients checked
					return new ComplexType(
							((w == 0.0) ? 1.0 : ((Math.sin(w / 2.0 * numerators.size()) / Math.sin(w / 2.0)) * val)),
							0.0);
				}
			} while (n.doubleValue() == val);
			// process coefficients (not all same value):
			double rVal = 0.0, iVal = 0.0;
			i = 0;
			for (i = 0; i < numerators.size(); i++) {
				Numerator num = numerators.get(i);
				val = w * i;
				rVal += num.doubleValue() * Math.cos(val);
				iVal += num.doubleValue() * -Math.sin(val);
			}

			final double mod = Math.sqrt(rVal * rVal + iVal * iVal);
//		      double pha = Math.atan2(iVal,rVal);
			// revert to previous version after Gabi Laske report:
			double pha = Math.atan2(iVal, rVal) + (w * (double) ((numerators.size() - 1) / 2.0) * interval);
			rVal = mod * Math.cos(pha);
			iVal = mod * Math.sin(pha);

			numerator = new ComplexType(rVal, iVal);
		}

		return numerator.multiply(factor);
	}
}
