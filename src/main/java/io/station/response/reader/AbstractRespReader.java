package io.station.response.reader;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.measure.Unit;

import io.station.model.FloatType;
import io.station.model.Frequency;
import io.station.uom.StationUnitFormat;

public abstract class AbstractRespReader implements RespReader {

	protected int stageSequenceNumber;

	public AbstractRespReader() {

	}

	public int getStageSequenceNumber() {
		return this.stageSequenceNumber;
	}

	ZonedDateTime parseDateTime(String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		if (value.isEmpty()) {
			return null;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy,DDD,HH:mm:ss").withZone(ZoneId.of("UTC"));
		return ZonedDateTime.parse(value, formatter);
	}

	/**
	 * 
	 * @param line (M/S - Velocity in Meters Per Second)
	 * @return Units
	 */
	 Unit<?> parseUnit(String line) {
		if (line == null) {
			return null;
		}
		Unit<?> u = null;
		String[] array = line.trim().split("-");
		if (array.length > 0) {
			String name = array[0].trim();
			if (!name.isEmpty()) {
				return StationUnitFormat.getInstance().parse(name, (array.length > 1 ? array[1].trim() : null));
			}
		}
		return null;
	}

	int parseInt(String value) throws NumberFormatException {
		value = clean(value);
		if (value == null) {
			return 0;
		}
		return Integer.parseInt(value);
	}

	Double parseDouble(String value) throws NumberFormatException {
		value = clean(value);
		if (value == null) {
			return null;
		}
		return Double.parseDouble(value);
	}

	Frequency parseFrequency(String value) throws NumberFormatException {
		value = clean(value);
		if (value == null) {
			return null;
		}
		return Frequency.valueOf(Double.parseDouble(value));
	}

	FloatType parseFloatType(String value) throws NumberFormatException {
		value = clean(value);
		if (value == null) {
			return null;
		}
		return Frequency.valueOf(Double.parseDouble(value));
	}

	String clean(String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		if (value.isEmpty()) {
			return null;
		}
		return value;
	}
}
