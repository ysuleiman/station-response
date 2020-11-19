package io.station.response.reader;

import java.io.IOException;

public class ResponseTypeReaderFactory {

	public static RespReader create(int type) throws IOException {
		switch (type) {
		case 53:
			return new PolesZerosReader();
		case 54:
			return new CoefficientsReader();
		case 55:
			break;
		case 56:
			break;
		case 57:
			return new DecimationReader();
		case 58:
			return new SensitivityReader();
		case 60:
			break;
		case 61:
			return new FIRReader();
		case 62:
			return new PolynomialReader();
		default:
			throw new IOException("Expected a valid blockette type but was " + type);
		}
		return null;
	}
}
