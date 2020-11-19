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
import io.station.model.FIR;
import io.station.model.PolesZeros;
import io.station.model.Response;
import io.station.model.ResponseStage;
import io.station.model.StageGain;
import io.station.model.Symmetry;
import io.station.response.RespFile;

public class FIRCoefficientTest {

	@Test
	public void odd() throws Exception {
		Path path = Paths.get(this.getClass().getClassLoader().getResource("1E.AXF.FIR.odd.resp").toURI());
		FDSNStationXML fdsnStationXML = RespFile.read(path);
		List<Channel> channels = fdsnStationXML.find("1E", "AXF", "  ", "BHE");
		assertNotNull(channels);
		assertFalse(channels.isEmpty());
		Channel channel = channels.get(0);
		Response response = channel.getResponse();
		ResponseStage responseStage = response.getStage(3);
		assertNotNull(responseStage);

		FIR fir = responseStage.getFIR();
		assertNotNull(fir);
		assertEquals(Symmetry.ODD,fir.getSymmetry());
		TransferFunction tf=TransferFunction.of(responseStage);
		assertTrue(tf instanceof FIRCoefficient);
		ComplexType ct = TransferFunction.of(responseStage).transform(1.0);
		//System.out.println(ct);
		assertEquals(responseStage.getStageGain().getValue(), ct.getReal(), 0.0001);

		ct = TransferFunction.of(responseStage).transform(1.0, 1.0);
		assertEquals(1.0, ct.getReal(), 0.0001);
	}
	
	@Test
	public void even() throws Exception {
		Path path = Paths.get(this.getClass().getClassLoader().getResource("3A.L001.HHE.FIR.even.resp").toURI());
		FDSNStationXML fdsnStationXML = RespFile.read(path);
		List<Channel> channels = fdsnStationXML.find("3A", "L001", "  ", "HHE");
		assertNotNull(channels);
		assertFalse(channels.isEmpty());
		Channel channel = channels.get(0);
		Response response = channel.getResponse();
		ResponseStage responseStage = response.getStage(4);
		assertNotNull(responseStage);

		FIR fir = responseStage.getFIR();
		assertNotNull(fir);
		assertEquals(Symmetry.EVEN,fir.getSymmetry());
		TransferFunction tf=TransferFunction.of(responseStage);
		assertTrue(tf instanceof FIRCoefficient);
		ComplexType ct = TransferFunction.of(responseStage).transform(1.0);
		//System.out.println(ct);
		//assertEquals(responseStage.getStageGain().getValue(), ct.getReal(), 0.0001);

		ct = TransferFunction.of(responseStage).transform(1.0, 1.0);
		assertEquals(1.0, ct.getReal(), 0.0001);
	}

}
