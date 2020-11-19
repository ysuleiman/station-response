package io.station.response.util;

import java.util.Objects;

import io.station.response.FrequencySpacing;

public class FrequencySet {

	private final double min;
	private final double max;

	private final FrequencySpacing spacing;
	private final double[] values;

	public FrequencySet(double min, double max, FrequencySpacing spacing, double[] values) {
		this.min = min;
		this.max = max;
		this.spacing = spacing;
		this.values = values;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public FrequencySpacing getSpacing() {
		return spacing;
	}

	public double get(int index) {
		return values[index];
	}

	public double[] getValues() {
		return values;
	}

	public int size() {
		return values.length;
	}

	public static FrequencySet valueOf(double[] frequencyArray, FrequencySpacing spacing) {
		Objects.requireNonNull(frequencyArray, "frequencyArray cannot be null.");

		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (double frequency : frequencyArray) {
			if (frequency > max) {
				max = frequency;
			}

			if (frequency < min) {
				min = frequency;
			}
		}

		return new FrequencySet(min, max, spacing, frequencyArray);
	}
}
