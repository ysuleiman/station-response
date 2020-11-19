package io.station.response;

import java.util.Objects;

import io.station.model.Frequency;

public class Normalized {

	private double value;
	private Frequency frequency;

	public Normalized(double value, Frequency frequency) {
		this.value = value;
		this.frequency = frequency;
	}

	public double getValue() {
		return value;
	}

	public Frequency getFrequency() {
		return frequency;
	}

	public void validate() {
		if(!Double.isFinite(value)) {
			throw new IllegalArgumentException(""+value);
		}
	}
	public int compare(Frequency f1, double f2) {
		Objects.requireNonNull(f1, "f1 cannot be null.");
		Objects.requireNonNull(f1.getValue(), "f1 value cannot be null.");
		return Double.compare(f1.getValue(), f2);
	}
}
