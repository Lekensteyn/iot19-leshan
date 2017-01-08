package group19;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RGBColor {

	final private static Pattern rgbPattern = Pattern.compile("\\((\\d+), *(\\d+), *(\\d+)\\)");

	final public int r, g, b;

	/**
	 * @param color
	 *            A color in the format {@code (r, g, b)}, for example
	 *            {@code (255, 255, 0)}.
	 */
	public RGBColor(String color) {
		Matcher matcher = rgbPattern.matcher(color);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Unrecognized color");
		}

		r = parseColorComponent(matcher.group(1));
		g = parseColorComponent(matcher.group(2));
		b = parseColorComponent(matcher.group(3));
	}

	public RGBColor(int r, int g, int b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	private int parseColorComponent(String val) {
		int num = Integer.valueOf(val);
		if (num > 255) {
			throw new IllegalArgumentException("Invalid color component");
		}
		return num;
	}

	@Override
	public String toString() {
		return String.format("(%d, %d, %d)", r, g, b);
	}
}
