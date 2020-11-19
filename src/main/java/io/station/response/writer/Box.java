package io.station.response.writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Box {
	private int width;
	private List<String> lines = new ArrayList<>();

	private Box(int width) {
		this.width = width;
	}

	public static BoxBuilder builder(int width) {
		return new BoxBuilder(width);
	}

	public void addLine(String line) {
		lines.add(line);
	}

	public String draw() {
		int length = 0;
		for (String s : lines) {
			length = Math.max(length, s.length());
		}
		StringBuilder b = new StringBuilder();
		String top = doTop();
		b.append(top).append(System.lineSeparator());
		for (String s : lines) {
			b.append(doLine(s)).append(System.lineSeparator());
		}
		return b.append(top).append(System.lineSeparator()).toString();
	}

	private String doTop() {
		char[] kars = new char[width];
		Arrays.fill(kars, '-');
		kars[0] = '+';
		kars[width - 1] = '+';
		return new String(kars);
	}

	private String doLine(String text) {
		int leftPadding = Math.floorDiv(width - 2 - text.length(), 2);
		int rightPadding = width - 2 - text.length() - leftPadding;
		// int length = width + 2 + (2 * padding);
		char[] kars = new char[width];
		int i = 0;
		kars[i++] = '|';
		for (int x = 0; x < leftPadding; x++, i++) {
			kars[i] = ' ';
		}
		for (int x = 0; x < text.length(); x++, i++) {
			kars[i] = text.charAt(x);
		}
		for (int x = 0; x < rightPadding; x++, i++) {
			kars[i] = ' ';
		}
		kars[i++] = '|';
		// System.out.println()
		return new String(kars);
	}

	public static class BoxBuilder {
		private int width;
		private List<String> lines = new ArrayList<>();

		BoxBuilder(int width) {
			this.width = width;
		}

		public BoxBuilder addLine(String line) {
			lines.add(line);
			return this;
		}

		public String draw() {
			Box b = new Box(width);
			for (String line : lines) {
				b.addLine(line);
			}
			return b.draw();
		}
	}
}
