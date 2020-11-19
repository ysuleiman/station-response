package io.station.response;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.station.model.Channel;
import io.station.model.FDSNStationXML;
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
import io.station.response.util.RespChannelReader;
import io.station.time.DateTimeUtil;
import io.station.util.ChannelIterator;
import io.station.util.RewindableLineIterator;

public class RespIO {

	public static FDSNStationXML read(String body) throws IOException {
		try (InputStream inputStream = new ByteArrayInputStream(body.getBytes());) {
			return read(inputStream);
		}
	}

	public static ChannelIterator iterateChannels(InputStream inputStream) throws IOException {
		ChannelIterator channelIterator = new ChannelIterator(new RespChannelReader(inputStream));
		return channelIterator;
	}

	/*-
	 * 	B050F03     Station:     ANMO
		B050F16     Network:     IU
		B052F03     Location:    00
		B052F04     Channel:     BHZ
		B052F22     Start date:  2002,323,21:07:00
		B052F23     End date:    2008,182,00:00:00
	 */
	public static FDSNStationXML read(InputStream inputStream) throws IOException {

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				RewindableLineIterator it = new RewindableLineIterator(reader);) {
			FDSNStationXML fdsnStationXML = new FDSNStationXML();
			fdsnStationXML.setCreated(ZonedDateTime.now());
			fdsnStationXML.setModule("");
			fdsnStationXML.setModuleURI("");
			// fdsnStationXML.setSchemaVersion("");
			fdsnStationXML.setSender("");
			fdsnStationXML.setSource("");

			String line = null;// 2002,323,21:07:00
			Network network = null;
			Station station = null;
			Channel channel = null;

			while (it.hasNext()) {
				line = it.next();
				if (line == null) {
					throw new IOException("Unexpected error!");
				}
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("B050F03")) {
					String[] array = line.split("\\s+");
					if (array.length < 3) {
						throw new IOException(
								"Invalid Resp format, expected to find a station code at index 2 but found none");
					}
					String stationCode = array[2];
					if (station == null || !stationCode.equals(station.getCode())) {
						station = new Station();
						station.setCode(stationCode);
					}
					continue;
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
						fdsnStationXML.getNetwork().add(network);
					}
					continue;
				}

				if (line.startsWith("B052")) {
					it.rewind();
					Channel temp = readChannel(it);
					if (temp == null) {
						throw new IOException(
								"Error reading channel information, expected a channel but received null");
					}
					if (!temp.equals(channel)) {
						channel = temp;
						station.addChannel(channel);
					}
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
					it.rewind();
					type = Integer.parseInt(value);
					RespReader respReader = ResponseTypeReaderFactory.create(type);
					ResponseType responseType = respReader.read(it);
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
			return fdsnStationXML;
		}
	}

	public static void write(FDSNStationXML document, OutputStream outputStream) throws IOException {

	}

	private static Channel readChannel(Iterator<String> it) throws IOException {
		String B052F03 = "B052F03";
		String B052F04 = "B052F04";
		String B052F22 = "B052F22";
		String B052F23 = "B052F23";
		try {
			Map<String, String> pairs = new HashMap<>();
			int expectedNUmberOfLines = 4;

			int index = 0;
			while (index < expectedNUmberOfLines && it.hasNext()) {
				String line = it.next();
				if (line == null || line.startsWith("#")) {
					throw new IOException();
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
		return DateTimeUtil.parseAny(value);
		//DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy,DDD,HH:mm:ss").withZone(ZoneId.of("UTC"));
		//return ZonedDateTime.parse(value, formatter);
	}
}
