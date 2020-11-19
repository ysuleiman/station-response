package io.station.response.reader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.station.model.Sensitivity;

public class SensitivityReader extends AbstractRespReader {

	private static final String B058F03 = "B058F03";
	private static final String B058F04 = "B058F04";
	private static final String B058F05 = "B058F05";
	private static final String B058F06 = "B058F06";

	public Sensitivity read(Iterator<String> it) throws IOException {
		if (it == null) {
			throw new IllegalArgumentException("reader cannot be null");
		}

		Map<String, String> pairs = new HashMap<>();
		int expectedNUmberOfLines = 4;
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
		String value = pairs.get(B058F03);
		if (value == null || value.trim().isEmpty()) {
			throw new IOException("Couldn't determine stage sequence number");
		}
		try {
			this.stageSequenceNumber = Integer.parseInt(value.trim());
			Sensitivity sensitivity = build(pairs);
			if (sensitivity == null) {
				throw new IOException("Couldn't build Sensitivity - header");
			}

			value = pairs.get(B058F03);
			int numberOfcalibrations = (value == null ? 0 : Integer.parseInt(value.trim()));
			for (int i = 0; i < numberOfcalibrations;) {
				i++;
			}

			return sensitivity;
		} catch (NumberFormatException e) {
			throw new IOException(e);
		}
	}

	/*-
	B058F03     Stage sequence number:                 5
	B058F04     Sensitivity:                           +1.00000E+00
	B058F05     Frequency of sensitivity:              +0.00000E+00
	B058F06     Number of calibrations:                0
	*/
	private Sensitivity build(Map<String, String> lines) throws IOException {
		if (lines == null || lines.isEmpty()) {
			return null;
		}
		try {
			Sensitivity sensitivity = new Sensitivity();
			String value = lines.get(B058F04);
			if (value != null && !value.trim().isEmpty()) {
				sensitivity.setValue(Double.valueOf(value.trim()));
			}
			value = lines.get(B058F05);
			if (value != null && !value.trim().isEmpty()) {
				sensitivity.setFrequency(Double.valueOf(value.trim()));
			}
			value = lines.get(B058F06);
			if (value != null && value.trim().isEmpty()) {
				int numberOfCallibration = Integer.parseInt(value.trim());
			}
			return sensitivity;
		} catch (NumberFormatException e) {
			throw new IOException("", e);
		}
	}
}
