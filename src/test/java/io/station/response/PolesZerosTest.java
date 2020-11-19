package io.station.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.math3.complex.Complex;
import org.junit.jupiter.api.Test;

import io.station.model.Channel;
import io.station.model.FDSNStationXML;
import io.station.model.Frequency;
import io.station.model.PoleZero;
import io.station.model.PolesZeros;
import io.station.model.Response;
import io.station.model.ResponseStage;

public class PolesZerosTest {

	@Test
	public void analogTransFromResp() throws Exception {

		Path path = Paths.get(this.getClass().getClassLoader().getResource("iu.anmo.bhz.one.epoch.resp").toURI());
		FDSNStationXML fdsnStationXML = RespFile.read(path);

		List<Channel> channels = fdsnStationXML.find("IU", "ANMO", "00", "BHZ");
		assertNotNull(channels);
		assertEquals(1, channels.size());

		Channel channel = channels.get(0);
		Response response = channel.getResponse();
		assertNotNull(response);

		ResponseStage stage = response.getStage(1);
		assertNotNull(stage);

		PolesZeros polesZeros = stage.getPolesZeros();
		assertNotNull(polesZeros);

		List<PoleZero> poles = polesZeros.getPole();
		List<PoleZero> zeros = polesZeros.getZero();

		assertNotNull(polesZeros.getNormalizationFactor());
		double normalizationFactor = polesZeros.getNormalizationFactor();
		assertEquals(+5.51178E-20, normalizationFactor);
		Frequency normalizationFrequency = polesZeros.getNormalizationFrequency();
		assertNotNull(normalizationFrequency);
		assertEquals(+2.00000E-02, normalizationFrequency.getValue());

		PoleZero total = null;
		for (PoleZero pz : poles) {
			if (total == null) {
				total = pz;
			} else {
				total = pz.multiply(total);
			}
		}
		assertEquals(pole().getReal(), total.getReal());
		assertEquals(pole().getImaginary(), total.getImaginary());
		
		total = null;
		for (PoleZero pz : zeros) {
			if (total == null) {
				total = pz;
			} else {
				total = pz.multiply(total);
			}
		}

		
		
		assertEquals(zero().getReal(), total.getReal());
		assertEquals(zero().getImaginary(), total.getImaginary());
	}

	private static Complex pole() {
		return new Complex(-1.23105E-02, -1.24009E-02).multiply(new Complex(-1.23105E-02, +1.24009E-02))
				.multiply(new Complex(-4.30268E+00, +0.00000E+00)).multiply(new Complex(-4.30268E+00, +0.00000E+00))
				.multiply(new Complex(-3.53203E+02, +0.00000E+00)).multiply(new Complex(-4.84230E+02, -4.73792E+02))
				.multiply(new Complex(-4.84230E+02, +4.73792E+02));
	}

	private static Complex zero() {
		return new Complex(+0.00000E+00, +0.00000E+00).multiply(new Complex(+0.00000E+00, +0.00000E+00))
				.multiply(new Complex(-4.15782E+00, +0.00000E+00)).multiply(new Complex(-4.15782E+00, +0.00000E+00))
				.multiply(new Complex(-4.95793E+06, +0.00000E+00)).multiply(new Complex(-6.48766E-01, -7.73567E+06))
				.multiply(new Complex(-6.48766E-01, +7.73567E+06)).multiply(new Complex(-1.06157E+07, +0.00000E+00));
	}
}
