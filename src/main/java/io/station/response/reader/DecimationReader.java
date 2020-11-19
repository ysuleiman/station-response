package io.station.response.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.station.model.Decimation;

public class DecimationReader extends AbstractRespReader {

	private static final String B057F03 = "B057F03";
	private static final String B057F04 = "B057F04";
	private static final String B057F05 = "B057F05";
	private static final String B057F06 = "B057F06";
	private static final String B057F07 = "B057F07";
	private static final String B057F08 = "B057F08";

	public Decimation read(Iterator<String> it) throws IOException {

		Map<String, String> pairs = new HashMap<>();
		int expectedNUmberOfLines = 6;

		int index = 0;
		while (index < expectedNUmberOfLines && it.hasNext()) {
			String line = it.next();
			if (line == null || line.startsWith("#")) {
				throw new IOException();
			}
			String[] array = line.split(":");
			if (array.length < 1) {
				throw new IOException();
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
		String value = pairs.get(B057F03);
		if (value == null || value.trim().isEmpty()) {
			throw new IOException("Couldn't determine stage sequence number");
		}
		try {
			this.stageSequenceNumber = Integer.parseInt(value.trim());
			Decimation decimation = build(pairs);
			if (decimation == null) {
				throw new IOException("Couldn't build Decimation - header");
			}
			return decimation;
		} catch (NumberFormatException e) {
			throw new IOException(e);
		}
	}

	public Decimation read(Reader reader) throws IOException {
		if (reader == null) {
			throw new IllegalArgumentException("reader cannot be null");
		}
		BufferedReader theReader = null;
		if (reader instanceof BufferedReader) {
			theReader = (BufferedReader) reader;
		} else {
			theReader = new BufferedReader(reader);
		}

		Map<String, String> pairs = new HashMap<>();
		int expectedNUmberOfLines = 6;
		for (int i = 0; i < expectedNUmberOfLines; i++) {
			String line = theReader.readLine();
			if (line == null) {
				throw new IOException();
			}
			String[] array = line.split(":");
			if (array.length < 1) {
				throw new IOException();
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
		}
		String value = pairs.get(B057F03);
		if (value == null || value.trim().isEmpty()) {
			throw new IOException("Couldn't determine stage sequence number");
		}
		this.stageSequenceNumber = Integer.parseInt(value.trim());
		Decimation decimation = build(pairs);
		if (decimation == null) {
			throw new IOException("Couldn't build Decimation - header");
		}
		return decimation;
	}

	/*-
	B057F03     Stage sequence number:                  6
	B057F04     Input sample rate (HZ):                 4.0000E+01
	B057F05     Decimation factor:                      00002
	B057F06     Decimation offset:                      00000
	B057F07     Estimated delay (seconds):             +7.8800E-01
	B057F08     Correction applied (seconds):          +7.6250E-01
	*/
	private Decimation build(Map<String, String> lines) throws IOException {
		if (lines == null || lines.isEmpty()) {
			return null;
		}
		try {
			Decimation decimation = new Decimation();

			String value = lines.get(B057F04);
			if (value != null && !value.trim().isEmpty()) {
				decimation.setInputSampleRate(super.parseFrequency(value.trim()));
			}
			value = lines.get(B057F05);
			if (value != null && !value.trim().isEmpty()) {
				decimation.setFactor(new BigInteger(value.trim()));
			}
			value = lines.get(B057F06);
			if (value != null && !value.trim().isEmpty()) {
				decimation.setOffset(new BigInteger(value.trim()));
			}

			value = lines.get(B057F07);
			if (value != null && !value.trim().isEmpty()) {
				decimation.setDelay(this.parseFloatType(value));
			}
			value = lines.get(B057F08);
			if (value != null && !value.trim().isEmpty()) {
				decimation.setCorrection(this.parseFloatType(value));
			}
			return decimation;
		} catch (NumberFormatException e) {
			throw new IOException("", e);
		}
	}
}
