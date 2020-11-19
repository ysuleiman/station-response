package io.station.response;

public enum FrequencySpacing {

	LINEAR("Linear",0), LOGARITHMIC("logarithmicc",1);

	private String name;
	private int intValue;

	FrequencySpacing(String name,int value) {
		this.name = name;
		this.intValue=value;
	}

	public String getName() {
		return name;
	}

	public int intValue() {
		return intValue;
	}
	
	public static FrequencySpacing from(int value) {
		if(value==0) {
			return LINEAR;
		}else if(value==1) {
			return LOGARITHMIC;
		}else {
			throw new IllegalArgumentException("Invalid FrequencySpacing value:"+value);
		}
	}
}
