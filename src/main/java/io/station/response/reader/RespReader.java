package io.station.response.reader;

import java.io.IOException;
import java.util.Iterator;

import io.station.model.ResponseType;

public interface RespReader {

	public int getStageSequenceNumber();
	public ResponseType read(Iterator<String> it)throws IOException;
}
