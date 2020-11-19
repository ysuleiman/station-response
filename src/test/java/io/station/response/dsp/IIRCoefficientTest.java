package io.station.response.dsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.station.math.ComplexType;
import io.station.model.Channel;
import io.station.model.FDSNStationXML;
import io.station.model.PolesZeros;
import io.station.model.Response;
import io.station.model.ResponseStage;
import io.station.model.StageGain;
import io.station.response.RespFile;

public class IIRCoefficientTest {

	@Test
	public void iirCoefficientSeedExample() throws Exception {
		Path path = Paths.get(this.getClass().getClassLoader().getResource("seed-example.resp").toURI());
		FDSNStationXML fdsnStationXML = RespFile.read(path);
		List<Channel> channels = fdsnStationXML.find("IU", "ANMO", "00", "BHZ");
		assertNotNull(channels);
		assertFalse(channels.isEmpty());
		Channel channel = channels.get(0);
		Response response = channel.getResponse();
		ResponseStage responseStage = response.getStage(3);
		assertNotNull(responseStage);

		ComplexType ct = TransferFunction.of(responseStage).transform(1.0);
		assertEquals(responseStage.getStageGain().getValue(), ct.getReal(), 0.0001);

		ct = TransferFunction.of(responseStage).transform(1.0, 1.0);
		assertEquals(1.0, ct.getReal(), 0.0001);
	}

}
