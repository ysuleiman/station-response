package io.station.response;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.measure.Unit;

import org.apache.commons.math3.complex.Complex;

import io.station.math.CubicSpline;
import io.station.model.Gain;
import io.station.model.SampleRate;
import io.station.model.Sensitivity;
import io.station.model.StageGain;
import io.station.response.util.FrequencySet;
import io.station.uom.StationUnits;

public class ResponseSpectrum {

	private String network;
	private String station;
	private String location;
	private String channel;
	private ZonedDateTime start;
	private ZonedDateTime end;
	private SampleRate sampleRate;
	// private Sensitivity instrumentSensitivity;
	private Map<Integer, StageSpectrum> map = new TreeMap<>();
	private FrequencySet frequencySet;

	public ResponseSpectrum(NormalizedResponse normalizedResponse, FrequencySet frequencySet) {
		// this.instrumentSensitivity = instrumentSensitivity;
		this.network = normalizedResponse.getNetwork();

		this.station = normalizedResponse.getStation();
		this.location = normalizedResponse.getLocation();
		this.channel = normalizedResponse.getChannel();
		this.start = normalizedResponse.getStart();
		this.end = normalizedResponse.getEnd();
		this.sampleRate = normalizedResponse.getSampleRate();

		this.frequencySet = frequencySet;
	}

	public ResponseSpectrum(String network, String station, String location, String channel, ZonedDateTime start,
			ZonedDateTime end, SampleRate sampleRate, FrequencySet frequencySet) {
		// this.instrumentSensitivity = instrumentSensitivity;
		this.network = network;

		this.station = station;
		this.location = location;
		this.channel = channel;
		this.start = start;
		this.end = end;
		this.sampleRate = sampleRate;

		this.frequencySet = frequencySet;
	}

	public String getNetwork() {
		return network;
	}

	public String getStation() {
		return station;
	}

	public String getLocation() {
		return location;
	}

	public String getChannel() {
		return channel;
	}

	public ZonedDateTime getStart() {
		return start;
	}

	public ZonedDateTime getEnd() {
		return end;
	}

	public SampleRate getSampleRate() {
		return sampleRate;
	}

	public FrequencySet getFrequencySet() {
		if (this.frequencySet == null) {
			return null;
		}
		return this.frequencySet;
	}

	public double[] getFrequencies() {
		if (this.frequencySet == null) {
			return null;
		}
		return this.frequencySet.getValues();
	}

	public List<StageSpectrum> getAll() {
		return map.values().stream().collect(Collectors.toCollection(ArrayList::new));
	}

	public void add(int stageNumber, Gain stageGain, int index, Complex c) {
		Objects.requireNonNull(c, "complex cannot be null");
		if (c.isNaN()) {
			throw new IllegalArgumentException("stageNumber:" + stageNumber + ", at index:" + index + " cannot be NaN");
		}
		StageSpectrum stageSpectrum = map.get(stageNumber);
		if (stageSpectrum == null) {
			if (stageGain == null) {
				stageGain = StageGain.valueOf(1, 0);
			}
			stageSpectrum = new StageSpectrum(this, stageNumber, stageGain);
			map.put(stageNumber, stageSpectrum);
		}
		stageSpectrum.add(index, c);
	}

	public Unit<?> getInputUnit() {
		StageSpectrum stageSpectrum = this.get(0);
		if (stageSpectrum == null) {
			return null;
		}
		Gain gain = stageSpectrum.getStageGain();
		if (gain instanceof Sensitivity) {
			return ((Sensitivity) gain).getInputUnits();
		} else {
			return null;
		}
	}

	public Unit<?> getOutputUnit() {
		StageSpectrum stageSpectrum = this.get(0);
		if (stageSpectrum == null) {
			return null;
		}
		Gain gain = stageSpectrum.getStageGain();
		if (gain instanceof Sensitivity) {
			return ((Sensitivity) gain).getOutputUnits();
		} else {
			return null;
		}
	}

	public StageSpectrum get(int stageNumber) {
		return map.get(stageNumber);
	}

	public Complex get(int stageNumber, int index) {
		StageSpectrum stageSpectrum = map.get(stageNumber);
		if (stageSpectrum == null) {
			return null;
		}
		return stageSpectrum.get(index);
	}

	public int size() {
		return this.map.size();
	}

	public Complex[] calculateTotalSpectrum() {
		Complex[] array = new Complex[frequencySet.size()];

		for (int i = 0; i < array.length; i++) {
			Complex c = new Complex(1, 0);
			for (StageSpectrum stageSpectra : map.values()) {
				c = c.multiply(stageSpectra.get(i));
			}
			array[i] = c;
		}
		return array;
	}

	public Complex calculateStageSpectrum(int index) {
		Complex c = new Complex(1, 0);

		for (StageSpectrum stageSpectra : map.values()) {
			c = c.multiply(stageSpectra.get(index));
		}
		return c;
	}

	public Complex[][] asDisplacement() {
		Complex[][] array = new Complex[size()][this.frequencySet.size()];
		Iterator<StageSpectrum> it = map.values().iterator();
		while (it.hasNext()) {
			StageSpectrum ss = it.next();
			int stageNumber = ss.getStageNumber();
			array[stageNumber] = ss.asDisplacement();
		}
		return array;
	}

	public Complex[] asDisplacement(int stageNumber) {
		StageSpectrum stageSpectrum = map.get(stageNumber);
		if (stageSpectrum == null) {
			return null;
		}
		return stageSpectrum.asDisplacement();
	}

	public Complex[] asAcceleration(int stageNumber) {
		StageSpectrum stageSpectrum = map.get(stageNumber);
		if (stageSpectrum == null) {
			return null;
		}
		return stageSpectrum.asAcceleration();
	}

	class StageSpectrum {
		private ResponseSpectrum response;
		int stageNumber;
		Gain stageGain;
		Complex[] array;

		StageSpectrum(ResponseSpectrum response, int stageNumber, Gain stageGain) {
			this.response = response;
			this.stageNumber = stageNumber;
			this.stageGain = stageGain;
			array = new Complex[response.frequencySet.size()];
		}

		Gain getStageGain() {
			return stageGain;
		}

		void add(int index, Complex element) {
			Objects.requireNonNull(element, "Complex element cannot be null.");
			array[index] = element;
		}

		Complex get(int index) {
			return array[index];
		}

		Complex[] getAll() {
			return array;
		}

		int getStageNumber() {
			return stageNumber;
		}

		int size() {
			return array == null ? 0 : array.length;
		}

		/**
		 * Calculates amplitude/phase values via the complex-spectra values from the
		 * given reponse arrays/information object.
		 * 
		 * @param rArrsInfoObj reponse arrays/information object to use.
		 * @param respArrIdx   associated index into 'respArraysInfoArray[]'.
		 * @return true if successful; false if an error occurred.
		 * @throws Exception
		 */
		protected double[][] calculatePhase(double listInterpTension, boolean listInterpOutFlag,
				boolean unwrapPhaseFlag) throws Exception {

			double[][] phaseArray = new double[2][];
			for (int i = 0; i < array.length; i++) {
				Complex complex = this.array[i];
				phaseArray[1][i] = complex.abs();
				phaseArray[1][i] = Math
						.sqrt(complex.getReal() * complex.getReal() + complex.getImaginary() * complex.getImaginary());
				// ampPhaseArray[1][i] = Math.atan2(complex.getImaginary(), complex.getReal() +
				// 1.0e-200) * 180.0
				// / Math.PI;
			}

			if (listInterpOutFlag) { // interpolate amp/phase values generated via List blockette
				try { // use req freqs, with any out-of-range freqs clipped
						// (if all-stages entry then show "Note:" messages):
					final double[] newFreqArray = EvalRespUtil.clipFreqArray(frequencySet.getValues(), true);
					if (newFreqArray.length <= 0) { // all requested frequencies were clipped
						throw new Exception("Error interpolating amp/phase output values:  "
								+ "All requested freqencies out of range");
					}
					final CubicSpline splineObj = new CubicSpline();

					// unwrap phase data:
					final double[] unwrappedPhaseArr = EvalRespUtil.unwrapPhaseArray(phaseArray[1]);
					double[] listInterpPhaseArr;
					// interpolate List blockette phase values:
					if ((listInterpPhaseArr = splineObj.calcSpline(frequencySet.getValues(), unwrappedPhaseArr,
							listInterpTension, 1.0, newFreqArray)) == null) { // error interploating values
						throw new Exception("Error interpolating phase output values:  ");
					}
					// if unwrap flag not set and phase data was unwrapped
					// then wrap interpolated data:
					if (!unwrapPhaseFlag && unwrappedPhaseArr != phaseArray[1]) {
						listInterpPhaseArr = EvalRespUtil.wrapPhaseArray(listInterpPhaseArr);
					}
					phaseArray[0] = newFreqArray;
					phaseArray[1] = listInterpPhaseArr;
				} catch (Exception ex) { // some kind of error; set error message
					throw new Exception("Exception error interpolating amp/phase output values", ex);
				}
			}
			if (unwrapPhaseFlag) { // flag set for unwrapping phase values (via "-unwrap" parameter)
				try { // get generated phase data:
						// final double[] srcPhaseArr = ampPhaseArray[1];
					final double[] unwrappedPhaseArr = // unwrap phase data
							EvalRespUtil.unwrapPhaseArray(phaseArray[1], true);
					if (unwrappedPhaseArr != phaseArray[1]) { // phase data changed; enter with new phase data
						phaseArray[1] = unwrappedPhaseArr;
					}
				} catch (Exception ex) { // some kind of error; set error message
					throw new Exception("Exception error unwrapping phase output values", ex);
				}
			}
			return phaseArray;
		}

		public Complex[] asDisplacement() {
			Complex[] array = new Complex[size()];
			for (int index = 0; index < array.length; index++) {
				Complex ofNum = this.array[index];
				double wVal = 2 * Math.PI * frequencySet.get(index);
				if (StationUnits.isDisplacement(response.getInputUnit())) {
					// convert to velocity
					if (wVal != 0.0) {
						ofNum = ofNum.multiply(new Complex(0, -1 / wVal));
					} else {
						ofNum = new Complex(0, 0);// .real = ofNum.imag = 0.0;
					}
				} else if (StationUnits.isAcceleration(response.getInputUnit())) {
					// convert to velocity
					// if input unit is 'accel' then convert to 'velocity':
					ofNum = new Complex(0, wVal);
				}

				ofNum = ofNum.multiply(new Complex(0, wVal));
				array[index] = ofNum;
			}
			return array;
		}

		public Complex[] asAcceleration() {
			Complex[] array = new Complex[size()];
			for (int index = 0; index < map.size(); index++) {
				Complex ofNum = array[index];
				double wVal = 2 * Math.PI * frequencySet.get(index);
				if (StationUnits.isDisplacement(response.getInputUnit())) {
					// convert to velocity
					if (wVal != 0.0) {
						ofNum = ofNum.multiply(new Complex(0, -1 / wVal));
					} else {
						ofNum = new Complex(0, 0);// .real = ofNum.imag = 0.0;
					}
				} else if (StationUnits.isAcceleration(response.getInputUnit())) {
					// convert to velocity
					// if input unit is 'accel' then convert to 'velocity':
					ofNum = new Complex(0, wVal);
				}

				if (wVal != 0.0) {
					ofNum = ofNum.multiply(new Complex(0.0, -1.0 / wVal));
				} else {
					ofNum = new Complex(0, 0);
				}

				array[index] = ofNum;
			}
			return array;
		}
	}
}
