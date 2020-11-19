package io.station.response.reader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.station.model.Coefficients;
import io.station.model.Denominator;
import io.station.model.Numerator;
import io.station.model.Polynomial;
import io.station.model.Polynomial.Coefficient;

public class PolynomialReader extends AbstractRespReader {

	private static final String B062F03 = "B062F03";
	private static final String B062F04 = "B062F04";
	private static final String B062F05 = "B062F05";
	private static final String B062F06 = "B062F06";
	private static final String B062F07 = "B062F07";
	private static final String B062F08 = "B062F08";
	private static final String B062F09 = "B062F09";
	private static final String B062F10 = "B062F10";
	private static final String B062F11 = "B062F11";
	private static final String B062F12 = "B062F12";
	private static final String B062F13 = "B062F13";
	private static final String B062F14 = "B062F14";

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
	public Polynomial read(Iterator<String> it) throws IOException {
		if (it == null) {
			throw new IllegalArgumentException("reader cannot be null");
		}
		
		Map<String, String> pairs = new HashMap<>();
		int expectedNUmberOfLines = 8;
		int index = 0;
		while (index < expectedNUmberOfLines && it.hasNext()) {
			String line = it.next();
			if (line == null||line.startsWith("#")) {
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
		String value = pairs.get(B062F04);
		if (value == null || value.trim().isEmpty()) {
			throw new IOException("Couldn't determine stage sequence number");
		}
		this.stageSequenceNumber = Integer.parseInt(value.trim());
		Polynomial polynomial = build(pairs);
		if (polynomial == null) {
			throw new IOException("Couldn't build Polynomial - header");
		}
		value = pairs.get(B062F14);
		int numberOfCefficients = (value == null ? 0 : Integer.parseInt(value));
		index = 0;
		while (index < numberOfCefficients && it.hasNext()) {
			String line = it.next();
			if (line == null||line.startsWith("#")) {
				throw new IOException();
			}
			String[] array = line.split("\\s+");
			if (array.length != 4) {

			}
			// B062F15-16 i, coefficient, error
			Coefficient coefficient = Coefficient.valueOf(Double.parseDouble(array[2]), Double.parseDouble(array[3]),
					Double.parseDouble(array[3]), null);
			if (coefficient != null) {
				polynomial.getCoefficient().add(coefficient);
			}
			index++;
		}
		return polynomial;
	}

	/*-
	B054F03     Transfer function type:                D
	B054F04     Stage sequence number:                 3
	B054F05     Response in units lookup:              counts - Digital Counts
	B054F06     Response out units lookup:             counts - Digital Counts
	B054F07     Number of numerators:                  64
	B054F10     Number of denominators:                0
	*/
	private Polynomial build(Map<String, String> lines) {
		if (lines == null || lines.isEmpty()) {
			return null;
		}
		Polynomial polynomial = new Polynomial();

		// B062F03 Transfer function type: P");
		// out.println("B062F04 Stage sequence number: " + stage);

		polynomial.setInputUnits(parseUnit(lines.get(B062F05)));
		polynomial.setOutputUnits(parseUnit(lines.get(B062F06)));
		polynomial.setApproximationType(lines.get(B062F07));
		lines.get(B062F08);// valid frequency unit

		polynomial.setFrequencyLowerBound(parseFrequency(lines.get(B062F09)));
		polynomial.setFrequencyUpperBound(parseFrequency(lines.get(B062F10)));
		polynomial.setApproximationLowerBound(parseDouble(lines.get(B062F11)));
		polynomial.setApproximationUpperBound(parseDouble(lines.get(B062F12)));
		polynomial.setMaximumError(parseDouble(lines.get(B062F13)));
		return polynomial;
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
			if (!line.startsWith("B053F10-13")) {
				throw new IOException("Coefficients:Invalid numerator line" + line);
			}
			String[] array = line.split("\\s+");
			if (array.length != 6) {
				throw new IOException("Coefficients:Invalid numerator line" + line);
			}

			return Numerator.valueOf(Double.parseDouble(array[2]), Double.parseDouble(array[3]), 0, null);
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
			if (!line.startsWith("B053F10-13")) {
				throw new IOException("Coefficients:Invalid denominator line" + line);
			}
			String[] array = line.split("\\s+");
			if (array.length != 6) {
				throw new IOException("Coefficients:Invalid denominator line" + line);
			}

			return Denominator.valueOf(Double.parseDouble(array[2]), Double.parseDouble(array[3]), 0, null);
		} catch (NumberFormatException e) {
			throw new IOException("Coefficients:Invalid denominator line" + line, e);
		}
	}
}
