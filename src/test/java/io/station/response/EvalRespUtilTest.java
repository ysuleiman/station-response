package io.station.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.math3.complex.Complex;
import org.junit.jupiter.api.Test;

import io.station.model.CfTransferFunctionType;
import io.station.model.Coefficients;
import io.station.model.FDSNStationXML;
import io.station.model.FloatNoUnitType;
import io.station.model.Numerator;
import io.station.model.PoleZero;
import io.station.model.PolesZeros;
import io.station.model.PzTransferFunctionType;
import io.station.model.Symmetry;
import io.station.uom.StationUnits;

public class EvalRespUtilTest {

	@Test
	public void analogTrans() {
		PolesZeros polesZeros = createPolesZeros();

		Complex complex = EvalRespUtil.analogTrans(polesZeros, +5.51178E-20, 0.1);
		assertNotNull(complex);
		assertEquals(0.968740976046097, complex.getReal(), 0.0000001);
		assertEquals(0.2471855389775642, complex.getImaginary(), 0.0000001);
	}

	@Test
	public void analogTransFromResp() {
		PolesZeros polesZeros = createPolesZerosFromResp();

		Complex complex = EvalRespUtil.analogTrans(polesZeros, +9.19836E-01, 0.1);
		assertNotNull(complex);
		assertEquals(-2.876508163900839E-4, complex.getReal(), 0.0000001);
		assertEquals(5.088368482030121E-6, complex.getImaginary(), 0.0000001);
	}

	@Test
	public void iirTrans() {
		Complex complex = EvalRespUtil.iirTrans(createCoefficients(), +5.51178E-20, 0.025, 0.12566370614359174);
		assertNotNull(complex);
		assertEquals(5.511699294810929E-20, complex.getReal(), 0.0000001);
		assertEquals(-2.959504661672211E-21, complex.getImaginary(), 0.0000001);
	}

	@Test
	public void firTransEvenODD() {
		Complex complex = EvalRespUtil.firTrans(createCoefficients(), +5.51178E-20, 0.025, 0.1291549665014884,
				Symmetry.ODD);
		assertNotNull(complex);
		assertEquals(1.130775906501618E-19, complex.getReal(), 0.0000001);
		assertEquals(0, complex.getImaginary(), 0.0000001);
	}

	@Test
	public void firTransEVEN() {
		Complex complex = EvalRespUtil.firTrans(createCoefficients(), +5.51178E-20, 0.025, 0.1291549665014884,
				Symmetry.EVEN);
		assertNotNull(complex);
		assertEquals(1.2226628230024264E-19, complex.getReal(), 0.0000001);
		assertEquals(0, complex.getImaginary(), 0.0000001);
	}

	@Test
	public void firTransNONE() {
		Complex complex = EvalRespUtil.firTrans(createCoefficients(), +5.51178E-20, 0.025, 0.1291549665014884,
				Symmetry.NONE);
		assertNotNull(complex);
		assertEquals(5.511699294810929E-20, complex.getReal(), 0.00000000001);
		assertEquals(3.3822097796610406E-22, complex.getImaginary(), 0.00000000001);
	}

	/*-
	 * B053F03     Transfer function type:                A
	B053F04     Stage sequence number:                 1
	B053F05     Response in units lookup:              m/s - Velocity in Meters Per Second
	B053F06     Response out units lookup:             V - Volts
	B053F07     A0 normalization factor:               +5.51178E-20
	B053F08     Normalization frequency:               +2.00000E-02
	B053F09     Number of zeroes:                      8
	B053F14     Number of poles:                       7
	#              Complex zeroes:
	#              i  real          imag          real_error    imag_error
	B053F10-13     0  +0.00000E+00  +0.00000E+00  +0.00000E+00  +0.00000E+00
	B053F10-13     1  +0.00000E+00  +0.00000E+00  +0.00000E+00  +0.00000E+00
	B053F10-13     2  -4.15782E+00  +0.00000E+00  +0.00000E+00  +0.00000E+00
	B053F10-13     3  -4.15782E+00  +0.00000E+00  +0.00000E+00  +0.00000E+00
	B053F10-13     4  -4.95793E+06  +0.00000E+00  +0.00000E+00  +0.00000E+00
	B053F10-13     5  -6.48766E-01  -7.73567E+06  +0.00000E+00  +0.00000E+00
	B053F10-13     6  -6.48766E-01  +7.73567E+06  +0.00000E+00  +0.00000E+00
	B053F10-13     7  -1.06157E+07  +0.00000E+00  +0.00000E+00  +0.00000E+00
	#              Complex poles:
	#              i  real          imag          real_error    imag_error
	B053F15-18     0  -1.23105E-02  -1.24009E-02  +0.00000E+00  +0.00000E+00
	B053F15-18     1  -1.23105E-02  +1.24009E-02  +0.00000E+00  +0.00000E+00
	B053F15-18     2  -4.30268E+00  +0.00000E+00  +0.00000E+00  +0.00000E+00
	B053F15-18     3  -4.30268E+00  +0.00000E+00  +0.00000E+00  +0.00000E+00
	B053F15-18     4  -3.53203E+02  +0.00000E+00  +0.00000E+00  +0.00000E+00
	B053F15-18     5  -4.84230E+02  -4.73792E+02  +0.00000E+00  +0.00000E+00
	B053F15-18     6  -4.84230E+02  +4.73792E+02  +0.00000E+00  +0.00000E+00
	 */
	PolesZeros createPolesZeros() {
		PolesZeros polesZeros = new PolesZeros();
		polesZeros.setPzTransferFunctionType(PzTransferFunctionType.DIGITAL_Z_TRANSFORM);

		polesZeros.getZero().add(createPoleZero(0, +0.00000E+00, +0.00000E+00));
		polesZeros.getZero().add(createPoleZero(1, +0.00000E+00, +0.00000E+00));
		polesZeros.getZero().add(createPoleZero(2, -4.15782E+00, +0.00000E+00));
		polesZeros.getZero().add(createPoleZero(3, -4.15782E+00, +0.00000E+00));
		polesZeros.getZero().add(createPoleZero(4, -4.95793E+06, +0.00000E+00));
		polesZeros.getZero().add(createPoleZero(5, -6.48766E-01, -7.73567E+06));
		polesZeros.getZero().add(createPoleZero(6, -6.48766E-01, +7.73567E+06));
		polesZeros.getZero().add(createPoleZero(7, -1.06157E+07, +0.00000E+00));

		polesZeros.getPole().add(createPoleZero(0, -1.23105E-02, -1.24009E-02));
		polesZeros.getPole().add(createPoleZero(1, -1.23105E-02, +1.24009E-02));
		polesZeros.getPole().add(createPoleZero(2, -4.30268E+00, +0.00000E+00));
		polesZeros.getPole().add(createPoleZero(3, -4.30268E+00, +0.00000E+00));
		polesZeros.getPole().add(createPoleZero(4, -3.53203E+02, +0.00000E+00));
		polesZeros.getPole().add(createPoleZero(5, -4.84230E+02, -4.73792E+02));
		polesZeros.getPole().add(createPoleZero(6, -4.84230E+02, +4.73792E+02));
		return polesZeros;
	}

	PolesZeros createPolesZerosFromResp() {
		PolesZeros polesZeros = new PolesZeros();
		polesZeros.setPzTransferFunctionType(PzTransferFunctionType.LAPLACE_RADIANS_SECOND);

		polesZeros.getZero().add(createPoleZero(0, +0.00000E+00, +0.00000E+00));
		polesZeros.getZero().add(createPoleZero(1, +0.00000E+00, +0.00000E+00));

		polesZeros.getPole().add(createPoleZero(0, -2.82743E+00, +4.89726E+00));
		polesZeros.getPole().add(createPoleZero(1, -2.82743E+00, -4.89726E+00));
		return polesZeros;
	}

	PoleZero createPoleZero(int sequence, double real, double imaginary) {
		return new PoleZero(sequence, createFloatNoUnitType(real), createFloatNoUnitType(imaginary));
	}

	FloatNoUnitType createFloatNoUnitType(double value) {
		return FloatNoUnitType.valueOf(value);
	}

	Coefficients createCoefficients() {
		Coefficients coefficients = new Coefficients();

		coefficients.setCfTransferFunctionType(CfTransferFunctionType.DIGITAL);
		coefficients.setInputUnits(StationUnits.COUNT);
		coefficients.setOutputUnits(StationUnits.COUNT);
		coefficients.getNumerators().add(Numerator.valueOf(+4.18952E-13));
		coefficients.getNumerators().add(Numerator.valueOf(+3.30318E-04));
		coefficients.getNumerators().add(Numerator.valueOf(+1.02921E-03));
		coefficients.getNumerators().add(Numerator.valueOf(-3.14123E-03));
		coefficients.getNumerators().add(Numerator.valueOf(+2.05709E-04));
		coefficients.getNumerators().add(Numerator.valueOf(+1.52521E-03));
		coefficients.getNumerators().add(Numerator.valueOf(-6.23193E-03));
		coefficients.getNumerators().add(Numerator.valueOf(+1.04801E-02));
		coefficients.getNumerators().add(Numerator.valueOf(-1.31202E-02));
		coefficients.getNumerators().add(Numerator.valueOf(+1.07821E-02));
		coefficients.getNumerators().add(Numerator.valueOf(-1.44455E-03));
		coefficients.getNumerators().add(Numerator.valueOf(-1.58729E-02));
		coefficients.getNumerators().add(Numerator.valueOf(+3.95074E-02));
		coefficients.getNumerators().add(Numerator.valueOf(-6.51036E-02));
		coefficients.getNumerators().add(Numerator.valueOf(+8.53716E-02));
		coefficients.getNumerators().add(Numerator.valueOf(-8.91913E-02));
		coefficients.getNumerators().add(Numerator.valueOf(+5.00619E-02));
		coefficients.getNumerators().add(Numerator.valueOf(+8.37233E-01));
		coefficients.getNumerators().add(Numerator.valueOf(+2.66723E-01));
		coefficients.getNumerators().add(Numerator.valueOf(-1.66693E-01));
		coefficients.getNumerators().add(Numerator.valueOf(+9.52840E-02));
		coefficients.getNumerators().add(Numerator.valueOf(-5.09218E-02));
		coefficients.getNumerators().add(Numerator.valueOf(+1.61458E-02));
		coefficients.getNumerators().add(Numerator.valueOf(+7.06362E-03));
		coefficients.getNumerators().add(Numerator.valueOf(-1.83877E-02));
		coefficients.getNumerators().add(Numerator.valueOf(+1.99414E-02));
		coefficients.getNumerators().add(Numerator.valueOf(-1.54895E-02));
		coefficients.getNumerators().add(Numerator.valueOf(+8.52735E-03));
		coefficients.getNumerators().add(Numerator.valueOf(-2.55789E-03));
		coefficients.getNumerators().add(Numerator.valueOf(-1.81103E-03));
		coefficients.getNumerators().add(Numerator.valueOf(+2.42649E-03));
		coefficients.getNumerators().add(Numerator.valueOf(-3.75769E-03));
		coefficients.getNumerators().add(Numerator.valueOf(+4.67293E-04));
		coefficients.getNumerators().add(Numerator.valueOf(+6.33072E-04));
		coefficients.getNumerators().add(Numerator.valueOf(-1.56874E-06));
		coefficients.getNumerators().add(Numerator.valueOf(-1.25480E-05));
		coefficients.getNumerators().add(Numerator.valueOf(+3.21041E-07));
		coefficients.getNumerators().add(Numerator.valueOf(-2.63324E-08));
		coefficients.getNumerators().add(Numerator.valueOf(-5.09997E-08));
		return coefficients;
	}

	double[] frequencies = new double[] { 0.1, 0.1291549665014884, 0.16681005372000587, 0.21544346900318834,
			0.2782559402207124, 0.35938136638046275, 0.46415888336127786, 0.5994842503189409, 0.774263682681127, 1.0,
			1.291549665014884, 1.6681005372000584, 2.1544346900318834, 2.7825594022071245, 3.593813663804626,
			4.6415888336127775, 5.994842503189409, 7.742636826811269, 10.0 };
}
