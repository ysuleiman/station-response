package io.station.response.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import io.station.model.Channel;
import io.station.model.Network;
import io.station.model.Polynomial;
import io.station.model.Response;
import io.station.model.ResponseStage;
import io.station.model.ResponseType;
import io.station.model.Sensitivity;
import io.station.model.StageGain;
import io.station.model.Station;
import io.station.response.reader.RespReader;
import io.station.response.reader.ResponseTypeReaderFactory;
import io.station.util.ChannelReader;
import io.station.util.RewindableLineIterator;

public class RespChannelReader implements ChannelReader {

	private RewindableLineIterator lineIterator;

	Network network = null;
	Station station = null;

	public RespChannelReader(InputStream inputStream) {
		this.lineIterator = new RewindableLineIterator(new BufferedReader(new InputStreamReader(inputStream)));
	}

	@Override
	public Channel readNext() {
		Channel channel = null;
		try {
			while (lineIterator.hasNext()) {
				String line = lineIterator.next();
				if (line == null) {
					throw new IOException("Unexpected error!");
				}
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("B050F03") || line.startsWith("B050F16")) {
					if (channel != null) {
						lineIterator.rewind();
						return channel;
					}
					if (line.startsWith("B050F16")) {
						String[] array = line.split("\\s+");
						if (array.length < 3) {
							throw new IOException(
									"Invalid Resp format, expected to find a network code at index 2 but found none");
						}
						String networkCode = array[2];
						if (network == null || !networkCode.equals(network.getCode())) {
							network = new Network();
							network.setCode(networkCode);
							network.addStation(station);
						}
					} else if (line.startsWith("B050F03")) {
						String[] array = line.split("\\s+");
						if (array.length < 3) {
							throw new IOException(
									"Invalid Resp format, expected to find a station code at index 2 but found none");
						}
						String stationCode = array[2];
						if (station == null || !stationCode.equals(station.getCode())) {
							station = new Station();
							station.setCode(stationCode);
							station.setNetwork(network);
						}
					}
					continue;
				}
				if (line.startsWith("B052")) {
					lineIterator.rewind();
					if (channel != null) {
						return channel;
					}

					channel = readChannel();
					if (channel == null) {
						throw new IOException(
								"Error reading channel information, expected a channel but received null");
					}
					channel.setStation(station);
					continue;
				}

				if (line.length() < 7 || !line.startsWith("B0")) {
					throw new IOException(
							"Invalid Resp format, expected at least 7 characters but found " + line.length());
				}
				if (channel == null) {
					throw new IOException("Invalid Resp format, expected to find a channel but found none.");
				}
				Response response = channel.getResponse();
				if (response == null) {
					response = new Response();
					channel.setResponse(response);
				}

				String value = line.substring(2, 4).trim();
				int type = 0;
				try {
					lineIterator.rewind();
					type = Integer.parseInt(value);
					RespReader respReader = ResponseTypeReaderFactory.create(type);
					ResponseType responseType = respReader.read(lineIterator);
					if (responseType == null) {
						throw new IOException("Failed to read response of type:" + type);
					}
					int stageSequenceNumber = respReader.getStageSequenceNumber();
					if (stageSequenceNumber == 0) {
						if (responseType instanceof Sensitivity) {
							response.setInstrumentSensitivity((Sensitivity) responseType);
						} else if (responseType instanceof Polynomial) {
							response.setInstrumentPolynomial((Polynomial) responseType);
						}
					} else {
						ResponseStage stage = response.computeIfAbsent(stageSequenceNumber);
						if (responseType instanceof Sensitivity) {
							StageGain stageGain = new StageGain();
							stageGain.setValue(((Sensitivity) responseType).getValue());
							stageGain.setFrequency(((Sensitivity) responseType).getFrequency());
							stage.setStageGain(stageGain);
						} else {
							stage.add(responseType);
						}
					}
				} catch (NumberFormatException e) {
					throw new IOException("Invalid Resp format, expected B0XX but was " + type + ":" + line);
				}
			}
			return channel;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Channel readChannel() throws IOException {
		String B052F03 = "B052F03";
		String B052F04 = "B052F04";
		String B052F22 = "B052F22";
		String B052F23 = "B052F23";
		try {
			Map<String, String> pairs = new HashMap<>();
			int expectedNUmberOfLines = 4;

			int index = 0;
			while (index < expectedNUmberOfLines && lineIterator.hasNext()) {
				String line = lineIterator.next();
				if (line == null || line.startsWith("#")) {
					throw new IOException(line);
				}
				String[] array = line.split(":", 2);
				if (array.length < 2) {
					throw new IOException("Invalid Resp format, expected startDate but was empty");
				}
				String value = null;
				if (array.length == 2) {
					value = array[1];
				}
				array = array[0].split("\\s+");
				if (array.length < 1) {
					throw new IOException();
				}
				pairs.put(array[0], value);
				index++;
			}

			Channel channel = new Channel();
			String value = pairs.get(B052F03);
			if (value == null || value.trim().isEmpty()) {
				throw new IOException("Couldn't determine location");
			}
			value = value.trim();
			if ("??".equalsIgnoreCase(value)) {
				value = "";
			}
			channel.setLocationCode(value);
			value = extractValue(pairs.get(B052F04));
			if (value == null || value.trim().isEmpty()) {
				throw new IOException("Couldn't determine code");
			}
			channel.setCode(value);
			value = extractValue(pairs.get(B052F22));
			if (value.isEmpty()) {
				throw new IOException("Invalid Resp format, expected startDate but was empty");
			}
			ZonedDateTime start = parseDateTime(value);
			if (start == null) {
				throw new IOException("Expected a valid starttime but was null or empty");
			}
			channel.setStartDate(start);
			value = pairs.get(B052F23);
			if (value == null || value.trim().isEmpty()) {
				throw new IOException("Couldn't determine stage sequence number");
			}
			channel.setEndDate(parseDateTime(value));
			return channel;
		} catch (NumberFormatException e) {
			throw new IOException(e);
		}
	}

	static String extractValue(String value) throws IOException {
		if (value == null) {
			return null;
		}
		return value.trim();
	}

	static ZonedDateTime parseDateTime(String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		if (value.isEmpty()) {
			return null;
		}
		if ("no ending time".equalsIgnoreCase(value) || "null".equalsIgnoreCase(value)
				|| "undefined".equalsIgnoreCase(value) || "unknown".equalsIgnoreCase(value)) {
			return null;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy,DDD,HH:mm:ss").withZone(ZoneId.of("UTC"));
		return ZonedDateTime.parse(value, formatter);
	}

	@Override
	public void close() throws Exception {
		if (lineIterator != null) {
			lineIterator.close();
		}

	}
}
