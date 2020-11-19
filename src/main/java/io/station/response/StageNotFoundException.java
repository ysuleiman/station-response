package io.station.response;

public class StageNotFoundException extends Exception{

	public StageNotFoundException(int stageNumber) {
		super(""+stageNumber);
	}
}
