package io.station.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.junit.jupiter.api.Test;

import io.station.StationIO;
import io.station.StationMarshaller;
import io.station.model.Channel;
import io.station.model.FDSNStationXML;
import io.station.model.Network;
import io.station.model.Response;
import io.station.model.ResponseStage;
import io.station.model.Sensitivity;
import io.station.model.Station;
import io.station.response.RespIO;
import io.station.response.writer.RespWriter;
import io.station.util.ChannelIterator;

public class RespIOTest {

	@Test
	public void fromInputStream() throws Exception {
		try (InputStream inputStream = RespIOTest.class.getClassLoader().getResourceAsStream("IU.ANMO.resp");) {
			FDSNStationXML doc = RespIO.read(inputStream);
			StationMarshaller.marshal(doc, System.out);
		}
	}

	@Test
	public void iterateChannels() throws Exception {

		JAXBContext jaxbContext = JAXBContext.newInstance(io.station.model.ObjectFactory.class);
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		try (InputStream inputStream = RespIOTest.class.getClassLoader().getResourceAsStream("IU.ANMO.resp");
				ChannelIterator it = RespIO.iterateChannels(inputStream);) {
			assertTrue(it.hasNext());
			Channel channel = it.next();
			assertNotNull(channel);
			assertFalse(it.hasNext());

			assertEquals("BHZ", channel.getCode());
			assertEquals("00", channel.getLocationCode());
			channel.getStartDate();
			channel.getEndDate();

			Response response = channel.getResponse();
			assertNotNull(response);
			Sensitivity sensitivity=response.getInstrumentSensitivity();
			assertNotNull(sensitivity);
			List<ResponseStage>stages=response.getStages();
			assertNotNull(stages);
			assertEquals(6,stages.size());

		}
	}

	@Test
	public void write() throws Exception {
		try (InputStream inputStream = RespIOTest.class.getClassLoader().getResourceAsStream("IU.ANMO.BHZ.xml");) {
			FDSNStationXML doc = StationIO.read(inputStream);
			try (RespWriter resp = new RespWriter(System.out)) {
				Network n = doc.getNetwork().get(0);
				Station s = n.getStations().get(0);
				Channel c = s.getChannels().get(0);
				resp.write(c);
			}
		}
	}

	@Test
	public void writeDoc() throws Exception {
		try (InputStream inputStream = RespIOTest.class.getClassLoader().getResourceAsStream("IU.ANMO.BHZ.xml");) {
			FDSNStationXML doc = StationIO.read(inputStream);
			try (RespWriter resp = new RespWriter(System.out)) {
				resp.write(doc);
			}
		}
	}

}
