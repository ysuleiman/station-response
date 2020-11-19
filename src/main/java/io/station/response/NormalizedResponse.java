package io.station.response;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.math3.complex.Complex;

import io.station.model.BaseFilter;
import io.station.model.Channel;
import io.station.model.Coefficients;
import io.station.model.Decimation;
import io.station.model.Frequency;
import io.station.model.Gain;
import io.station.model.Network;
import io.station.model.PolesZeros;
import io.station.model.Polynomial;
import io.station.model.PzTransferFunctionType;
import io.station.model.Response;
import io.station.model.ResponseList;
import io.station.model.ResponseStage;
import io.station.model.SampleRate;
import io.station.model.Sensitivity;
import io.station.model.StageGain;
import io.station.model.Station;

public class NormalizedResponse {

	private String network;
	private String station;
	private String location;
	private String channel;
	private ZonedDateTime start;
	private ZonedDateTime end;
	private SampleRate sampleRate;
	private final Response response;

	private Map<BigInteger, Normalized> normalization = new HashMap<>();
	private Map<BigInteger, Gain> gainMap = new HashMap<>();

	public NormalizedResponse(String network, String station, Channel channel) {
		Objects.requireNonNull(channel, "channel cannot be null.");
		this.network = network;
		this.station = station;
		this.location = channel.getLocationCode();
		this.channel = channel.getCode();
		this.start = channel.getStartDate();
		this.end = channel.getEndDate();
		this.sampleRate = channel.getSampleRate();

		this.response = channel.getResponse();
	}

	public NormalizedResponse(String network, String station, String location, String channel, ZonedDateTime start,
			ZonedDateTime end, SampleRate sampleRate, Response response) {
		Objects.requireNonNull(response, "response cannot be null.");
		this.network = network;
		this.station = station;
		this.location = location;
		this.channel = channel;
		this.start = start;
		this.end = end;
		this.sampleRate = sampleRate;

		this.response = response;
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

	private void putNormalization(BigInteger stageNumber, Normalized normalized) {
		Objects.requireNonNull(stageNumber, "stageNumber cannot be null.");
		Objects.requireNonNull(normalized, "normalized cannot be null.");
		normalized.validate();
		normalization.put(stageNumber, normalized);
	}

	private void putGain(BigInteger stageNumber, Gain gain) {
		Objects.requireNonNull(stageNumber, "stageNumber cannot be null.");
		gainMap.put(stageNumber, gain);
	}

	private static boolean isValid(Gain gain) {
		if (gain == null || gain.getValue() == null) {
			return false;
		}
		if (gain.getValue() != 0) {
			return true;
		}
		return false;
	}

	public static NormalizedResponse wrap(Channel channel) throws InvalidResponseException {
		Objects.requireNonNull(channel, "channel cannot be null.");
		Station station = channel.getStation();
		String stationCode = null;
		String networkCode = null;
		if (station != null) {
			stationCode = station.getCode();
			Network network = station.getNetwork();
			if (network != null) {
				networkCode = network.getCode();
			}
		}

		return wrap(networkCode, stationCode, channel.getLocationCode(), channel.getCode(), channel.getStartDate(),
				channel.getEndDate(), channel.getSampleRate(), channel.getResponse());
	}

	public static NormalizedResponse wrap(String network, String station, String location, String channel,
			ZonedDateTime start, ZonedDateTime end, SampleRate sampleRate, Response response)
			throws InvalidResponseException {
		Objects.requireNonNull(response, "response cannot be null.");
		Objects.requireNonNull(network, "network cannot be null.");
	
		EvalResp.check(response);

		NormalizedResponse normalizedResponse = new NormalizedResponse(network, station, location, channel, start, end,
				sampleRate, response);

		Frequency calculatedFrequency = calculateFrequency(response);

		Sensitivity instrumentSensitivity = calculateSensitivity(response);
		if (instrumentSensitivity == null) {

		}
		normalizedResponse.putGain(BigInteger.valueOf(0), instrumentSensitivity);

		for (ResponseStage stage : response.getStages()) {
			BaseFilter filter = null;
			List<BaseFilter> filters = stage.getFilters();
			if (filters != null && !filters.isEmpty()) {
				filter = filters.get(0);
				if (filter == null) { // no filter object; set error message
					throw new InvalidResponseException("Filter[0] of stage #" + stage.getNumber() + " is null");
				}
			}
			StageGain stageGain = stage.getStageGain();

			if (!isValid(stageGain)) {
				if (filter == null || !stage.isPolynomial()) {
					if (response.getStages().size() > 1) {// more than one stage in response; set error message
						throw new InvalidResponseException(
								"No gain value for stage #" + stage.getNumber() + " of multi-stage response");

					}
					if (!isValid(instrumentSensitivity)) { // no stage 0 sensitivity for
						// response; set error message
						throw new InvalidResponseException(
								"No 'stage 0' response sensitivity or gain " + "value for single stage");
					}
				}
				// enter overall sensitivity as gain for single stage:
				stageGain = StageGain.valueOf(instrumentSensitivity.getValue(), instrumentSensitivity.getFrequency());
			}
			normalizedResponse.putGain(stage.getNumber(), stageGain);

			if (filter != null) {
				// stage contains filters (not gain-only stage)
				// setup normalization object:
				if (filter instanceof PolesZeros) {
					PolesZeros polesZeros = (PolesZeros) filter;
					// bbb
					normalizedResponse.putNormalization(stage.getNumber(), new Normalized(
							polesZeros.getNormalizationFactor(), polesZeros.getNormalizationFrequency()));
				}
				if (!stage.isPolynomial()) {
					Complex ofNum = null;
					Complex dfNum = null;
					Normalized normalized = normalizedResponse.getStageNormalization(stage.getNumber());
					if (!calculatedFrequency.equals(Frequency.valueOf(stageGain.getFrequency()))
							|| (normalized != null && !calculatedFrequency.equals(normalized.getFrequency()))) {
						if (filter instanceof PolesZeros) {
							PolesZeros polesZeros = (PolesZeros) filter;
							if (polesZeros.getNormalizationFactor() == null) {
								throw new NullPointerException(
										"No normalization for poles/zeros " + "filter in stage #" + stage.getNumber());
							}
							Frequency frequency = polesZeros.getNormalizationFrequency();

							if (frequency == null || !Objects.equals(frequency, calculatedFrequency)) {
								dfNum = EvalRespUtil.analogTrans(polesZeros, 1,
										((polesZeros
												.getPzTransferFunctionType() == PzTransferFunctionType.LAPLACE_HERTZ)
														? 2 * Math.PI * stageGain.getFrequency()
														: stageGain.getFrequency()));

								ofNum = EvalRespUtil.analogTrans(polesZeros, 1,
										((polesZeros
												.getPzTransferFunctionType() == PzTransferFunctionType.LAPLACE_HERTZ)
														? 2 * Math.PI * calculatedFrequency.getValue()
														: calculatedFrequency.getValue()));
							} else {
								normalizedResponse.putNormalization(stage.getNumber(), new Normalized(
										polesZeros.getNormalizationFactor(), polesZeros.getNormalizationFrequency()));
								System.out.println("111Setting normalization: " + stage.getNumber());
							}
						} else if (filter instanceof Coefficients) {
							Coefficients coefficients = (Coefficients) filter;
							Complex[] array = EvalRespUtil.normalize(coefficients, stage.getStageGain(),
									stage.getDecimation(), calculatedFrequency.toAngular());
							dfNum = array[0];
							ofNum = array[1];
						} else {
							dfNum = ofNum = null;
						}
						if (dfNum != null && ofNum != null) { // values were entered; process them
							double ofSqVal = ofNum.abs();
							double newGainVal = stageGain.getValue() / (dfNum.abs() * ofSqVal);
							stageGain = StageGain.valueOf(newGainVal, calculatedFrequency);
							normalizedResponse.putGain(stage.getNumber(), stageGain);
							normalizedResponse.putNormalization(stage.getNumber(),
									new Normalized(1.0 / ofSqVal, calculatedFrequency));
							System.out.println("Setting normalization: " + stage.getNumber());
						} else {
							System.out.println(":::Setting normalization: " + stage.getNumber());
							normalizedResponse.putGain(stage.getNumber(),
									StageGain.valueOf(stageGain.getValue(), calculatedFrequency));
						}
					} else {
						normalizedResponse.putGain(stage.getNumber(),
								StageGain.valueOf(stageGain.getValue(), calculatedFrequency));
					}
				} else {
					normalizedResponse.putGain(stage.getNumber(), StageGain.valueOf(1, calculatedFrequency));
				}
			} else {
				normalizedResponse.putGain(stage.getNumber(), StageGain.valueOf(1, calculatedFrequency));
			}
		}
		return normalizedResponse;
	}

	private double getSensitivity() {
		if (this.response == null) {
			throw new NullPointerException();
		}
		Sensitivity instrumentSensitivity = response.getInstrumentSensitivity();
		if (instrumentSensitivity != null) {
			if (instrumentSensitivity.getValue() != null) {
				return instrumentSensitivity.getValue();
			}
		}
		return 0;
	}

	/*
	 * private double getFrequency() { if (this.response == null) { throw new
	 * NullPointerException(); } Sensitivity instrumentSensitivity =
	 * response.getInstrumentSensitivity(); if (instrumentSensitivity != null) { if
	 * (instrumentSensitivity.getFrequency() != null) { return
	 * instrumentSensitivity.getFrequency(); } } return 0; }
	 */

	/**
	 * look for the instrumentSensitivity, if not found calculate from stages, if
	 * non found return 0
	 * 
	 * @return sensitivity
	 */
	private static Sensitivity calculateSensitivity(Response response) {
		Objects.requireNonNull(response, "response cannot be null.");
		Sensitivity sensitivity = response.getInstrumentSensitivity();
		if (sensitivity != null) {
			return sensitivity;
		}

		List<ResponseStage> stages = response.getStages();
		if (stages == null || stages.isEmpty()) {
			return null;
		}
		sensitivity = new Sensitivity();
		double totalSensitivity = 1;

		for (ResponseStage stage : stages) {
			StageGain stageGain = stage.getStageGain();
			if (stageGain == null) {
				continue;
			}
			if (stageGain.getValue() != null && stageGain.getValue() != 0) {
				totalSensitivity *= stageGain.getValue();
				sensitivity.setValue(totalSensitivity);
			}
			if (stageGain.getFrequency() != null) {
				sensitivity.setFrequency(stageGain.getFrequency());
			}
		}
		return sensitivity;
	}

	/**
	 * if no sensitivity for response then look for the last non-zero frequency
	 * value among the stage gains (since filters are typically for low pass
	 * purposes, the last non-zero frequency is likely the best choice, as its pass
	 * band is the narrowest):
	 * 
	 * 
	 * @return sensitivity
	 */
	private static Frequency calculateFrequency(Response response) {
		Objects.requireNonNull(response, "response cannot be null");

		List<ResponseStage> stages = response.getStages();
		if (stages == null || stages.isEmpty()) {
			return null;
		}
		Frequency frequency = null;

		for (ResponseStage stage : stages) {
			StageGain stageGain = stage.getStageGain();
			if (stageGain != null && stageGain.getFrequency() != null && stageGain.getFrequency() != 0) {
				frequency = Frequency.valueOf(stageGain.getFrequency());
			}
		}
		return frequency;
	}

	public ResponseStage getStage(int stageNumber) {
		List<ResponseStage> stages = response.getStages();
		if (stages == null || stages.isEmpty()) {
			return null;
		}
		for (ResponseStage stage : stages) {
			if (stage.getNumber().intValue() == stageNumber) {
				return stage;
			}
		}
		return null;
	}

	public Normalized getStageNormalization(BigInteger stageNumber) {
		return this.normalization.get(stageNumber);
	}

	public Gain getStageGain(BigInteger stageNumber) {
		return this.gainMap.get(stageNumber);
	}

	public double getNormalizationFactor(int stageNumber)
			throws StageNotFoundException, EmptyStageException, InvalidResponseException {
		Normalized normalized = this.normalization.get(BigInteger.valueOf(stageNumber));
		if (normalized == null) {
			return 0;
		}
		return normalized.getValue();
	}

	/*
	 * public Sensitivity getInstrumentSensitivity() { if (response == null) {
	 * 
	 * } return response.getInstrumentSensitivity(); }
	 */

	public Frequency getNormalizationFrequency(int stageNumber) throws InvalidResponseException {
		Normalized normalized = this.normalization.get(BigInteger.valueOf(stageNumber));
		if (normalized == null) {
			return null;
		}
		return normalized.getFrequency();
	}

	public Coefficients getCoefficients(int stageNumber) {
		ResponseStage stage = getStage(stageNumber);
		if (stage == null) {
			return null;
		}
		return stage.getCoefficients();
	}

	public PolesZeros getPolesZeros(int stageNumber) {
		ResponseStage stage = getStage(stageNumber);
		if (stage == null) {
			return null;
		}
		return stage.getPolesZeros();
	}

	public ResponseList getResponseList(int stageNumber) {
		ResponseStage stage = getStage(stageNumber);
		if (stage == null) {
			return null;
		}
		return stage.getResponseList();
	}

	public Decimation getDecimation(int stageNumber) {
		ResponseStage stage = getStage(stageNumber);
		if (stage == null) {
			return null;
		}
		return stage.getDecimation();
	}

	public Gain getStageGain(int stageNumber) throws InvalidResponseException {
		return gainMap.get(BigInteger.valueOf(stageNumber));
	}

	public Polynomial getPolynomial(int stageNumber) {
		ResponseStage stage = getStage(stageNumber);
		if (stage == null) {
			return null;
		}
		return stage.getPolynomial();
	}

	public BaseFilter getStageFilter(int stageNumber) throws InvalidResponseException {
		List<ResponseStage> stages = response.getStages();
		if (stages == null || stages.isEmpty()) {
			return null;
		}
		ResponseStage stage = stages.get(stageNumber);

		if (stage == null) {
			return null;
		}

		List<BaseFilter> filters = stage.getFilters();
		if (filters == null || filters.isEmpty()) {
			return null;
		}
		return filters.get(0);
	}

	public boolean isStagePolynomial(int stageNumber) {
		ResponseStage stage = response.getStage(stageNumber);
		if (stage == null) {
			return false;
		}
		return stage.isPolynomial();
	}

	public boolean isEmpty() {
		return response == null || response.isEmpty();
	}

	public int size() {
		List<ResponseStage> stages = response.getStages();
		if (stages == null || stages.isEmpty()) {
			return 0;
		}
		return stages.size();
	}

	class StageSensitivity {
		int stageNumber;

		double value;
	}

}
