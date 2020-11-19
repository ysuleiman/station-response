package io.station.response.dsp;

import io.station.model.PolesZeros;
import io.station.model.StageGain;

public class PolesZerosIIR {

	private PolesZeros polesZeros;
	public PolesZerosIIR(PolesZeros polesZeros) {
		this.polesZeros=polesZeros;
		//gain
		//normalizationFactor
	}
	
	public static PolesZerosIIRBuilder builder(PolesZeros polesZeros) {
		return new PolesZerosIIRBuilder(polesZeros);
	}
	public static class PolesZerosIIRBuilder{
		PolesZeros polesZeros;
		private StageGain stageGain;
		
		PolesZerosIIRBuilder(PolesZeros polesZeros){
			this.polesZeros=polesZeros;
		}
		PolesZerosIIRBuilder gain(StageGain stageGain) {
			return this;
		}
	}
}
