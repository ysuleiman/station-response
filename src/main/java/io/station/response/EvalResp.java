package io.station.response;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import org.apache.commons.math3.complex.Complex;

import io.station.model.Channel;
import io.station.model.Coefficients;
import io.station.model.Decimation;
import io.station.model.FDSNStationXML;
import io.station.model.Gain;
import io.station.model.Network;
import io.station.model.PolesZeros;
import io.station.model.Polynomial;
import io.station.model.PzTransferFunctionType;
import io.station.model.Response;
import io.station.model.ResponseList;
import io.station.model.ResponseListElement;
import io.station.model.ResponseStage;
import io.station.model.SampleRate;
import io.station.model.Station;
import io.station.response.ResponseSpectrum.StageSpectrum;
import io.station.response.util.FrequencySet;
import io.station.uom.StationUnits;
import io.station.validation.ErrorHandler;
import io.station.validation.Validator;

public class EvalResp {

	private EvalResp() {
	}
	/*
	 * private static FDSNStationXML read(Path path) throws IOException {
	 * ContentTypeDescriptor contentTypeDescriptor = null; try {
	 * contentTypeDescriptor =
	 * DefaultContentDiscoveryService.instance().discover(path); } catch
	 * (UnkownContentTypeException e) { throw new IOException(e); } if
	 * (contentTypeDescriptor == null) { throw new IOException("Unkown file type.");
	 * }
	 * 
	 * try (InputStream inputStream = Files.newInputStream(path);) { FDSNStationXML
	 * document = null; if
	 * (DefaultContentTypeDescriptor.respTypeDescriptor().equals(
	 * contentTypeDescriptor)) { document = RespIO.read(inputStream); } else if
	 * (DefaultContentTypeDescriptor.stationXmlTypeDescriptor().equals(
	 * contentTypeDescriptor)) { document = StationIO.read(inputStream); } else if
	 * (DefaultContentTypeDescriptor.seedTypeDescriptor().equals(
	 * contentTypeDescriptor)) { SeedVolume volume =
	 * SeedIO.toSeedVolume(inputStream); if (volume == null) { throw new
	 * IOException("Null: Error reading Seed volume."); } ConversionService
	 * conversionService = (ConversionService) DefaultFileConversionService
	 * .getSharedInstance(); document = (FDSNStationXML)
	 * conversionService.convert(volume, FDSNStationXML.class); } else { throw new
	 * IOException("Unkown document type!"); }
	 * 
	 * if (document == null) { throw new
	 * IOException("Null: Couldn't read document."); } return document; } catch
	 * (JAXBException | SAXException e) { throw new IOException(e); } }
	 * 
	 * public static List<NormalizedResponse> normalize(Path path) throws
	 * IOException, InvalidResponseException {
	 * 
	 * ContentTypeDescriptor contentTypeDescriptor = null; try {
	 * contentTypeDescriptor =
	 * DefaultContentDiscoveryService.instance().discover(path); } catch
	 * (UnkownContentTypeException | IOException e) { throw new IOException(e); } if
	 * (contentTypeDescriptor == null) { throw new IOException("Unkown file type.");
	 * }
	 * 
	 * try (InputStream inputStream = Files.newInputStream(path);) { FDSNStationXML
	 * document = null; if
	 * (DefaultContentTypeDescriptor.respTypeDescriptor().equals(
	 * contentTypeDescriptor)) { document = RespIO.read(inputStream); } else if
	 * (DefaultContentTypeDescriptor.stationXmlTypeDescriptor().equals(
	 * contentTypeDescriptor)) { document = StationIO.read(inputStream); } else if
	 * (DefaultContentTypeDescriptor.seedTypeDescriptor().equals(
	 * contentTypeDescriptor)) { SeedVolume volume =
	 * SeedIO.toSeedVolume(inputStream); if (volume == null) { throw new
	 * IOException("Null: Error reading Seed volume."); } ConversionService
	 * conversionService = (ConversionService) DefaultFileConversionService
	 * .getSharedInstance(); document = (FDSNStationXML)
	 * conversionService.convert(volume, FDSNStationXML.class); } else { throw new
	 * IOException("Unkown document type!"); }
	 * 
	 * if (document == null) { throw new
	 * IOException("Null: Couldn't read document."); } List<Network> networks =
	 * document.getNetwork(); if (networks == null || networks.isEmpty()) { throw
	 * new InvalidResponseException("Document is empty, no network(s) found!"); }
	 * List<NormalizedResponse> list = new ArrayList<>(); for (Network network :
	 * networks) { List<Station> stations = network.getStations(); if (stations !=
	 * null && !stations.isEmpty()) { for (Station station : stations) {
	 * List<Channel> channels = station.getChannels(); if (channels != null &&
	 * !channels.isEmpty()) { for (Channel channel : channels) { NormalizedResponse
	 * normalizedResponse = normalize(channel); if (normalizedResponse != null) {
	 * list.add(normalizedResponse); } } } } } } return list; } catch (JAXBException
	 * | SAXException e) { throw new IOException(e); } }
	 */

	public static NormalizedResponse normalize(Channel channel) throws InvalidResponseException {
		Objects.requireNonNull(channel, "channel cannot be null.");
		return NormalizedResponse.wrap(channel);
	}

	public static NormalizedResponse normalize(String network, String station, String location, String channel,
			ZonedDateTime start, ZonedDateTime end, SampleRate sampleRate, Response response)
			throws InvalidResponseException {
		Objects.requireNonNull(response, "response cannot be null.");
		return NormalizedResponse.wrap(network, station, location, channel, start, end, sampleRate, response);
	}

	public static void check(Response response) throws InvalidResponseException {
		// Errors errors = EvalRespErrors.newInstance("Response", "Errors found when
		// validating response.");
		Validator validator = new EvalRespValidator();
		validator.setErrorHandler(new ErrorHandler());
		validator.validate(response);
		ErrorHandler errorHandler = validator.getErrorHandler();
		// ValidationUtils.invokeValidator(new EvalRespValidator(), response, errors);
		if (errorHandler.hasErrors()) {
			throw new InvalidResponseException(errorHandler.getErrors());
		}
	}

	public static int toUnitConvIndex(javax.measure.Unit<?> unit) {
		if (StationUnits.isDisplacement(unit)) {
			return 1;
		} else if (StationUnits.isAcceleration(unit)) {
			return 3;
		} else if (StationUnits.isSpeed(unit)) {
			return 2;
		}
		return -1; // return no-conversion value
	}

	/*public static List<ResponseSpectrum> calculate(Path path) throws Exception {
		Objects.requireNonNull(path, "path cannot be null.");
		return calculate(path, EvalRespUtil.createFrequency());
	}

	public static List<ResponseSpectrum> calculate(Path path, FrequencySet frequencySet) throws Exception {
		Objects.requireNonNull(path, "path cannot be null.");
		FDSNStationXML document = read(path);
		if (document == null) {
			return Collections.emptyList();
		}
		return calculate(document, frequencySet);
	}*/

	public static List<ResponseSpectrum> calculate(FDSNStationXML document) throws Exception {
		Objects.requireNonNull(document, "document cannot be null.");
		return calculate(document, EvalRespUtil.createFrequency());
	}

	public static List<ResponseSpectrum> calculate(FDSNStationXML document, FrequencySet frequencySet)
			throws Exception {
		Objects.requireNonNull(document, "document cannot be null.");
		List<Network> networks = document.getNetwork();
		if (networks == null || networks.isEmpty()) {
			return null;
		}
		List<ResponseSpectrum> list = new ArrayList<>();
		for (Network network : networks) {
			List<ResponseSpectrum> responseSpectrum = calculate(network, frequencySet);
			if (responseSpectrum != null) {
				list.addAll(responseSpectrum);
			}
		}
		return list;
	}

	public static List<ResponseSpectrum> calculate(Network network, FrequencySet frequencySet) throws Exception {
		Objects.requireNonNull(network, "network cannot be null.");
		List<Station> stations = network.getStations();
		if (stations == null || stations.isEmpty()) {
			return null;
		}
		List<ResponseSpectrum> list = new ArrayList<>();
		for (Station station : stations) {
			List<ResponseSpectrum> responseSpectrum = calculate(station, frequencySet);
			if (responseSpectrum != null) {
				list.addAll(responseSpectrum);
			}
		}
		return list;

	}

	public static List<ResponseSpectrum> calculate(Station station, FrequencySet frequencySet) throws Exception {
		Objects.requireNonNull(station, "station cannot be null.");
		List<Channel> channels = station.getChannels();
		if (channels == null || channels.isEmpty()) {
			return null;
		}
		List<ResponseSpectrum> list = new ArrayList<>();
		for (Channel channel : channels) {
			ResponseSpectrum responseSpectrum = calculate(channel, frequencySet);
			if (responseSpectrum != null) {
				list.add(responseSpectrum);
			}
		}
		return list;
	}

	public static ResponseSpectrum calculate(Channel channel, FrequencySet frequencySet) throws Exception {
		Objects.requireNonNull(channel, "channel cannot be null.");
		return calculate(channel, frequencySet, false, false, false, false, false, 0, false, false, 0);
	}

	public static ResponseSpectrum calculate(Channel channel, FrequencySet frequencySet, boolean logSpacingFlag,
			boolean useEstDelayFlag, boolean showInputFlag, boolean listInterpOutFlag, boolean listInterpInFlag,
			double listInterpTension, boolean unwrapPhaseFlag, boolean totalSensitFlag, double b62XValue)
			throws Exception {
		Objects.requireNonNull(channel, "response cannot be null.");
		Response response = channel.getResponse();
		if (response == null || response.isEmpty()) {

		}
		return calculate(normalize(channel), frequencySet, logSpacingFlag, 0, response.size(), useEstDelayFlag,
				listInterpOutFlag, listInterpInFlag, listInterpTension, unwrapPhaseFlag, totalSensitFlag, b62XValue);
	}

	public static ResponseSpectrum calculate(Channel channel, FrequencySet frequencySet, boolean logSpacingFlag,
			int startStageNum, int stopStageNum, boolean useEstDelayFlag, boolean listInterpOutFlag,
			boolean listInterpInFlag, double listInterpTension, boolean unwrapPhaseFlag, boolean totalSensitFlag,
			double b62XValue) throws Exception {
		Objects.requireNonNull(channel, "channel cannot be null.");
		return calculate(normalize(channel), frequencySet, logSpacingFlag, startStageNum, stopStageNum, useEstDelayFlag,
				listInterpOutFlag, listInterpInFlag, listInterpTension, unwrapPhaseFlag, totalSensitFlag, b62XValue);
	}

	public static ResponseSpectrum calculate(NormalizedResponse normalizedResponse, FrequencySet frequencySet)
			throws Exception {
		Objects.requireNonNull(normalizedResponse, "normalizedResponse cannot be null.");
		return calculate(normalizedResponse, frequencySet, false, 1, normalizedResponse.size() + 1, false, false, false,
				0, false, false, 0);
	}

	public static ResponseSpectrum calculate(NormalizedResponse normalizedResponse, FrequencySet frequencySet,
			boolean logSpacingFlag, int startStageNum, int stopStageNum, boolean useEstDelayFlag,
			boolean listInterpOutFlag, boolean listInterpInFlag, double listInterpTension, boolean unwrapPhaseFlag,
			boolean totalSensitFlag, double b62XValue) throws Exception {
		Objects.requireNonNull(normalizedResponse, "normalizedResponse cannot be null.");
		double phaseConvVal = 1;

		if (!normalizedResponse.isEmpty()) {
			ResponseStage stage = normalizedResponse.getStage(1);
			if (stage != null) {
				ResponseList responseList = stage.getResponseList();
				if (responseList != null) {

				}
			}
		}

		if (stopStageNum < startStageNum) {
			throw new IllegalArgumentException(
					"stopStageNum:[" + stopStageNum + "] cannot be less than startStageNum[" + startStageNum + "]");
		}
		Complex cNum = null;
		// Complex dfNum = null;
		ResponseSpectrum responseSpectra = new ResponseSpectrum(normalizedResponse, frequencySet);

		for (int fIdx = 0; fIdx < frequencySet.size(); ++fIdx) { // for each frequency value
			double freqVal = frequencySet.get(fIdx);
			double wVal = 2 * Math.PI * freqVal;
			for (int stageNum = startStageNum; stageNum < stopStageNum; stageNum++) {
				double normalizationFactor = normalizedResponse.getNormalizationFactor(stageNum);
				if (normalizationFactor == 0) {
					normalizationFactor = 1.0;
				}
				Complex ofNum = null;
				cNum = new Complex(1, 0);
				PolesZeros polesZeros = normalizedResponse.getPolesZeros(stageNum);
				if (polesZeros != null) {
					PzTransferFunctionType functionType = polesZeros.getPzTransferFunctionType();
					// A single character describing the type of stage:
					// A — Laplace transform analog response, in rad/sec
					// B — Analog response, in Hz
					// C — Composite (currently undefined)
					// D — Digital (Z - transform)
					if (functionType == PzTransferFunctionType.LAPLACE_HERTZ
							|| functionType == PzTransferFunctionType.LAPLACE_RADIANS_SECOND) {
						ofNum = EvalRespUtil.analogTrans(polesZeros, normalizationFactor,
								((functionType == PzTransferFunctionType.LAPLACE_RADIANS_SECOND) ? 2 * Math.PI * freqVal
										: freqVal));
					} else if (functionType == PzTransferFunctionType.DIGITAL_Z_TRANSFORM) {
						Decimation decimation = normalizedResponse.getDecimation(stageNum);
						if (decimation == null) {
							throw new InvalidResponseException(
									"Required decimation not found in " + "stage #" + stageNum);
						}
						Quantity<Time> samplingInterval = decimation.getInputSampleRate().calculateSamplingInterval();
						if (samplingInterval == null) {
							throw new InvalidResponseException("Invalid decimation object in stage #" + stageNum);
						}
						ofNum = EvalRespUtil.iirPzTrans(polesZeros, normalizationFactor,
								samplingInterval.getValue().doubleValue(), 2 * Math.PI * freqVal);
					} else {
						throw new InvalidResponseException(
								"Invalid transfer type for poles/zeros " + "filter in stage #" + stageNum);
					}
				}
				Coefficients coefficients = normalizedResponse.getCoefficients(stageNum);
				if (coefficients != null) {
					if ((coefficients.getDenominators() != null && !coefficients.getDenominators().isEmpty())
							|| (coefficients.getNumerators() != null && !coefficients.getNumerators().isEmpty())) {

						ofNum = EvalRespUtil.calculateSpectrum(coefficients, normalizationFactor,
								normalizedResponse.getDecimation(stageNum), wVal, useEstDelayFlag);
					}
				}
				ResponseList responseList = normalizedResponse.getResponseList(stageNum);
				if (responseList != null) {
					double[] splineInterpolatedAmplitude = EvalRespUtil.splineInterpolatePhase(frequencySet.getValues(),
							responseList);
					double[] splineInterpolatedPhase = EvalRespUtil.splineInterpolateAmplitude(frequencySet.getValues(),
							responseList);
					double amp = 0;
					double phase = 0;
					if (listInterpInFlag) {
						amp = splineInterpolatedAmplitude[fIdx];
						phase = splineInterpolatedPhase[fIdx] * phaseConvVal;
					} else {
						ResponseListElement element = responseList.getElement(fIdx);
						amp = element.getAmplitude().getValue(); // get amplitude value
						// get phase value, convert degrees to radians (if nec):
						phase = element.getPhase().getValue() * phaseConvVal;
					}
					ofNum = new Complex(amp * Math.cos(phase), amp * Math.sin(phase));
				}

				Polynomial polynomial = normalizedResponse.getPolynomial(stageNum);
				if (polynomial != null) {
					ofNum = EvalRespUtil.polynomial(polynomial, 0);
				}
				responseSpectra.add(stageNum, normalizedResponse.getStageGain(stageNum), fIdx,
						ofNum == null ? cNum : cNum.multiply(ofNum));
			}

			// calculate response value for all stages put together:
			Complex c = new Complex(1, 0);
			for (int stageNumber = 1; stageNumber < responseSpectra.size(); stageNumber++) {
				Complex sComplex = responseSpectra.get(stageNumber, fIdx);
				if (sComplex != null) {
					c = c.multiply(sComplex);
				}
			}
			responseSpectra.add(0, normalizedResponse.getStageGain(BigInteger.valueOf(0)), fIdx, c);

			double totalSensitVal = 1.0;
			Gain sensitivity = normalizedResponse.getStageGain(BigInteger.valueOf(0));
			if (totalSensitFlag && sensitivity != null) {
				totalSensitVal = sensitivity.getValue();
			}

			for (int stageNumber = 0; stageNumber < stopStageNum; stageNumber++) {
				double calcSensVal = 1.0;
				StageSpectrum stageSpectrum = responseSpectra.get(stageNumber);
				if (stageSpectrum != null) {
					c = stageSpectrum.get(fIdx);
					if (c != null) {
						if (totalSensitFlag) { // using stage 0 (total) sensitivity
							// multiply in sensitivity and unit conv scale factor:
							c = c.multiply(totalSensitVal);
						} else { // using computed sensitivity from each stage
									// get calculated sensitivity value for stage index:
							Gain stageGain = normalizedResponse.getStageGain(stageNumber);
							if (stageGain != null) {
								calcSensVal = stageGain.getValue();
							}
							// multiply in sensitivity and unit conv scale factor:
							c = c.multiply(calcSensVal);
						}
						stageSpectrum.add(fIdx, c);
					}
				}
			}
		}
		return responseSpectra;
	}
}
