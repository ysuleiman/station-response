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

public class TransferFunctionTest {

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
	
	@Test
	public void laplacePolesZerosSeedExample() throws Exception {
		Path path = Paths.get(this.getClass().getClassLoader().getResource("seed-example.resp").toURI());
		FDSNStationXML fdsnStationXML = RespFile.read(path);
		List<Channel> channels = fdsnStationXML.find("IU", "ANMO", "00", "BHZ");
		assertNotNull(channels);
		assertFalse(channels.isEmpty());
		Channel channel = channels.get(0);
		Response response = channel.getResponse();
		ResponseStage responseStage = response.getStage(1);
		assertNotNull(responseStage);

		ComplexType ct =  TransferFunction.of(responseStage).transform(1.0, 1.0);
		assertEquals(responseStage.getPolesZeros().getNormalizationFactor(), 1 / ct.getReal(), 0.0000000001);
		assertEquals(0.7853933052538834, ct.getImaginary(), 0.0000000001);
	}

	@Test
	public void iirPolesZerosSeedExample() throws Exception {
		Path path = Paths.get(this.getClass().getClassLoader().getResource("seed-example.resp").toURI());
		FDSNStationXML fdsnStationXML = RespFile.read(path);
		List<Channel> channels = fdsnStationXML.find("IU", "ANMO", "00", "BHZ");
		assertNotNull(channels);
		assertFalse(channels.isEmpty());
		Channel channel = channels.get(0);
		Response response = channel.getResponse();
		ResponseStage responseStage = response.getStage(1);
		assertNotNull(responseStage);

		ComplexType ct =  TransferFunction.of(responseStage).transform(1.0, 1.0);
		assertEquals(responseStage.getPolesZeros().getNormalizationFactor(), 1 / ct.getReal(), 0.0000000001);
		assertEquals(0.7853933052538834, ct.getImaginary(), 0.0000000001);
	}
	
	@Test
	public void iirPolesZeros() throws Exception {
		Path path = Paths.get(this.getClass().getClassLoader().getResource("digital.pz.OO.AXCC2.MNZ.resp").toURI());
		FDSNStationXML fdsnStationXML = RespFile.read(path);
		List<Channel> channels = fdsnStationXML.find("OO", "AXCC2", "  ", "MNZ");
		assertNotNull(channels);
		assertFalse(channels.isEmpty());
		Channel channel = channels.get(0);
		Response response = channel.getResponse();
		ResponseStage responseStage = response.getStage(1);
		assertNotNull(responseStage);
		TransferFunction transferFunction = TransferFunction.of(responseStage);
		ComplexType ct = transferFunction.transform(1.0, 1.0);
		System.out.println(ct.getReal());
		assertEquals(responseStage.getPolesZeros().getNormalizationFactor(), 1 / ct.getReal(), 0.0000000001);
		assertEquals(-1.057208229564565E-4, ct.getImaginary(), 0.0000000001);
		PolesZeros polesZeros = responseStage.getPolesZeros();
		assertNotNull(polesZeros);
		StageGain stageGain = responseStage.getStageGain();
		assertNotNull(stageGain);

		
		//assertEquals(polesZeros.getNormalizationFactor(), 1/tf.transform(1.0, 1.0),0.0000000001);
		//assertEquals(-0.802722257, tf.phase(1.0),0.00000001);
	}
	
	@Test
	public void laplacePolesZeros() throws Exception {
		Path path = Paths.get(this.getClass().getClassLoader().getResource("iu.anmo.bhz.one.epoch.resp").toURI());
		FDSNStationXML fdsnStationXML = RespFile.read(path);
		List<Channel> channels = fdsnStationXML.find("IU", "ANMO", "00", "BHZ");
		assertNotNull(channels);
		assertFalse(channels.isEmpty());
		Channel channel = channels.get(0);
		Response response = channel.getResponse();
		ResponseStage responseStage = response.getStage(1);
		assertNotNull(responseStage);

		TransferFunction transferFunction = TransferFunction.of(responseStage);
		ComplexType ct = transferFunction.transform(1.0, 1.0);
		assertEquals(responseStage.getPolesZeros().getNormalizationFactor(), 1 / ct.getReal(), 0.0000000001);
		assertEquals(-1.057208229564565E-4, ct.getImaginary(), 0.0000000001);
	}
	
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
