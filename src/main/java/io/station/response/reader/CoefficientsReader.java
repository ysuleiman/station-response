package io.station.response.reader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.station.model.CfTransferFunctionType;
import io.station.model.Coefficients;
import io.station.model.Denominator;
import io.station.model.Numerator;

public class CoefficientsReader extends AbstractRespReader {

	private static final String B054F03 = "B054F03";
	private static final String B054F04 = "B054F04";
	private static final String B054F05 = "B054F05";
	private static final String B054F06 = "B054F06";
	private static final String B054F07 = "B054F07";
	private static final String B054F10 = "B054F10";

	/*-
	B054F03     Transfer function type:                D
	B054F04     Stage sequence number:                 3
	B054F05     Response in units lookup:              counts - Digital Counts
	B054F06     Response out units lookup:             counts - Digital Counts
	B054F07     Number of numerators:                  64
	B054F10     Number of denominators:                0
	#              Numerator coefficients:
	#              i  coefficient   error
	B054F08-09     0  -1.09707E-03  +0.00000E+00
	B054F08-09     1  -9.93327E-04  +0.00000E+00
	B054F08-09     2  -1.33316E-03  +0.00000E+00
	 */
	public Coefficients read(Iterator<String> it) throws IOException {
		if (it == null) {
			throw new IllegalArgumentException("reader cannot be null");
		}

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
		try {
			String value = pairs.get(B054F04);
			if (value == null || value.trim().isEmpty()) {
				throw new IOException("Couldn't determine stage sequence number");
			}
			this.stageSequenceNumber = Integer.parseInt(value.trim());
			Coefficients coefficients = build(pairs);
			if (coefficients == null) {
				throw new IOException("Couldn't build Coefficients - header");
			}
			value = pairs.get(B054F07);
			int numberOfNumerators = (value == null ? 0 : parseInt(value));

			index = 0;
			while (index < numberOfNumerators && it.hasNext()) {
				String line = it.next();
				if (line == null || line.trim().isEmpty()) {
					throw new IOException();
				}
				if (line.startsWith("#")) {
					continue;
				}
				Numerator numerator = parseNumerator(line);
				if (numerator != null) {
					coefficients.getNumerators().add(numerator);
				}
				index++;
			}
			value = pairs.get(B054F10);
			int numberOfDenominators = (value == null ? 0 : parseInt(value));
			index = 0;
			while (index < numberOfDenominators && it.hasNext()) {
				String line = it.next();
				if (line == null || line.trim().isEmpty()) {
					throw new IOException();
				}
				if (line.startsWith("#")) {
					continue;
				}
				Denominator denominator = parseDenominator(line);
				if (denominator != null) {
					coefficients.getDenominators().add(denominator);
				}
				index++;
			}
			return coefficients;
		} catch (NumberFormatException e) {
			throw new IOException(e);
		}
	}

	/*-
	B054F03     Transfer function type:                D
	B054F04     Stage sequence number:                 3
	B054F05     Response in units lookup:              counts - Digital Counts
	B054F06     Response out units lookup:             counts - Digital Counts
	B054F07     Number of numerators:                  64
	B054F10     Number of denominators:                0
	*/
	private Coefficients build(Map<String, String> lines) throws IOException {
		if (lines == null || lines.isEmpty()) {
			return null;
		}
		Coefficients coefficients = new Coefficients();
		String value = clean(lines.get(B054F03));
		if (value == null) {
			throw new IOException("Expected B054F03 but found none");
		}
		char type = value.trim().charAt(0);

		coefficients.setCfTransferFunctionType(value == null ? null : CfTransferFunctionType.fromValue(type));

		coefficients.setInputUnits(parseUnit(lines.get(B054F05)));
		coefficients.setOutputUnits(parseUnit(lines.get(B054F06)));
		return coefficients;
	}

	/*-
	 *#              i  coefficient   error
	 *B054F08-09     0  -1.09707E-03  +0.00000E+00
	 * 
	 * @param line 
	 * @return Units
	 */
	private Numerator parseNumerator(String line) throws IOException {
		try {
			if (line == null) {
				return null;
			}
			if (!line.startsWith("B054F08")) {
				throw new IOException("Coefficients:Invalid numerator line" + line);
			}
			String[] array = line.split("\\s+");
			if (array.length != 4) {
				throw new IOException("Coefficients:Invalid numerator line" + line);
			}

			return Numerator.valueOf(Integer.parseInt(array[1]),parseDouble(array[2].trim()), parseDouble(array[3]), 0, null);
		} catch (NumberFormatException e) {
			throw new IOException("Coefficients:Invalid numerator line" + line, e);
		}
	}

	/*-
	 * #              i  real          imag          real_error    imag_error
	 * B053F15-18     0  -4.80040E-03  +0.00000E+00  +0.00000E+00  +0.00000E+00
	 * 
	 * @param line 
	 * @return Units
	 */
	private Denominator parseDenominator(String line) throws IOException {
		try {
			if (line == null) {
				return null;
			}
			line = line.trim();
			if (line.isEmpty()) {
				return null;
			}
			if (!line.startsWith("B053F10-13")) {
				throw new IOException("Coefficients:Invalid denominator line" + line);
			}
			String[] array = line.split("\\s+");
			if (array.length != 4) {
				throw new IOException("Coefficients:Invalid denominator line" + line);
			}

			return Denominator.valueOf(Integer.parseInt(array[1]),parseDouble(array[2]), parseDouble(array[3]), 0, null);
		} catch (NumberFormatException e) {
			throw new IOException("Coefficients:Invalid denominator line" + line, e);
		}
	}
}
