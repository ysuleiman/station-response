package io.station.response;

public class EmptyStageException extends Exception{

	public EmptyStageException(int stageNumber) {
		super(""+stageNumber);
	}
}
