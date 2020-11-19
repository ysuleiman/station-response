package io.station.response.reader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.station.model.FIR;
import io.station.model.Numerator;
import io.station.model.Symmetry;

public class FIRReader extends AbstractRespReader {

	private static final String B061F03 = "B061F03";
	private static final String B061F04 = "B061F04";
	private static final String B061F05 = "B061F05";
	private static final String B061F06 = "B061F06";
	private static final String B061F07 = "B061F07";
	private static final String B061F08 = "B061F08";
	private static final String B061F09 = "B061F09";

	/*-
	 * 
	#
	#                  +-----------------------------------+
	#                  |           FIR Response            |
	#                  |        DK  MUD        BHE         |
	#                  |    12/04/2004 to No Ending Time   |
	#                  +-----------------------------------+
	#
	B061F03     Stage sequence number:                 4
	B061F04     Response Name:                         GFZ_DK1980_HDR24_FIR_1
	B061F05     Symmetry Code:                         C
	B061F06     Response in units lookup:              COUNTS - null
	B061F07     Response out units lookup:             COUNTS - null
	B061F08     Number of Coefficients:                17
	#              i  FIR Coefficient
	B061F09     0  +3.78878E-05
	B061F09     1  +1.99727E-04
	B061F09     2  +5.91277E-04
	B061F09     3  +1.19834E-03
	B061F09     4  +1.67720E-03
	B061F09     5  +1.23444E-03
	B061F09     6  -1.15877E-03
	B061F09     7  -6.07173E-03
	B061F09     8  -1.26102E-02
	B061F09     9  -1.76669E-02
	B061F09    10  -1.61537E-02
	B061F09    11  -2.63181E-03
	B061F09    12  +2.60166E-02
	B061F09    13  +6.80539E-02
	B061F09    14  +1.15986E-01
	B061F09    15  +1.58234E-01
	B061F09    16  +1.83050E-01
	 */
	public FIR read(Iterator<String> it) throws IOException {
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
			String value = pairs.get(B061F03);
			if (value == null || value.trim().isEmpty()) {
				throw new IOException("Couldn't determine stage sequence number");
			}
			this.stageSequenceNumber = Integer.parseInt(value.trim());
			FIR fir = build(pairs);
			if (fir == null) {
				throw new IOException("Couldn't build FIR - header");
			}
			value = pairs.get(B061F08);
			int numberOfCoefficients = (value == null ? 0 : Integer.parseInt(value.trim()));

			index = 0;
			while (index < numberOfCoefficients && it.hasNext()) {
				String line = it.next();
				if (line == null) {
					throw new IOException();
				}
				if (line.startsWith("#")) {
					continue;
				}
				Numerator numeratorCoefficient = parseNumeratorCoefficient(line);
				fir.getNumerators().add(numeratorCoefficient);
				index++;
			}

			return fir;
		} catch (NumberFormatException e) {
			throw new IOException(e);
		}
	}

	/*-
	B061F03     Stage sequence number:                 4
	B061F04     Response Name:                         GFZ_DK1980_HDR24_FIR_1
	B061F05     Symmetry Code:                         C
	B061F06     Response in units lookup:              COUNTS - null
	B061F07     Response out units lookup:             COUNTS - null
	B061F08     Number of Coefficients:                17
	*/
	private FIR build(Map<String, String> lines) {
		if (lines == null || lines.isEmpty()) {
			return null;
		}
		FIR fir = new FIR();

		String value = lines.get(B061F04);
		if (value != null) {
			fir.setName(value.trim());
		}
		value = lines.get(B061F05);
		if (value != null) {
			fir.setSymmetry(Symmetry.fromValue(value.trim().charAt(0)));
		}
		return fir;
	}

	/*-
	 * 	#              i  FIR Coefficient
	       B061F09     0  +3.78878E-05
	 * 
	 * @param line 
	 * @return Units
	 */

	private Numerator parseNumeratorCoefficient(String line) throws IOException {
		try {
			if (line == null) {
				return null;
			}
			if (!line.startsWith(B061F09)) {
				throw new IOException("FIR:Invalid numeratorCoefficient line" + line);
			}
			String[] array = line.split("\\s+");
			if (array.length != 3) {
				throw new IOException("FIR:Invalid numeratorCoefficient line" + line);
			}

			return Numerator.valueOf(Integer.parseInt(array[1]), Double.parseDouble(array[2]));
		} catch (NumberFormatException e) {
			throw new IOException("PolesZeros:Invalid zero line" + line, e);
		}
	}

}
