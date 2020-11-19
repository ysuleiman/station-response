package io.station.response;

import java.util.List;
import java.util.Objects;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.complex.Complex;

import io.station.model.AngleType;
import io.station.model.CfTransferFunctionType;
import io.station.model.Coefficients;
import io.station.model.Decimation;
import io.station.model.FloatNoUnitType;
import io.station.model.FloatType;
import io.station.model.Gain;
import io.station.model.PoleZero;
import io.station.model.PolesZeros;
import io.station.model.Polynomial;
import io.station.model.PzTransferFunctionType;
import io.station.model.ResponseList;
import io.station.model.ResponseListElement;
import io.station.model.ResponseStage;
import io.station.model.StageGain;
import io.station.model.Symmetry;
import lombok.extern.slf4j.Slf4j;
import io.station.model.Coefficients.Denominator;
import io.station.model.Coefficients.Numerator;
import io.station.model.Polynomial.Coefficient;
import io.station.response.util.FrequencySet;
import tech.units.indriya.quantity.Quantities;

@Slf4j
public class EvalRespUtil {

	public static Complex calculateSpectrum(Coefficients coefficients, double normalizationFactor,
			Decimation decimation, double wVal, boolean useEstDelayFlag) throws Exception {
		Objects.requireNonNull(coefficients, "coefficients cannot be null!");
		Objects.requireNonNull(decimation, "decimation cannot be null!");

		CfTransferFunctionType transferFunctionType = coefficients.getCfTransferFunctionType();
		if (transferFunctionType != CfTransferFunctionType.DIGITAL) {
			throw new Exception("Invalid coefficients transfer type DIGITAL" + "filter in stage #");
		}

		Complex ofNum = null;

		Quantity<Time> samplingInterval = decimation.getInputSampleRate().calculateSamplingInterval();
		if (samplingInterval == null) {
			throw new InvalidResponseException("Invalid decimation object");
		}
		List<Denominator> denominators = coefficients.getDenominator();
		List<Numerator> numerators = coefficients.getNumerator();
		if (denominators == null || denominators.isEmpty()) { // no denominators, process as FIR filter
			if (numerators != null && !numerators.isEmpty()) { // more than zero numerators
																// determine if asymmetrical or symmetrical
																// FIR:
				Symmetry symmetry = determineFirTypeVal(coefficients);
				// final int firTypeVal = determineFirTypeVal(coefficients);
				ofNum = firTrans(coefficients, normalizationFactor, samplingInterval.getValue().doubleValue(), wVal,
						symmetry);
				if (symmetry == Symmetry.NONE) { // asymmetric FIR; requires delay correction

					FloatType floatType = null;
					if (useEstDelayFlag) {
						floatType = decimation.getDelay();
					} else {
						floatType = decimation.getCorrection();
					}

					Quantity<?> quantity = Quantities.getQuantity(floatType.getValue(), floatType.getUnit())
							.toSystemUnit();

					Number number = quantity.getValue();
					double deltaVal = number.doubleValue() - ((((double) (coefficients.getNumerator().size() - 1)) / 2)
							* samplingInterval.getValue().doubleValue());
					ofNum = ofNum.multiply(new Complex(Math.cos(wVal * deltaVal), Math.sin(wVal * deltaVal)));
				}
			}
		} else if (numerators != null && !numerators.isEmpty()) { // process as coefficients filter
			ofNum = iirTrans(coefficients, normalizationFactor, samplingInterval.getValue().doubleValue(), wVal);
		}
		return ofNum;
	}

	static Complex[] normalize(Coefficients coefficients, StageGain stageGain, Decimation decimation, double wVal)
			throws InvalidResponseException {
		Objects.requireNonNull(coefficients, "coefficients cannot be null!");
		Objects.requireNonNull(decimation, "decimation cannot be null!");

		Complex dfNum = null;
		Complex ofNum = null;
		CfTransferFunctionType cfTransferFunctionType = coefficients.getCfTransferFunctionType();
		if (cfTransferFunctionType != CfTransferFunctionType.DIGITAL) {
			throw new InvalidResponseException("Invalid transfer type for coefficients");
		}

		Quantity<Time> samplingInterval = decimation.getInputSampleRate().calculateSamplingInterval();
		if (samplingInterval == null) {
			throw new InvalidResponseException("Invalid decimation object");
		}
		List<Denominator> denominators = coefficients.getDenominator();
		List<Numerator> numerators = coefficients.getNumerator();

		// No denominators, process as FIR filter
		if (denominators == null || denominators.isEmpty()) {
			if (numerators != null && !numerators.isEmpty()) {
				// more than zero numerators
				// determine if asymmetrical or
				// symmetrical FIR:
				final Symmetry firTypeVal = determineFirTypeVal(coefficients);
				// check/fix FIR coefficients normalization:
				if (firTypeVal == Symmetry.NONE) { // (only if ASYM type)
					checkFixFirFreq0Norm(coefficients);
				}
				dfNum = firTrans(coefficients, 1.0, samplingInterval.getValue().doubleValue(),
						2 * Math.PI * stageGain.getFrequency(), firTypeVal);
				ofNum = firTrans(coefficients, 1.0, samplingInterval.getValue().doubleValue(), wVal, firTypeVal);
			} else { // empty FIR filter; ignore it
				dfNum = ofNum = null; // setup to not change gain, etc}
			}
		} else {
			dfNum = iirTrans(coefficients, 1.0, samplingInterval.getValue().doubleValue(),
					2 * Math.PI * stageGain.getFrequency());
			ofNum = iirTrans(coefficients, 1.0, samplingInterval.getValue().doubleValue(), wVal);
		}

		return new Complex[] { ofNum, dfNum };
	}

	public static Complex polynomial(Polynomial polynomial, double b62XValue) {
		Objects.requireNonNull(polynomial, "polynomial cannot be null!");
		Complex ofNum = null;
		if (polynomial != null) {
			double ampVal = 0.0; // initialize amplitude value

			List<Coefficient> polynomialCoefficients = polynomial.getCoefficient();

			// compute first derivate of MacLaurin polynomial:
			for (int i = 0; i < polynomialCoefficients.size(); i++) {
				Coefficient polynomialCoefficient = polynomialCoefficients.get(i);
				ampVal += polynomialCoefficient.getValue().doubleValue() * i * Math.pow(b62XValue, i - 1);
			}
			// set phase value based on amplitude
			double phaseVal = (ampVal >= 0.0) ? 0.0 : Math.PI;
			// apply values:
			ofNum = new Complex(ampVal * Math.cos(phaseVal), ampVal * Math.sin(phaseVal));
		}
		return ofNum;
	}

	public static Complex responseList(ResponseList responseList, FloatNoUnitType amplitude, AngleType phase) {
		Objects.requireNonNull(amplitude, "amplitude cannot be null.");
		Objects.requireNonNull(phase, "phase cannot be null.");
		return responseList(responseList, amplitude.getValue(), phase.getValue());
	}

	public static Complex responseList(ResponseList responseList, double amplitude, double phase) {
		// Complex responseList(ResponseStage stage, double[] freqArray, boolean
		// listInterpInFlag) {
		Objects.requireNonNull(responseList, "responseList cannot be null!");

		Complex ofNum = null;
		if (responseList != null) {
			ofNum = new Complex(amplitude * Math.cos(phase), amplitude * Math.sin(phase));
		}
		return ofNum;
	}

	public static double[] splineInterpolate(double[] x, List<ResponseListElement> elements) {
		Objects.requireNonNull(x, "a array cannot be null!");
		Objects.requireNonNull(x, "elements cannot be null!");
		if (elements.isEmpty()) {
			return null;
		}
		double[] y = new double[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			ResponseListElement element = elements.get(i);
			FloatNoUnitType floatNoUnitType = element.getAmplitude();
			if (floatNoUnitType == null || floatNoUnitType.getValue() == null) {

			}
			y[i] = floatNoUnitType.getValue();
		}
		return splineInterpolate(x, y);
	}

	public static double[] splineInterpolate(double[] x, double[] y) {
		PolynomialSplineFunction polynomialSplineFunction = new SplineInterpolator().interpolate(x, y);
		return null;
	}

	/**
	 * Calculates the response of an analog poles/zeros filter.
	 * 
	 * @param filterObj a poles/zeros filter object.
	 * @param normFact  the normalization factor to use.
	 * @param freq      the frequency value to use. If a Laplace filter then the
	 *                  frequency should be multplied by 2*pi.
	 * @return A 'ComplexBlk' object containing the response.
	 */
	public static Complex analogTrans(PolesZeros filterObj, double normFact, double frequency) {
		System.out.println("normFact:" + normFact + " freq:" + frequency);
		//Complex omega = new Complex(0.0, frequency);
		
		Complex omega = new Complex(0.0, frequency);
		Complex num = new Complex(1.0, 1.0);
		Complex denom = new Complex(1.0, 1.0);

		final int numZeros = (filterObj != null && filterObj.getZero() != null) ? filterObj.getZero().size() : 0;

		final int numPoles = (filterObj != null && filterObj.getPole() != null) ? filterObj.getPole().size() : 0;
		int i;
		for (i = 0; i < numZeros; i++) { // for each zero, numerator=numerator*(omega-zero[i])
			PoleZero zero = filterObj.getZero().get(i);
			num = num.multiply(omega.subtract(EvalRespUtil.createComplex(zero)));
		}

		for (i = 0; i < numPoles; i++) { // for each pole, denominator=denominator*(omega-pole[i])
			PoleZero pole = filterObj.getPole().get(i);
			denom = denom.multiply(omega.subtract(createComplex(pole)));
		}

		// gain*num/denum
		Complex temp = new Complex(denom.getReal(), -denom.getImaginary()).multiply(num);

		final double modSquared = denom.getReal() * denom.getReal() + denom.getImaginary() * denom.getImaginary();
		return temp.divide(modSquared).multiply(normFact);
	}

	// this is the same as analogTrans
	static Complex[] polesZeros(PolesZeros polesZeros, Decimation decimation, Gain stageGain, double cSenseFreq)
			throws InvalidResponseException {
		Objects.requireNonNull(polesZeros, "polesZeros cannot be null!");

		PzTransferFunctionType functionType = polesZeros.getPzTransferFunctionType();

		Complex ofNum = null;
		Complex dfNum = null;

		double wVal = 2 * Math.PI * cSenseFreq;
		if (functionType == PzTransferFunctionType.LAPLACE_HERTZ
				|| functionType == PzTransferFunctionType.LAPLACE_RADIANS_SECOND) {
			dfNum = analogTrans(polesZeros, 1.0,
					((functionType == PzTransferFunctionType.LAPLACE_HERTZ) ? 2 * Math.PI * stageGain.getFrequency()
							: stageGain.getFrequency()));
			ofNum = analogTrans(polesZeros, 1.0,
					((functionType == PzTransferFunctionType.LAPLACE_HERTZ) ? 2 * Math.PI * cSenseFreq : cSenseFreq));
			if (isZero(dfNum) || isZero(ofNum)) {
				throw new InvalidResponseException("Zero frequency in bandpass analog");
			}
		} else if (functionType == PzTransferFunctionType.DIGITAL_Z_TRANSFORM) {

			if (decimation == null) {
				throw new InvalidResponseException("Required decimation not found");
			}
			Quantity<Time> samplingInterval = decimation.getInputSampleRate().calculateSamplingInterval();
			if (samplingInterval == null) {
				throw new InvalidResponseException("Invalid decimation object");
			}

			dfNum = iirPzTrans(polesZeros, 1.0, samplingInterval.getValue().doubleValue(),
					2 * Math.PI * stageGain.getFrequency());
			ofNum = iirPzTrans(polesZeros, 1.0, samplingInterval.getValue().doubleValue(), wVal);
		} else {
			throw new InvalidResponseException("Invalid transfer type for poles/zeros");
		}
		return new Complex[] { ofNum, dfNum };
	}

	/**
	 * Calculates the response of a "Digital (Z - transform)" IIR poles/zeros
	 * filter.
	 * 
	 * @param filterObj     a poles/zeros filter object.
	 * @param normFact      the normalization factor to use.
	 * @param sIntervalTime the sample interval time to use.
	 * @param wVal          the frequency value to use.
	 * @return A 'ComplexBlk' object containing the response.
	 */
	public static Complex iirPzTrans(PolesZeros filterObj, double normFact, double sIntervalTime, double wVal) {
//    if(XDEBUG_FLAG)
//    {    //send debug message to default log file
//      LogFile.getGlobalLogObj().debug("iirPzTrans() input:  norm=" +
//               normFact + ", sIntTime=" + sIntervalTime + ", wVal=" + wVal);
//    }
		// get number of zeros:
		final int numZeros = (filterObj != null && filterObj.getZero() != null) ? filterObj.getZero().size() : 0;
		// get number of poles:
		final int numPoles = (filterObj != null && filterObj.getPole() != null) ? filterObj.getPole().size() : 0;
		// calculate radial freq. time sample interval:
		final double wsint = wVal * sIntervalTime;
		final double cosWsint = Math.cos(wsint);
		final double sinWsint = Math.sin(wsint);
		int i;
		double rVal, iVal, mod = 1.0, pha = 0.0;
		for (i = 0; i < numZeros; i++) { // for each zero
			PoleZero zero = filterObj.getZero().get(i);

			rVal = cosWsint - zero.getReal(); // 10/22/2003: + to -
			iVal = sinWsint - zero.getImaginary(); // 10/22/2003: + to -
			mod *= Math.sqrt(rVal * rVal + iVal * iVal);
			if (rVal != 0.0 || iVal != 0.0)
				pha += Math.atan2(iVal, rVal);
		}
		for (i = 0; i < numPoles; i++) { // for each pole
			PoleZero pole = filterObj.getPole().get(i);
			rVal = cosWsint - pole.getReal(); // 10/22/2003: + to -
			iVal = sinWsint - pole.getImaginary(); // 10/22/2003: + to -
			mod /= Math.sqrt(rVal * rVal + iVal * iVal);
			if (rVal != 0.0 || iVal != 0.0)
				pha -= Math.atan2(iVal, rVal);
		}
		return new Complex(mod * Math.cos(pha) * normFact, mod * Math.sin(pha) * normFact);
	}

	/**
	 * Calculates the response of a digital IIR filter. It evaluates phase directly
	 * from imaginary and real parts of IIR filter coefficients.
	 * 
	 * @param filterObj     a coefficients filter object.
	 * @param normFact      the normalization factor to use.
	 * @param sIntervalTime the sample interval time to use.
	 * @param wVal          the frequency value to use.
	 * @return A 'ComplexBlk' object containing the response.
	 */
	public static Complex iirTrans(Coefficients filterObj, double normFact, double sIntervalTime, double wVal) {
		// get number of numerators:
		final int numNumers = (filterObj != null && filterObj.getNumerator() != null) ? filterObj.getNumerator().size()
				: 0;
		// get number of denominators:
		final int numDenoms = (filterObj != null && filterObj.getDenominator() != null)
				? filterObj.getDenominator().size()
				: 0;
		// calculate radial freq. time sample interval:
		final double wsint = wVal * sIntervalTime;

		double xre, xim, phase, amp;
		int i;
		// process numerator:
		if (numNumers > 0) {
			xre = filterObj.getNumerator().get(0).getValue();
			xim = 0.0;
			for (i = 1; i < numNumers; ++i) {
				xre += filterObj.getNumerator().get(i).getValue() * Math.cos(-(i * wsint));
				xim += filterObj.getNumerator().get(i).getValue() * Math.sin(-(i * wsint));
			}
			amp = Math.sqrt(xre * xre + xim * xim);
			phase = Math.atan2(xim, xre);
		} else
			amp = phase = 0.0;
		// process denominator:
		if (numDenoms > 0) {
			xre = filterObj.getDenominator().get(0).getValue();
			xim = 0.0;
			for (i = 1; i < numDenoms; ++i) {
				xre += filterObj.getDenominator().get(i).getValue() * Math.cos(-(i * wsint));
				xim += filterObj.getDenominator().get(i).getValue() * Math.sin(-(i * wsint));
			}
			amp /= Math.sqrt(xre * xre + xim * xim);
			phase -= Math.atan2(xim, xre);
		}
//    if(XDEBUG_FLAG)
//    {    //send debug message to default log file
//      LogFile.getGlobalLogObj().debug("iirTrans() output:  out.real=" +
//                              amp*Math.cos(phase)*normFact + ", out.imag=" +
//                                              amp*Math.sin(phase)*normFact);
//    }
		return new Complex(amp * Math.cos(phase) * normFact, amp * Math.sin(phase) * normFact);
	}

	/**
	 * Calculates the response of a digital FIR filter. Only the numerators of the
	 * given filter object are used.
	 * 
	 * @param filterObj     a coefficients filter object.
	 * @param normFact      the normalization factor to use.
	 * @param sIntervalTime the sample interval time to use.
	 * @param wVal          the frequency value to use.
	 * @param firTypeVal    one of the 'FIR_...' values.
	 * @return A 'ComplexBlk' object containing the response.
	 */
	public static Complex firTrans(Coefficients filterObj, double normFact, double sIntervalTime, double wVal,
			Symmetry firTypeVal) {

		final int numCoeffs = (filterObj != null && filterObj.getNumerator() != null) ? filterObj.getNumerator().size()
				: 0;
		// calculate radial freq. time sample interval:
		final double wsint = wVal * sIntervalTime;
		if (numCoeffs <= 0) { // if no coefficients then return dummy value
			return new Complex(0.0, 0.0);
		}
		if (firTypeVal == Symmetry.ODD) { // FIR type is symmetrical 1
			final int numNumerators = (numCoeffs + 1) / 2;
			int i, factVal;
			double rVal = 0.0;
			for (i = 0; i < (numNumerators - 1); ++i) {
				factVal = numNumerators - (i + 1);
				rVal += filterObj.getNumerator().get(i).getValue() * Math.cos(wsint * factVal);
			}
			return new Complex((filterObj.getNumerator().get(i).getValue() + (2.0 * rVal)) * normFact, 0.0);
		} else if (firTypeVal == Symmetry.EVEN) { // FIR type is symmetrical 2
			final int numNumerators = numCoeffs / 2;
			int i, factVal;
			double rVal = 0.0;
			for (i = 0; i < numNumerators; ++i) {
				factVal = numNumerators - (i + 1);
				rVal += filterObj.getNumerator().get(i).getValue() * Math.cos(wsint * ((double) factVal + 0.5));
			}
			return new Complex(2.0 * rVal * normFact, 0.0);
		} else { // FIR type is asymmetrical
					// check if all coefficients have the same value:
			double val = filterObj.getNumerator().get(0).getValue();
			int i = 0;
			do {
				if (++i >= numCoeffs) { // all coefficients checked
					return new Complex(((wsint == 0.0) ? 1.0
							: ((Math.sin(wsint / 2.0 * numCoeffs) / Math.sin(wsint / 2.0)) * val)), 0.0);
				}
			} while (filterObj.getNumerator().get(i).getValue() == val);
			// process coefficients (not all same value):
			double rVal = 0.0, iVal = 0.0;
			for (i = 0; i < numCoeffs; ++i) {
				val = wsint * i;
				rVal += filterObj.getNumerator().get(i).getValue() * Math.cos(val);
				iVal += filterObj.getNumerator().get(i).getValue() * -Math.sin(val);
			}
			final double mod = Math.sqrt(rVal * rVal + iVal * iVal);
//      double pha = Math.atan2(iVal,rVal);
			// revert to previous version after Gabi Laske report:
			double pha = Math.atan2(iVal, rVal) + (wVal * (double) ((numCoeffs - 1) / 2.0) * sIntervalTime);
			rVal = mod * Math.cos(pha);
			iVal = mod * Math.sin(pha);

			return new Complex(rVal * normFact, iVal * normFact);
		}
	}

	/**
	 * Generates an array of frequency values.
	 * 
	 * @param minFreq        the minimum frequency to generate output for.
	 * @param maxFreq        the maximum frequency to generate output for.
	 * @param numFreqs       the number of frequencies to generate output for.
	 * @param logSpacingFlag log spacing flag
	 * @return A new array of double values, or null if an error was detected.
	 */
	public static double[] generateFreqArray1(double minFreq, double maxFreq, int numFreqs, boolean logSpacingFlag) {
		if (numFreqs <= 0) // if no frequencies then
			return null; // return null for error
		// create array for frequency values:
		final double[] freqArr = new double[numFreqs];
		if (numFreqs > 1 && !isZero(maxFreq - minFreq)) { // more than one frequency
			if (minFreq > maxFreq) { // min greater than max; exchange values
				final double tempVal = maxFreq;
				maxFreq = minFreq;
				minFreq = tempVal;
			}
			if (logSpacingFlag) { // logarithmic spacing selected
				if (isNegOrZero(minFreq)) // if min frequency <= zero then
					return null; // return null for error
				// calculate multiplier value:
				final double multVal = Math.pow(maxFreq / minFreq, 1.0 / (numFreqs - 1));
				double fVal = minFreq; // start with minimum frequency
				int i = 0; // initialize index
				while (true) { // for each frequency
					freqArr[i] = fVal; // enter value into array
					if (++i >= numFreqs) // increment index
						break; // if no more then exit loop
					fVal *= multVal; // calculate next value
				}
			} else { // linear spacing selected; calculate multiplier value
				final double multVal = (maxFreq - minFreq) / (numFreqs - 1);
				for (int i = 0; i < numFreqs; ++i) // for each frequency
					freqArr[i] = minFreq + i * multVal; // enter value into array
			}
		} else // single frequency
			freqArr[0] = minFreq; // enter frequency value
		return freqArr;
	}

	/**
	 * Unwraps the given array of 'phase' values. A phase array is "wrapped" by
	 * adding +/-360 to portions of the dataset to make all the array values be
	 * between -180 and +180 (inclusive). This method "unwraps" the given array by
	 * detecting transitions where the dataset has been "wrapped" and adding +/-360
	 * to restore the "original" values.
	 * 
	 * @param srcPhaseArr phase array to unwrap.
	 * @return A new 'double' array containing the unwrapped values, or the given
	 *         array if it was not wrapped.
	 */
	protected static double[] unwrapPhaseArray1(double[] srcPhaseArr) {
		return unwrapPhaseArray(srcPhaseArr, false);
	}

	/**
	 * Unwraps the given array of 'phase' values. A phase array is "wrapped" by
	 * adding +/-360 to portions of the dataset to make all the array values be
	 * between -180 and +180 (inclusive). This method "unwraps" the given array by
	 * detecting transitions where the dataset has been "wrapped" and adding +/-360
	 * to restore the "original" values.
	 * 
	 * @param srcPhaseArr     phase array to unwrap.
	 * @param firstNonNegFlag true if first phase value should always be made
	 *                        non-negative; false if not.
	 * @return A new 'double' array containing the unwrapped values, or the given
	 *         array if it was not wrapped.
	 */
	protected static double[] unwrapPhaseArray1(double[] srcPhaseArr, boolean firstNonNegFlag) {
		final int srcPhaseArrLen;
		if ((srcPhaseArrLen = srcPhaseArr.length) <= 0)
			return srcPhaseArr; // if source array empty then just return
		final double[] retArr = new double[srcPhaseArrLen];
		double offsetVal = 0.0; // offset value for unwrapping
		boolean wrapFlag = false; // flag set true if any unwrapping
		double prevPhaseVal = srcPhaseArr[0]; // initialize "previous" value
		if (firstNonNegFlag && prevPhaseVal < 0.0) { // flag set and first value is negative
			prevPhaseVal += 360.0; // add offset to value
			offsetVal = 360.0; // set new offset
			wrapFlag = true; // indicate unwrapping
		}
		retArr[0] = prevPhaseVal; // set first value in return array
		double newPhaseVal, diff;
		for (int i = 1; i < srcPhaseArrLen; ++i) { // for each remaining value in source array
			newPhaseVal = srcPhaseArr[i] + offsetVal;
			if ((diff = newPhaseVal - prevPhaseVal) > 180.0) { // phase "wrap" transition detected
				offsetVal -= 360.0; // adjust offset
				newPhaseVal -= 360.0; // adjust phase value
				wrapFlag = true; // indicate unwrapping
			} else if (diff < -180.0) { // phase "wrap" transition detected
				offsetVal += 360.0; // adjust offset
				newPhaseVal += 360.0; // adjust phase value
				wrapFlag = true; // indicate unwrapping
			}
			// enter value into return array and set "previous" value:
			retArr[i] = prevPhaseVal = newPhaseVal;
		}
		// return generated array if unwrapped; source array if not:
		return wrapFlag ? retArr : srcPhaseArr;
	}

	static Complex zMultiply1(Complex c1, Complex c2) {
		Objects.requireNonNull(c1, "c1 cannot be null!");
		if (c2 == null) {
			return c1;
		}

		final double real = c1.getReal() * c2.getReal() - c1.getImaginary() * c2.getImaginary();
		final double imaginary = c1.getImaginary() * c2.getReal() + c1.getReal() * c2.getImaginary();
		return new Complex(real, imaginary);
	}

	/**
	 * @return true if both parts of the given complex value are near zero.
	 * @param val value to compare.
	 */
	public static boolean isZero(Complex val) {
		return (Math.abs(val.getReal()) < SMALL_DOUBLE_VAL) && (Math.abs(val.getImaginary()) < SMALL_DOUBLE_VAL);
	}

	private static boolean isZero(double value) {
		if (value != 0) {
			return false;
		}
		return true;
	}

	/**
	 * @return true if given value is negative or near zero.
	 * @param val value to compare.
	 */
	public static boolean isNegOrZero(double val) {
		return val < (float) SMALL_DOUBLE_VAL;
	}

	/**
	 * Returns true if the given value is "nearly" equal to '-1'.
	 * 
	 * @param val value to compare.
	 * @return true if the given value is "nearly" equal to '-1'.
	 */
	public static boolean isNegativeOne(double val) {
		return (val > -1) ? (val + 1 < SMALL_DOUBLE_VAL) : (-1 - val < SMALL_DOUBLE_VAL);
	}

	/**
	 * Checks if the FIR coefficients filter should be normalized to 1.0 at
	 * frequency zero and adjusts the filter values if so.
	 * 
	 * @param filterObj a coefficients filter object.
	 * @param stageNum  current stage number.
	 */
	public static void checkFixFirFreq0Norm(Coefficients filterObj) {
		// get number of coefficients (numerators):
		final int numCoeffs = (filterObj != null && filterObj.getNumerator() != null) ? filterObj.getNumerator().size()
				: 0;
		if (numCoeffs > 0) { // more than zero coefficients (numerators)
			double sumVal = 0.0; // calculate sum of coefficients:
			for (int i = 0; i < numCoeffs; ++i)
				sumVal += filterObj.getNumerator().get(i).getValue();
			if (sumVal < (1.0 - FIR_NORM_TOL) || sumVal > (1.0 + FIR_NORM_TOL)) { // sum of coefficients is not 1.0
																					// divide by sum to make sum of
																					// coefficients be 1.0:
				for (int i = 0; i < numCoeffs; ++i) {
					filterObj.getNumerator().get(i).divide(sumVal);
				}
				// set info message:
				// setInfoMessage(
				// "WARNING: FIR blockette normalized, sum[coef]=" + sumVal + " (stage #" +
				// (stageNum + 1) + ")");
			}
		}
	}

	/**
	 * Determines the symmetry type for the given FIR filter.
	 * 
	 * @param filterObj FIR-filter object.
	 * @return One of the 'FIR_...' values.
	 */
	protected static Symmetry determineFirTypeVal(Coefficients coefficients) {
		// "B" FIR_SYM_1 odd number of values
		// 0 1 2 3 4 numNumerators = 5
		// a b c b a
		// "C" FIR_SYM_2 even number of values
		// 0 1 2 3 4 5 numNumerators = 6
		// a b c c b a

		if (coefficients == null) {

		}
		List<Numerator> numerators = coefficients.getNumerator();
		if (numerators == null || numerators.size() < 2) {
			return Symmetry.NONE;
		}
		int i = 0, j = numerators.size() - 1;
		do { // for each potential pair of symmetrical entries; check values

			Numerator ni = numerators.get(i);
			Numerator nj = numerators.get(j);

			if (!ni.equals(nj)) {
				return Symmetry.NONE;// FIR_ASYM; // indicate asymmetrical
			}
			++i; // move to next pair
			--j;
		} while (i < j); // loop if more pairs left to check
		// indicate symmetrical, type based on odd vs. even
		return (i == j) ? Symmetry.ODD : Symmetry.EVEN;// FIR_SYM1 : FIR_SYM2;}
	}

	/**
	 * Returns a modified version of the given 'srcArr' array with any of its
	 * entries outside of the given 'chkArr' array clipped. If any entries are
	 * clipped and 'showNotesFlag'==true then a message is sent to 'stderr'.
	 * 
	 * @param srcArr        source array.
	 * @param chkArr        check array.
	 * @param showNotesFlag true to send "Note:" messages to 'stderr'.
	 * @return A modified version of the given 'srcArr' array, or the original array
	 *         if no entries were clipped.
	 */
	protected static double[] clipFreqArray(double[] source, boolean showNotesFlag) {
		Objects.requireNonNull(source, "double[] srcArr array cannot be null");
		if (source.length > 0) {
			double firstVal = source[0]; // get first and last "check" values
			double lastVal = source[source.length - 1];
			if (firstVal > lastVal) { // first "check" value larger than last; swap them
				final double tmpVal = firstVal;
				firstVal = lastVal;
				lastVal = tmpVal;
			}
			int sPos = 0;
			while (sPos < source.length && (source[sPos] < firstVal || source[sPos] > lastVal)) { // for each
																									// out-of-range
																									// entry at
																									// beginning of
																									// "source" array
				++sPos;
			}
			// if out-of-range entries found at beginning of "source" array
			// and last clipped value is within 0.0001% of first "check"
			// value then setup to replace it with first "check" value:
			final boolean fixFirstFlag;
			if (sPos > 0 && Math.abs(firstVal - source[sPos - 1]) < firstVal * 1e-6) {
				--sPos; // restore clipped value
				fixFirstFlag = true; // indicate value should be "fixed"
			} else
				fixFirstFlag = false;
			int ePos = source.length - 1;
			while (ePos > 0 && (source[ePos] > lastVal || source[ePos] < firstVal)) { // for each out-of-range entry at
																						// end of "source" array
				--ePos;
			}

			// if out-of-range entries found at end of "source" array
			// and last clipped value is within 0.0001% of last "check"
			// value then setup to replace it with last "check" value:
			final boolean fixLastFlag;
			if (ePos < source.length - 1 && Math.abs(source[ePos + 1] - lastVal) < lastVal * 1e-6) {
				++ePos; // restore clipped value
				fixLastFlag = true; // indicate value should be "fixed"
			} else
				fixLastFlag = false;
			if (sPos > ePos) // if all values clipped then
				return new double[0]; // return empty array
			final int retArrLen;
			if ((retArrLen = ePos - sPos + 1) < source.length || fixFirstFlag || fixLastFlag) { // at least one entry
																								// was
				// clipped or first or last
				// value "fixed"
				// create new, clipped array
				final double[] retArr = new double[retArrLen];
				for (int i = 0; i < retArrLen; ++i) // copy over entries
					retArr[i] = source[i + sPos];
				if (fixFirstFlag) // if indicator flag then
					retArr[0] = firstVal; // "fix" first entry
				if (fixLastFlag) // if indicator flag then
					retArr[retArrLen - 1] = lastVal; // "fix" last entry
				if (showNotesFlag) { // "Note:" messages to 'stderr' enabled
					if (sPos > 0) { // at least one entry clipped from beginning; show note
						System.err.println("Note:  " + sPos + " frequenc" + ((sPos != 1) ? "ies" : "y")
								+ " clipped from beginning of requested range");
					}
					if ((ePos = source.length - ePos - 1) > 0) { // at least one entry clipped from beginning; show note
						System.err.println("Note:  " + ePos + " frequenc" + ((ePos != 1) ? "ies" : "y")
								+ " clipped from end of requested range");
					}
				}
				return retArr;
			}
		}
		return null;
	}

	public static double[] splineInterpolateAmplitude(double[] frequencies, ResponseList responseList) {
		Objects.requireNonNull(responseList, "responseList cannot be null!");
		return splineInterpolate(frequencies, responseList.getResponseListElement());
	}

	public static double[] splineInterpolatePhase(double[] frequencies, ResponseList responseList) {
		Objects.requireNonNull(responseList, "responseList cannot be null!");
		return splineInterpolate(frequencies, responseList.getResponseListElement());
	}

	public static double[] splineInterpolatePhase(double[] x, List<ResponseListElement> elements) {
		Objects.requireNonNull(x, "a array cannot be null!");
		Objects.requireNonNull(x, "elements cannot be null!");
		if (elements.isEmpty()) {
			return null;
		}
		double[] y = new double[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			ResponseListElement element = elements.get(i);
			AngleType angleType = element.getPhase();
			if (angleType == null || angleType.getValue() == null) {

			}
			y[i] = angleType.getValue();
		}
		double[] wrappedX = unwrapPhaseArray(y);
		boolean wrap = false;
		if (y != wrappedX) {
			wrap = true;
		}
		double[] interpolated = splineInterpolate(wrappedX, y);
		if (wrap) {
			interpolated = wrapPhaseArray(interpolated);
		}
		return interpolated;
	}

	/**
	 * Unwraps the given array of 'phase' values. A phase array is "wrapped" by
	 * adding +/-360 to portions of the dataset to make all the array values be
	 * between -180 and +180 (inclusive). This method "unwraps" the given array by
	 * detecting transitions where the dataset has been "wrapped" and adding +/-360
	 * to restore the "original" values.
	 * 
	 * @param srcPhaseArr phase array to unwrap.
	 * @return A new 'double' array containing the unwrapped values, or the given
	 *         array if it was not wrapped.
	 */

	protected static double[] unwrapPhaseArray(List<ResponseListElement> elements) {
		Objects.requireNonNull(elements, "elements cannot be null.");
		double[] srcPhaseArr = new double[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			ResponseListElement responseListElement = elements.get(i);
			srcPhaseArr[i] = responseListElement.getPhase().getValue();
		}
		return unwrapPhaseArray(srcPhaseArr, false);
	}

	/**
	 * Unwraps the given array of 'phase' values. A phase array is "wrapped" by
	 * adding +/-360 to portions of the dataset to make all the array values be
	 * between -180 and +180 (inclusive). This method "unwraps" the given array by
	 * detecting transitions where the dataset has been "wrapped" and adding +/-360
	 * to restore the "original" values.
	 * 
	 * @param srcPhaseArr phase array to unwrap.
	 * @return A new 'double' array containing the unwrapped values, or the given
	 *         array if it was not wrapped.
	 */
	protected static double[] unwrapPhaseArray(double[] srcPhaseArr) {
		return unwrapPhaseArray(srcPhaseArr, false);
	}

	/**
	 * Unwraps the given array of 'phase' values. A phase array is "wrapped" by
	 * adding +/-360 to portions of the dataset to make all the array values be
	 * between -180 and +180 (inclusive). This method "unwraps" the given array by
	 * detecting transitions where the dataset has been "wrapped" and adding +/-360
	 * to restore the "original" values.
	 * 
	 * @param srcPhaseArr     phase array to unwrap.
	 * @param firstNonNegFlag true if first phase value should always be made
	 *                        non-negative; false if not.
	 * @return A new 'double' array containing the unwrapped values, or the given
	 *         array if it was not wrapped.
	 */
	protected static double[] unwrapPhaseArray(double[] srcPhaseArr, boolean firstNonNegFlag) {
		final int srcPhaseArrLen;
		if ((srcPhaseArrLen = srcPhaseArr.length) <= 0)
			return srcPhaseArr; // if source array empty then just return
		final double[] retArr = new double[srcPhaseArrLen];
		double offsetVal = 0.0; // offset value for unwrapping
		boolean wrapFlag = false; // flag set true if any unwrapping
		double prevPhaseVal = srcPhaseArr[0]; // initialize "previous" value
		if (firstNonNegFlag && prevPhaseVal < 0.0) { // flag set and first value is negative
			prevPhaseVal += 360.0; // add offset to value
			offsetVal = 360.0; // set new offset
			wrapFlag = true; // indicate unwrapping
		}
		retArr[0] = prevPhaseVal; // set first value in return array
		double newPhaseVal, diff;
		for (int i = 1; i < srcPhaseArrLen; ++i) { // for each remaining value in source array
			newPhaseVal = srcPhaseArr[i] + offsetVal;
			if ((diff = newPhaseVal - prevPhaseVal) > 180.0) { // phase "wrap" transition detected
				offsetVal -= 360.0; // adjust offset
				newPhaseVal -= 360.0; // adjust phase value
				wrapFlag = true; // indicate unwrapping
			} else if (diff < -180.0) { // phase "wrap" transition detected
				offsetVal += 360.0; // adjust offset
				newPhaseVal += 360.0; // adjust phase value
				wrapFlag = true; // indicate unwrapping
			}
			// enter value into return array and set "previous" value:
			retArr[i] = prevPhaseVal = newPhaseVal;
		}
		// return generated array if unwrapped; source array if not:
		return wrapFlag ? retArr : srcPhaseArr;
	}

	/**
	 * Wraps the given array of 'phase' values. A phase array is "wrapped" by adding
	 * +/-360 to portions of the dataset to make all the array values be between
	 * -180 and +180 (inclusive).
	 * 
	 * @param srcPhaseArr phase array to wrap.
	 * @return A new 'double' array containing the wrapped values, or the given
	 *         array if it did not need to be wrapped.
	 */
	protected static double[] wrapPhaseArray(double[] srcPhaseArr) {
		final int srcPhaseArrLen;
		if ((srcPhaseArrLen = srcPhaseArr.length) <= 0)
			return srcPhaseArr; // if source array empty then just return
		double offsetVal = 0.0; // offset value for wrapping
		boolean wrapFlag = false; // flag set true if any wrapping
		double newPhaseVal;
		// pre-check first phase value to make sure that if it's >360
		// or <360 then the initial offset is setup accordingly:
		if ((newPhaseVal = srcPhaseArr[0]) > 180.0) { // first phase value is too high
			do // set offset to put values in range
				offsetVal -= 360.0;
			while (newPhaseVal + offsetVal > 180.0);
		} else if (newPhaseVal < -180.0) { // first phase value is too low
			do // set offset to put values in range
				offsetVal += 360.0;
			while (newPhaseVal + offsetVal < -180.0);
		}
		final double[] retArr = new double[srcPhaseArrLen];
		for (int i = 0; i < srcPhaseArrLen; ++i) { // for each value in source array
			newPhaseVal = srcPhaseArr[i] + offsetVal;
			if (newPhaseVal > 180.0) { // phase value too high
				offsetVal -= 360.0; // adjust offset
				newPhaseVal -= 360.0; // adjust phase value
				wrapFlag = true; // indicate wrapping
			} else if (newPhaseVal < -180.0) { // phase value too low
				offsetVal += 360.0; // adjust offset
				newPhaseVal += 360.0; // adjust phase value
				wrapFlag = true; // indicate wrapping
			}
			retArr[i] = newPhaseVal;
		}
		// return generated array if wrapped; source array if not:
		return wrapFlag ? retArr : srcPhaseArr;
	}

	private static final double SMALL_DOUBLE_VAL = 1e-40;
	/** Tolerance value used by 'checkFixFirFreq0Norm()' method. */
	public static final double FIR_NORM_TOL = 0.02;

	public static double[][] ampPase(Complex[] spectrum, boolean unwrapPhaseFlag) {
		if (log.isDebugEnabled()) {
			log.debug("ampPase({})", unwrapPhaseFlag);
		}

		double[][] result = new double[2][spectrum.length];
		for (int i = 0; i < spectrum.length; i++) {
			Complex c = spectrum[i];
			result[0][i] = Math.sqrt(c.getReal() * c.getReal() + c.getImaginary() * c.getImaginary());
			result[1][i] = Math.atan2(c.getImaginary(), c.getReal() + 1.0e-200) * 180.0 / Math.PI;
		}
		if (unwrapPhaseFlag) { // flag set for unwrapping phase values (via "-unwrap" parameter)
			try { // get generated phase data:
				final double[] srcPhaseArr = result[1];// fetchAmpPhaPhaseArray(ampPhaseArr);
				final double[] unwrappedPhaseArr = // unwrap phase data
						unwrapPhaseArray(srcPhaseArr, true);
				if (unwrappedPhaseArr != srcPhaseArr) { // phase data changed; enter with new phase data
					result[1] = unwrappedPhaseArr;
				}
			} catch (Exception ex) { // some kind of error; set error message
				ex.printStackTrace();// throw new Exception("Exception error unwrapping phase output values",ex);
			}
		}
		return result;
	}
	/*
	 * public static double[] createFrequency() { return createFrequency(0.00001,
	 * 2E01, 200); }
	 * 
	 * public static double[] createFrequency(double minFreq, double maxFreq, int
	 * numFreqs) { log.debug("createFrequency({},{},{})", minFreq, maxFreq,
	 * numFreqs); if (minFreq == 0) { minFreq = 0.00001; } if (maxFreq == 0) {
	 * maxFreq = 2E01; } if (numFreqs == 0) { numFreqs = 200; } final double[]
	 * freqArr = new double[numFreqs];
	 * 
	 * double delta = (Math.log10(maxFreq) - Math.log10(minFreq)) / (numFreqs - 1);
	 * for (int i = 0; i < numFreqs; ++i) { freqArr[i] = Math.pow(10.0,
	 * Math.log10(minFreq) + i * delta); } return freqArr; }
	 */

	public static FrequencySet createFrequency() {
		return createFrequency(0.00001, 2E01, 200, FrequencySpacing.LOGARITHMIC);
	}

	public static FrequencySet createFrequency(double minumumFrequency, double maxFrequency, int numberOfFrequencies,
			FrequencySpacing frequencySpacing) {
		boolean logSpacing = (frequencySpacing == FrequencySpacing.LOGARITHMIC);
		double delta = 0, lo, hi;

		if (minumumFrequency > maxFrequency) {

		}
		double[] frequencyArray = new double[numberOfFrequencies];

		if (!logSpacing) {
			lo = minumumFrequency;
			hi = maxFrequency;
		} else {
			lo = Math.log10(minumumFrequency);
			hi = Math.log10(maxFrequency);
		}
		delta = numberOfFrequencies == 1 ? 0 : (hi - lo) / (numberOfFrequencies - 1);
		for (int i = 0; i < numberOfFrequencies; i++) {
			frequencyArray[i] = lo + i * delta;
			if (logSpacing) {
				frequencyArray[i] = Math.pow(10, frequencyArray[i]);
			}
		}
		return new FrequencySet(minumumFrequency, maxFrequency, frequencySpacing, frequencyArray);
	}

	public static double calculateAngularFrequency(double common) {
		return common * 2 * Math.PI;
	}

	/**
	 * Unwraps angles by converting it to a 360 degrees complements whenever the
	 * jumps between the values is larger than 180 degrees.
	 * 
	 * @param angles an array of angles to be unwrapped
	 * @return the unwrapped copy of the angles
	 */
	public static double[] unwrap(double[] angles) {

		double[] unwrapped = new double[angles.length];

		unwrapped[0] = angles[0];

		for (int i = 1; i < unwrapped.length; i++) {
			if (Math.abs(angles[i] - unwrapped[i - 1]) > 180.0)
				unwrapped[i] = complement(angles[i]);
			else
				unwrapped[i] = angles[i];
		}
		return unwrapped;

	}

	/**
	 * Returns the given angle changed to a 360 degrees complement.
	 * 
	 * @param angle an angle to complement
	 * @return the 360 degrees complement of the angle
	 */
	public static double complement(double angle) {

		if (angle < 0)
			angle += 360;
		else if (angle > 0)
			angle -= 360;
		return angle;
	}
	
	public static Complex createComplex(double real, double imaginary) {
		return new Complex(real, imaginary);
	}
	public static Complex createComplex(PoleZero poleZero) {
		Objects.requireNonNull(poleZero, "poleZero cannot be null.");
		Objects.requireNonNull(poleZero.getRealType(), "poleZero.getRealType() cannot be null.");
		
		return new Complex(poleZero.getReal(), poleZero.getImaginaryType()==null?0.0:poleZero.getImaginary());
	}
}
