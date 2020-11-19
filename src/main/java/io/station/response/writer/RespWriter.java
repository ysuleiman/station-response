package io.station.response.writer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.measure.Unit;

import io.station.model.CfTransferFunctionType;
import io.station.model.PzTransferFunctionType;
import io.station.model.Channel;
import io.station.model.Coefficients;
import io.station.model.Decimation;
import io.station.model.FDSNStationXML;
import io.station.model.FIR;
import io.station.model.FloatNoUnitType;
import io.station.model.Frequency;
import io.station.model.Gain;
import io.station.model.Network;
import io.station.model.PoleZero;
import io.station.model.PolesZeros;
import io.station.model.Polynomial;
import io.station.model.Response;
import io.station.model.ResponseList;
import io.station.model.ResponseListElement;
import io.station.model.ResponseStage;
import io.station.model.Station;
import io.station.model.Symmetry;
import io.station.model.Units;
import io.station.uom.Describable;
import io.station.model.Denominator;
import io.station.model.Numerator;
import io.station.model.Polynomial.Coefficient;
import io.station.response.writer.Box.BoxBuilder;

public class RespWriter extends PrintWriter {

	public RespWriter(OutputStream out) {
		super(out);
	}

	public void write(FDSNStationXML doc) {
		if (doc == null) {
			throw new IllegalStateException("FDSNStationXML is null");
		}
		write(doc.getNetwork());
	}

	public void write(List<Network> networks) {
		if (networks == null) {
			throw new IllegalStateException("networks is null");
		}
		for (Network n : networks) {
			write(n);
		}
	}

	public void write(Network network) {
		if (network == null) {
			throw new IllegalStateException("networks is null");
		}
		for (Station station : network.getStations()) {
			write(station);
		}
	}

	public void write(Station station) {
		if (station == null) {
			throw new IllegalStateException("Station cannot be null");
		}
		Network network = station.getNetwork();
		printField("B050F03", "Station", station.getCode());
		printField("B050F16", "Network", network.getCode());
		if (station.getChannels() != null) {
			for (Channel channel : station.getChannels()) {
				write(channel);
			}
		}
	}

	public void write(Channel channel) {
		if (channel == null) {
			throw new IllegalStateException("Channel cannot be null");
		}
		Station station = channel.getStation();
		printField("B050F03", "Station", station);
		printField("B050F16", "Network", (station == null ? null : station.getNetwork()));

		printField("B052F03", "Location", channel.getLocationCode());
		printField("B052F04", "Channel", channel.getCode());
		printField("B052F22", "Start date", channel.getStartDate());
		printField("B052F23", "End date", channel.getEndDate());

		Response response = channel.getResponse();
		if (response != null) {
			print(channel, response.getInstrumentSensitivity(), 0);
			print(channel, response.getInstrumentPolynomial(), 0);
			List<ResponseStage> stages = response.getStages();
			if (stages != null && !stages.isEmpty()) {
				for (ResponseStage r : stages) {
					write(channel, r);
				}
			}
		}
	}

	public void write(Channel channel, ResponseStage stage) {
		print(channel, stage.getPolesZeros(), stage.getNumber().intValue());
		print(channel, stage.getCoefficients(), stage.getNumber().intValue());
		print(channel, stage.getResponseList(), stage.getNumber().intValue());
		print(channel, stage.getDecimation(), stage.getNumber().intValue());
		print(channel, stage.getStageGain(), stage.getNumber().intValue());
		print(channel, stage.getFIR(), stage.getNumber().intValue());
		print(channel, stage.getPolynomial(), stage.getNumber().intValue());
	}

	/*-
	 * #
	#                  +-----------------------------------+
	#                  |    Response (Poles and Zeros)     |
	#                  |        IU  ANMO   00  BHZ         |
	#                  |     11/19/2002 to 06/30/2008      |
	#                  +-----------------------------------+
	#
	 */
	private void box(String title, Channel channel) {
		if (channel == null) {
			throw new IllegalStateException("Channel cannot be null");
		}
		BoxBuilder boxBuilder = Box.builder(40);

		boxBuilder.addLine(title);
		String channelIdentifierString = channel.getLocationCode() + "  " + channel.getCode();
		Station station = channel.getStation();
		if (station != null) {
			channelIdentifierString = station.getCode() + "  " + channelIdentifierString;
			Network network = station.getNetwork();
			if (network != null) {
				channelIdentifierString = network.getCode() + "  " + channelIdentifierString;
			}
		}
		StringBuilder dateBuilder = new StringBuilder();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
		dateBuilder
				.append(channel.getStartDate() == null ? "No Starting Time" : channel.getStartDate().format(formatter));
		dateBuilder.append(" to ");
		dateBuilder.append(channel.getEndDate() == null ? "No Ending Time" : channel.getEndDate().format(formatter));
		String box = boxBuilder.addLine(channelIdentifierString).addLine(dateBuilder.toString()).draw();
		StringReader reader = new StringReader(box);
		try (BufferedReader r = new BufferedReader(reader)) {
			int width = 84;
			int leftPadding = Math.floorDiv((width - 40 - 1), 2);
			String line = null;
			StringBuilder b = new StringBuilder();
			b.append('#').append(System.lineSeparator());
			while ((line = r.readLine()) != null) {
				if (line.length() > 0) {
					char[] kars = new char[84];
					Arrays.fill(kars, ' ');
					int i = 0;
					kars[i++] = '#';
					i = leftPadding;
					for (int x = 0; x < line.length(); i++, x++) {
						kars[i] = line.charAt(x);
					}
					b.append(new String(kars)).append(System.lineSeparator());
				}
			}
			b.append('#').append(System.lineSeparator());
			this.print(b.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void print(Channel channel, PolesZeros polesZeros, int stage) {
		if (polesZeros == null) {
			return;
		}
		this.box("Response (Poles and Zeros)", channel);
		String fType = "";
		if (PzTransferFunctionType.LAPLACE_RADIANS_SECOND == polesZeros.getPzTransferFunctionType()) {
			fType = "A";
		} else if (PzTransferFunctionType.LAPLACE_HERTZ == polesZeros.getPzTransferFunctionType()) {
			fType = "B";
		} else if (PzTransferFunctionType.DIGITAL_Z_TRANSFORM == polesZeros.getPzTransferFunctionType()) {
			fType = "D";
		} else {
			fType = "Undefined";
		}

		this.printField("B053F03", "Transfer function type:", fType);

		this.printField("B053F04", "Stage sequence number:", stage);
		this.printField("B053F05", "Response in units lookup:", polesZeros.getInputUnits());
		this.printField("B053F06", "Response out units lookup:", polesZeros.getOutputUnits());
		this.printField("B053F07", "A0 normalization factor:", polesZeros.getNormalizationFactor(), 5, "E", 2, true);

		// + formatScientific("0.00000E00", "+", polesZeros.getNormalizationFactor()));
		this.printField("B053F08", "Normalization frequency:", polesZeros.getNormalizationFrequency(), 5, "E", 2, true);
		this.printField("B053F09", "Number of zeroes:", polesZeros.getZero() == null ? 0 : polesZeros.getZero().size());
		this.printField("B053F14", "Number of poles:", polesZeros.getPole() == null ? 0 : polesZeros.getPole().size());

		this.printZeros(polesZeros.getPole());
		this.printPoles(polesZeros.getPole());
	}

	public void print(Channel channel, Coefficients coefficients, int stage) {
		if (coefficients == null) {
			return;
		}
		this.box("Response (Coefficients) ", channel);
		String fType = "";
		if (CfTransferFunctionType.ANALOG_RADIANS_SECOND.equals(coefficients.getCfTransferFunctionType())) {
			fType = "A";
		} else if (CfTransferFunctionType.ANALOG_HERTZ.equals(coefficients.getCfTransferFunctionType())) {
			fType = "B";
		} else if (CfTransferFunctionType.DIGITAL.equals(coefficients.getCfTransferFunctionType())) {
			fType = "D";
		} else {
			fType = "Undefined";
		}
		this.printField("B054F03", "Transfer function type:", fType);
		this.printField("B054F04", "Stage sequence number:", stage);
		this.printField("B054F05", "Response in units lookup:", coefficients.getInputUnits());
		this.printField("B054F06", "Response out units lookup:", coefficients.getOutputUnits());

		this.printField("B054F07", "Number of numerators:",
				coefficients.getNumerators() == null ? 0 : coefficients.getNumerators().size());
		this.printField("B054F10", "Number of denominators:",
				coefficients.getDenominators() == null ? 0 : coefficients.getDenominators().size());

		this.printNumerators(coefficients.getNumerators());
		this.printDenominators(coefficients.getDenominators());
	}

	public void print(Channel channel, ResponseList list, int stage) {
		if (list == null) {
			return;
		}
		this.box("Response (ResponseList) ", channel);
		this.printField("B055F03", "Stage sequence number:", stage);
		this.printField("B055F04", "Response in units lookup:", list.getInputUnits());
		this.printField("B055F05", "Response out units lookup:", list.getOutputUnits());

		this.printField("B054F07", "Number of responses listed:",
				list.getResponseListElement() == null ? 0 : list.getResponseListElement().size());

		this.format("%-15c%-3c%-14s%-14s%-14s%-14s%-14s", '#', 'i', "frequency", "amplitude", "amplitude err",
				"phase angle", "phase err").println();

		if (!list.getResponseListElement().isEmpty()) {
			for (ResponseListElement e : list.getResponseListElement()) {
				printField("B055F07-11", e);
			}
		}
	}

	public void print(Channel channel, Decimation decimation, int stage) {
		if (decimation == null) {
			return;
		}
		this.box("Decimation", channel);
		this.printField("B057F03", "Stage sequence number:", stage);
		this.printField("B057F04", "Input sample rate (HZ):",
				(format(decimation.getInputSampleRate() == null ? 0 : decimation.getInputSampleRate().getValue(), 5,
						"E", 2, true)));
		this.printField("B057F05", "Decimation factor:",
				decimation.getFactor() == null ? 0 : decimation.getFactor().intValue());
		this.printField("B057F06", "Decimation offset: ",
				decimation.getOffset() == null ? 0 : decimation.getOffset().intValue());
		this.printField("B057F07", "Estimated delay (seconds):",
				format(decimation.getDelay() == null ? 0 : decimation.getDelay().getValue(), 5, "E", 2, true));
		this.printField("B057F08", "Correction applied (seconds):", format(
				decimation.getCorrection() == null ? 0 : decimation.getCorrection().getValue(), 5, "E", 2, true));
	}

	public void print(Channel channel, Gain sensitivity, int stage) {
		if (sensitivity == null) {
			return;
		}
		this.box("Channel Sensitivity/Gain", channel);
		this.printField("B058F03", "Stage sequence number:", stage);
		this.printField("B058F04", "Sensitivity:",
				format(sensitivity.getValue() == null ? 0 : sensitivity.getValue(), 5, "E", 2, true));
		this.printField("B058F05", "Frequency of sensitivity:",
				format(sensitivity.getFrequency() == null ? 0 : sensitivity.getFrequency(), 5, "E", 2, true));
		this.printField("B058F06", "Number of calibrations:", 0);
	}

	public void print(Channel channel, FIR fir, int stage) {
		if (fir == null) {
			return;
		}
		this.box("Response (FIR) ", channel);
		this.printField("B061F03", "Stage sequence number:", stage);
		this.printField("B061F04", "Response Name:", fir.getName());

		String symmetry = "";
		if (Symmetry.EVEN==fir.getSymmetry()) {
			symmetry = "C";
		} else if (Symmetry.ODD==fir.getSymmetry()) {
			symmetry = "B";
		} else {
			symmetry = "A";
		}
		this.printField("B061F05", "Symmetry Code:", symmetry);

		this.printField("B061F06", "Response in units lookup:", fir.getInputUnits());
		this.printField("B061F07", "Response out units lookup:", fir.getOutputUnits());
		this.printField("B061F08", "Number of Coefficients:",
				fir.getNumerators() == null ? 0 : fir.getNumerators().size());
		this.format("%-15c%-3c%-14s", '#', 'i', "FIR Coefficient");

		int index = 1;
		for (Numerator n : fir.getNumerators()) {
			this.format("%-15s%-3d%-14s", "B061F09", index,
					format(n.getValue() == null ? 0 : n.getValue(), 5, "E", 2, true)).println();
			index++;
		}
	}

	public void print(Channel channel, Polynomial polynomial, int stage) {
		if (polynomial == null) {
			return;
		}
		this.box("Response (Polynomial) ", channel);
		this.printField("B062F03", "Transfer function type:", 'P');
		this.printField("B062F04", "Stage sequence number:", stage);
		this.printField("B062F05", "Response in units lookup:", polynomial.getInputUnits());
		this.printField("B062F06", "Response out units lookup:", polynomial.getOutputUnits());
		this.printField("B062F07", "Polynomial Approximation Type:", 'M');

		this.printField("B062F08", "Valid Frequency Units:", 'B');

		this.printField("B062F09", "Lower Valid Frequency Bound:", format(
				(polynomial.getFrequencyLowerBound() == null ? 0 : polynomial.getFrequencyLowerBound().getValue()), 5,
				"E", 2, true));
		this.printField("B062F10", "Upper Valid Frequency Bound:", format(
				(polynomial.getFrequencyUpperBound() == null ? 0 : polynomial.getFrequencyUpperBound().getValue()), 5,
				"E", 2, true));
		this.printField("B062F11", "Lower Bound of Approximation:",
				format((polynomial.getApproximationLowerBound() == null ? 0 : polynomial.getApproximationLowerBound()),
						5, "E", 2, true));
		this.printField("B062F12", "Upper Bound of Approximation:",
				format((polynomial.getApproximationUpperBound() == null ? 0 : polynomial.getApproximationUpperBound()),
						5, "E", 2, true));
		this.printField("B062F13", "Maximum Absolute Error:",
				format((polynomial.getMaximumError() == null ? 0 : polynomial.getMaximumError()), 5, "E", 2, true));

		this.printField("B062F14", "Number of Coefficients:",
				(polynomial.getCoefficient() == null ? 0 : polynomial.getCoefficient().size()));

		this.format("%-15c%-14s", '#', "Polynomial Coefficient").println();
		this.format("%-15c%-3c%-14s%-14s", '#', 'i', "coefficient", "error").println();

		if (polynomial.getCoefficient() != null) {
			for (Coefficient coefficient : polynomial.getCoefficient()) {
				this.format("%-15s%-3d%-14s%-14s", "B062F15-16", coefficient.getNumber().intValue(),
						format((coefficient.getValue() == null ? 0 : coefficient.getValue()), 5, "E", 2, true),
						format((coefficient.getMinusError() == null ? 0 : coefficient.getMinusError()), 5, "E", 2,
								true));
			}
		}
	}

	private void printZeros(List<PoleZero> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		this.println("#              Complex zeros:");
		this.format("%-15c%-3c%-14s%-14s%-14s%s", '#', 'i', "real", "imag", "real_error", "imag_error").println();
		for (PoleZero pz : list) {
			printField("B053F10-13", pz);
		}
	}

	private void printPoles(List<PoleZero> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		this.println("#              Complex poles:");
		this.format("%-15c%-3c%-14s%-14s%-14s%s", '#', 'i', "real", "imag", "real_error", "imag_error").println();
		for (PoleZero pz : list) {
			printField("B053F15-18", pz);
		}
	}

	private void printNumerators(List<Numerator> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		this.println("#              Numerator coefficients:");

		this.format("%-15c%-3c%-14s%-14s", '#', 'i', "coefficient", "error").println();
		int index = 1;
		for (Numerator n : list) {
			printField("B054F08-09", index, n);
			index++;
		}
	}

	private void printDenominators(List<Denominator> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		this.println("#              Denominator coefficients:");

		this.format("%-15c%-3c%-14s%-14s", '#', 'i', "coefficient", "error").println();
		int index = 1;
		for (Denominator d : list) {
			printField("B054F11-12", index, d);
			index++;
		}
	}

	private void printField(String key, PoleZero pz) {
		if (pz == null) {
			return;
		}
		this.format("%-15s%-3d%-14s%-14s%-14s%-12s", key, pz.getNumber() == null ? 0 : pz.getNumber().intValue(),
				format(pz.getRealType(), 5, "E", 2, true), format(pz.getImaginaryType(), 5, "E", 2, true),

				pz.getRealType() == null ? format(0d, 5, "E", 2, true)
						: format(pz.getRealType().getMinusError(), 5, "E", 2, true),

				pz.getImaginaryType() == null ? format(0d, 5, "E", 2, true)
						: format(pz.getImaginaryType().getMinusError(), 5, "E", 2, true))
				.println();
	}

	private void printField(String key, int index, FloatNoUnitType floatNoUnitType) {
		if (floatNoUnitType == null) {
			return;
		}

		this.format("%-15s%-3d%-14s%-14s", key, index, format(floatNoUnitType.getValue(), 5, "E", 2, true),
				format(floatNoUnitType.getMinusError(), 5, "E", 2, true)).println();
	}

	private void printField(String key, ResponseListElement e) {
		if (e == null) {
			return;
		}
		this.format("%-15c%-3c%-14s%-14s%-14s%-14s%-14s", key,
				format(e.getFrequency() == null ? 0 : e.getFrequency().getValue(), 5, "E", 2, true),
				format(e.getAmplitude() == null ? 0 : e.getAmplitude().getValue(), 5, "E", 2, true),
				format(e.getAmplitude() == null ? 0 : e.getAmplitude().getMinusError(), 5, "E", 2, true),
				format(e.getPhase() == null ? 0 : e.getPhase().getValue(), 5, "E", 2, true),
				format(e.getPhase() == null ? 0 : e.getPhase().getMinusError(), 5, "E", 2, true));
	}

	private String format(FloatNoUnitType fnut, int minimumFractionDigits, String exponentSeparator,
			int inimumExponentDigits, boolean alwaysShowSign) {
		double d = 0.0;
		if (fnut != null && fnut.getValue() != null) {
			fnut.getValue();
			d = fnut.getValue();
		}
		return format(d, minimumFractionDigits, exponentSeparator, inimumExponentDigits, alwaysShowSign);
	}

	private String format(Double d, int minimumFractionDigits, String exponentSeparator, int inimumExponentDigits,
			boolean alwaysShowSign) {
		if (d == null) {
			d = 0D;
		}
		RespNumberFormat f = new RespNumberFormat();
		return f.formatScientific(d == null ? 0 : d, minimumFractionDigits, exponentSeparator, inimumExponentDigits,
				alwaysShowSign);
	}

	private void printField(String key, String description, Frequency frequency, int minimumFractionDigits,
			String exponentSeparator, int inimumExponentDigits, boolean alwaysShowSign) {
		printField(key, description, frequency == null ? null : frequency.getValue(), minimumFractionDigits,
				exponentSeparator, inimumExponentDigits, alwaysShowSign);
	}

	private void printField(String key, String description, Double value, int minimumFractionDigits,
			String exponentSeparator, int inimumExponentDigits, boolean alwaysShowSign) {
		RespNumberFormat f = new RespNumberFormat();

		format("%-11s %-38s%-12s", key, description, f.formatScientific(value == null ? 0 : value,
				minimumFractionDigits, exponentSeparator, inimumExponentDigits, alwaysShowSign)).println();
	}

	private void printField(String key, String description, int value) {
		format("%-11s %-38s%d", key, description, value).println();
	}

	private void printField(String key, String description, ZonedDateTime value) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY,DDD,HH:MM:SS"/* "MM/dd/yyyy" */);
		format("%-11s %-11s %s", key, description, value == null ? "" : value.format(formatter)).println();
	}

	private void printField(String key, String description, Unit<?> units) {
		StringBuilder b = new StringBuilder();
		if (units != null) {
			if (units.getName() != null) {
				b.append(units.getName());
				if (units instanceof Describable) {
					String unitDescription = ((Describable) units).getDescription();
					if (unitDescription != null) {
						b.append(" - ").append(unitDescription);
					}
				}
			}
		}
		format("%-11s %-38s%s", key, description, b.toString()).println();
	}

	private void printField(String key, String description, Network network) {
		this.format("%-11s %-38s%s", key, description, network == null ? "" : network.getCode()).println();
	}

	private void printField(String key, String description, Station station) {
		this.format("%-11s %-38s%s", key, description, station == null ? "" : station.getCode()).println();
	}

	private void printField(String key, String description, String value) {
		this.format("%-11s %-38s%s", key, description, value == null ? "" : value).println();
	}

	public static void main(String[] args) {
		try (RespWriter r = new RespWriter(System.out);) {
			r.printField("B053F03", "Station", "ANMO");
		}
	}

	private static class RespNumberFormat extends DecimalFormat {

		String formatScientific(double num, int minimumFractionDigits, String exponentSeparator,
				int minimumExponentDigits, boolean alwaysShowSign) {

			DecimalFormat f = new DecimalFormat("0.######E00");
			DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
			symbols.setExponentSeparator(exponentSeparator);
			f.setDecimalFormatSymbols(symbols);
			f.setMinimumFractionDigits(minimumFractionDigits);
			if (alwaysShowSign) {
				f.setPositivePrefix("+");
			}
			String s = f.format(num);
			String result;
			final String expo = symbols.getExponentSeparator();
			final char minus = symbols.getMinusSign();
			if (!s.contains(expo + minus)) { // don't blast a negative sign
				result = s.replace(expo, expo + '+');
			} else {
				result = s;
			}
			return result;
		}

	}

}
