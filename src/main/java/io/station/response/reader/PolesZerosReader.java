package io.station.response.reader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.station.model.PzTransferFunctionType;
import io.station.model.FloatNoUnitType;
import io.station.model.Frequency;
import io.station.model.PoleZero;
import io.station.model.PolesZeros;

public class PolesZerosReader extends AbstractRespReader {

	private static final String B053F03 = "B053F03";
	private static final String B053F04 = "B053F04";
	private static final String B053F05 = "B053F05";
	private static final String B053F06 = "B053F06";
	private static final String B053F07 = "B053F07";
	private static final String B053F08 = "B053F08";
	private static final String B053F09 = "B053F09";
	private static final String B053F14 = "B053F14";

	/*-
	B053F03     Transfer function type:                A [Laplace Transform (Rad/sec)]
	B053F04     Stage sequence number:                 1
	B053F05     Response in units lookup:              M/S - Velocity in Meters Per
	Second
	B053F06     Response out units lookup:             V - Volts
	B053F07     A0 normalization factor:               3948.58
	B053F08     Normalization frequency:               0.02
	B053F09     Number of zeroes:                      2
	B053F14     Number of poles:                       4
	#               Complex zeroes:
	#                 i  real          imag          real_error    imag_error
	B053F10-13    0  0.000000E+00  0.000000E+00  0.000000E+00  0.000000E+00
	B053F10-13    1  0.000000E+00  0.000000E+00  0.000000E+00  0.000000E+00
	#               Complex poles:
	#                 i  real          imag          real_error    imag_error
	B053F15-18    0 -1.234000E-02  1.234000E-02  0.000000E+00  0.000000E+00
	B053F15-18    1 -1.234000E-02 -1.234000E-02  0.000000E+00  0.000000E+00
	B053F15-18    2 -3.918000E+01  4.912000E+01  0.000000E+00  0.000000E+00
	B053F15-18    3 -3.918000E+01 -4.912000E+01  0.000000E+00  0.000000E+00
	 */
	public PolesZeros read(Iterator<String> it) throws IOException {
		if (it == null) {
			throw new IllegalArgumentException("reader cannot be null");
		}
		Map<String, String> pairs = new HashMap<>();
		int expectedNUmberOfLines = 8;
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
			String value = pairs.get(B053F04);
			if (value == null || value.trim().isEmpty()) {
				throw new IOException("Couldn't determine stage sequence number");
			}
			this.stageSequenceNumber = Integer.parseInt(value.trim());
			PolesZeros polesZeros = build(pairs);
			if (polesZeros == null) {
				throw new IOException("Couldn't build PolesZeros - header");
			}
			value = pairs.get(B053F09);
			int numberOfZeros = (value == null ? 0 : Integer.parseInt(value.trim()));

			index = 0;
			while (index < numberOfZeros && it.hasNext()) {
				String line = it.next();
				if (line == null) {
					throw new IOException();
				}
				if (line.startsWith("#")) {
					continue;
				}
				PoleZero zero = parseZero(line);
				if (zero != null) {
					polesZeros.getZero().add(zero);
				}
				index++;
			}
			value = pairs.get(B053F14);
			int numberOfPoles = (value == null ? 0 : Integer.parseInt(value.trim()));
			index = 0;
			while (index < numberOfPoles && it.hasNext()) {
				String line = it.next();
				if (line == null) {
					throw new IOException();
				}
				if (line.startsWith("#")) {
					continue;
				}
				PoleZero pole = parsePole(line);
				if (pole != null) {
					polesZeros.getPole().add(pole);
				}
				index++;
			}
			return polesZeros;
		} catch (NumberFormatException e) {
			throw new IOException(e);
		}
	}

	/*-
	B053F03     Transfer function type:                A [Laplace Transform (Rad/sec)]
	B053F04     Stage sequence number:                 1
	B053F05     Response in units lookup:              M/S - Velocity in Meters Per Second
	B053F06     Response out units lookup:             V - Volts
	B053F07     A0 normalization factor:               3948.58
	B053F08     Normalization frequency:               0.02
	B053F09     Number of zeroes:                      2
	B053F14     Number of poles:                       4
	*/
	private PolesZeros build(Map<String, String> lines) {
		if (lines == null || lines.isEmpty()) {
			return null;
		}
		PolesZeros polesZeros = new PolesZeros();
		String value = lines.get(B053F03);
		polesZeros.setPzTransferFunctionType(value == null ? null : PzTransferFunctionType.fromValue(value.trim().charAt(0)));

		polesZeros.setInputUnits(parseUnit(lines.get(B053F05)));
		polesZeros.setOutputUnits(parseUnit(lines.get(B053F06)));

		value = lines.get(B053F07);
		polesZeros.setNormalizationFactor(value == null ? null : Double.parseDouble(value.trim()));

		value = lines.get(B053F08);
		polesZeros.setNormalizationFrequency(value == null ? null : Frequency.valueOf(Double.parseDouble(value.trim())));

		return polesZeros;
	}

	/*-
	 * #              i  real          imag          real_error    imag_error
	 * B053F10-13     0  +0.00000E+00  +0.00000E+00  +0.00000E+00  +0.00000E+00
	 * 
	 * @param line 
	 * @return Units
	 */
	private PoleZero parseZero(String line) throws IOException {
		try {
			if (line == null) {
				return null;
			}
			if (!line.startsWith("B053F10-13")) {
				throw new IOException("PolesZeros:Invalid zero line" + line);
			}
			String[] array = line.split("\\s+");
			if (array.length != 6) {
				throw new IOException("PolesZeros:Invalid zero line" + line);
			}

			return PoleZero.valueOf(
					FloatNoUnitType.valueOf(Double.parseDouble(array[2]), Double.parseDouble(array[4]), 0, null),
					FloatNoUnitType.valueOf(Double.parseDouble(array[3]), Double.parseDouble(array[5]), 0, null));
		} catch (NumberFormatException e) {
			throw new IOException("PolesZeros:Invalid zero line" + line, e);
		}
	}

	/*-
	 * #              i  real          imag          real_error    imag_error
	 * B053F15-18     0  -4.80040E-03  +0.00000E+00  +0.00000E+00  +0.00000E+00
	 * 
	 * @param line 
	 * @return Units
	 */
	private PoleZero parsePole(String line) throws IOException {
		try {
			if (line == null) {
				return null;
			}
			if (!line.startsWith("B053F15-18")) {
				throw new IOException("PolesZeros:Invalid pole line" + line);
			}
			String[] array = line.split("\\s+");
			if (array.length != 6) {
				throw new IOException("PolesZeros:Invalid pole line" + line);
			}
			return PoleZero.valueOf(
					FloatNoUnitType.valueOf(Double.parseDouble(array[2]), Double.parseDouble(array[4]), 0, null),
					FloatNoUnitType.valueOf(Double.parseDouble(array[3]), Double.parseDouble(array[5]), 0, null));
		} catch (NumberFormatException e) {
			throw new IOException("PolesZeros:Invalid zero line" + line, e);
		}
	}
}
