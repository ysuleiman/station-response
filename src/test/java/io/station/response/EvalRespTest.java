package io.station.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.math3.complex.Complex;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import io.station.math.Spectrum;
import io.station.model.Channel;
import io.station.model.FDSNStationXML;
import io.station.response.ResponseSpectrum.StageSpectrum;
import io.station.response.util.FrequencySet;
import io.station.util.DoubleDataSet;

public class EvalRespTest {

	double[] frequency = new double[] { 0.1, 0.1291549665014884, 0.16681005372000587, 0.21544346900318834,
			0.2782559402207124, 0.35938136638046275, 0.46415888336127786, 0.5994842503189409, 0.774263682681127, 1.0,
			1.291549665014884, 1.6681005372000584, 2.1544346900318834, 2.7825594022071245, 3.593813663804626,
			4.6415888336127775, 5.994842503189409, 7.742636826811269, 10.0 };

	@Test
	public void imil01() throws Exception {
		Path path = Paths.get(this.getClass().getClassLoader()
				.getResource("IM&sta=IL01&loc=--&cha=SHZ&time=2010-001T00.00.00.resp").toURI());
		List<NormalizedResponse> list = EvalResp.normalize(path);

		assertNotNull(list);
		assertEquals(1, list.size());

		NormalizedResponse normalizedResponse = list.get(0);

		assertEquals(0.919836, normalizedResponse.getNormalizationFactor(1));
		assertEquals(1.0, normalizedResponse.getNormalizationFrequency(1).getValue());
		assertEquals(0.999996, normalizedResponse.getNormalizationFactor(3), 0.000005);

		System.out.println(normalizedResponse.getNormalizationFactor(1));

		FrequencySet frequencySet = EvalRespUtil.createFrequency();
		Complex[] spectrum = EvalResp.calculate(normalizedResponse, frequencySet).asDisplacement(0);
		assertNotNull(spectrum);
		System.out.println("..................................");
		double[][] result = EvalRespUtil.ampPase(spectrum, true);
		int length = result[0].length;
		for (int i = 0; i < length; i++) {
			System.out.println(spectrum[i] + ": " + frequency[i] + "   " + result[0][i] + "  " + result[1][i]);
		}

	}

	@Test
	public void testEmptyResponse() throws Exception {
		Path path = Paths.get(this.getClass().getClassLoader().getResource("iu.anmo.bhz.one.epoch.resp").toURI());
		List<NormalizedResponse> list = EvalResp.normalize(path);

		assertNotNull(list);
		assertEquals(1, list.size());

		NormalizedResponse normalizedResponse = list.get(0);

		assertEquals(5.51178E-20, normalizedResponse.getNormalizationFactor(1));
		assertEquals(0.02, normalizedResponse.getNormalizationFrequency(1).getValue());
		assertEquals(0.999996, normalizedResponse.getNormalizationFactor(3), 0.000005);

		System.out.println(normalizedResponse.getNormalizationFactor(1));

		FrequencySet frequencySet = EvalRespUtil.createFrequency();
		ResponseSpectrum spectrum = EvalResp.calculate(normalizedResponse, frequencySet);
		assertNotNull(spectrum);

		assertEquals(4, spectrum.size());
		StageSpectrum s1 = spectrum.get(1);
		assertNotNull(s1);
		assertEquals(1, s1.getStageNumber());
		assertArrayEquals(c1, s1.getAll(), 0.0001);

		StageSpectrum s2 = spectrum.get(2);
		assertNotNull(s2);
		assertEquals(2, s2.getStageNumber());
		assertArrayEquals(c2, s2.getAll(), 0.000001);
		StageSpectrum s3 = spectrum.get(3);
		assertNotNull(s3);

		assertArrayEquals(c3, s3.getAll(), 0.0001);

		StageSpectrum s0 = spectrum.get(0);
		assertNotNull(s0);

		assertArrayEquals(c0, s0.getAll(), 0.0003);
	}

	@Test
	public void displacement() throws Exception {
		Path path = Paths.get(this.getClass().getClassLoader().getResource("iu.anmo.bhz.one.epoch.resp").toURI());
		List<NormalizedResponse> list = EvalResp.normalize(path);

		assertNotNull(list);
		assertEquals(1, list.size());

		NormalizedResponse normalizedResponse = list.get(0);

		assertEquals(5.51178E-20, normalizedResponse.getNormalizationFactor(1));
		assertEquals(0.02, normalizedResponse.getNormalizationFrequency(1).getValue());
		assertEquals(0.999996, normalizedResponse.getNormalizationFactor(3), 0.000005);

		FrequencySet frequencySet = EvalRespUtil.createFrequency();
		Complex[][] array = EvalResp.calculate(normalizedResponse, frequencySet).asDisplacement();
		assertNotNull(array);
		assertEquals(4, array.length);

		// assertArrayEquals(d0, array[0], 0.000001);
		for (Complex[] stage : array) {
			System.out.println("//////");
			for (Complex c : stage) {
				System.out.println(c);
			}
		}
	}

	@Test
	public void power() throws Exception {
		Path path = Paths.get(this.getClass().getClassLoader().getResource("iu.anmo.bhz.one.epoch.resp").toURI());
		FDSNStationXML fdsnStationXML = RespFile.read(path);

		List<Channel> channels = fdsnStationXML.find("IU", "ANMO", "00", "BHZ");
		
		NormalizedResponse normalizedResponse = EvalResp.normalize(channels.get(0));

		assertNotNull(normalizedResponse);
		assertEquals(5.51178E-20, normalizedResponse.getNormalizationFactor(1));
		assertEquals(0.02, normalizedResponse.getNormalizationFrequency(1).getValue());
		assertEquals(0.999996, normalizedResponse.getNormalizationFactor(3), 0.000005);

		FrequencySet frequencySet = EvalRespUtil.createFrequency();
		ResponseSpectrum responseSpectrum = EvalResp.calculate(normalizedResponse, frequencySet);
		assertNotNull(responseSpectrum);

		Complex[] s0 = responseSpectrum.calculateTotalSpectrum();
		assertNotNull(s0);
		assertEquals(frequency.length, s0.length);
		assertArrayEquals(c0, s0, 0.000001);

		Complex[] complexArray = responseSpectrum.asDisplacement(0);
		double[][] array = EvalRespUtil.ampPase(complexArray, true);
		
		
	}

	private static void assertArrayEquals(Complex[] expected, Complex[] actual, double delta) {
		if (expected == actual) {
			return;
		}
		if (expected == null) {
			throw new AssertionFailedError("");
		}
		if (actual == null) {
			throw new AssertionFailedError("");
		}
		if (actual.length != expected.length) {
			throw new AssertionFailedError("");
		}
		if (Double.isNaN(delta) || delta < 0.0) {
			throw new AssertionFailedError("");
		}
		for (int i = 0; i < expected.length; i++) {
			if (Double.doubleToLongBits(actual[i].getReal()) != Double.doubleToLongBits(expected[i].getReal())) {
				if (Math.abs(actual[i].getReal() - expected[i].getReal()) > delta) {
					throw new AssertionFailedError("index:real " + i + " expected<" + expected[i].getReal()
							+ "> but was <" + actual[i].getReal() + ">");
				}
			}
			if (Double.doubleToLongBits(actual[i].getImaginary()) != Double
					.doubleToLongBits(expected[i].getImaginary())) {
				if (Math.abs(actual[i].getImaginary() - expected[i].getImaginary()) > delta) {
					throw new AssertionFailedError("index:imaginary " + i + " expected<" + expected[i].getImaginary()
							+ "> but was <" + actual[i].getImaginary() + ">");
				}
			}
		}
	}

	Complex[] c0 = new Complex[] { new Complex(1.9855534658593385E9, 9.520824753881085E7),
			new Complex(1.988135800967761E9, 8.239850976540066E7),
			new Complex(1.9916868316397805E9, 7.456323853956419E7),
			new Complex(1.9969411855009272E9, 7.07346644060725E7),
			new Complex(2.0047754615835795E9, 6.982969074185717E7),
			new Complex(2.0160787930280986E9, 7.04025941401677E7),
			new Complex(2.0314023523933823E9, 7.050620948432855E7),
			new Complex(2.0504659376208909E9, 6.791251782027535E7),
			new Complex(2.0718898778036427E9, 6.08034818639926E7),
			new Complex(2.0934764843971312E9, 4.850020486127371E7),
			new Complex(2.1126028534361935E9, 3.1504557641228847E7),
			new Complex(2.1258715355538604E9, 1.0792852281058613E7),
			new Complex(2.129184373169398E9, -1.2950391883180082E7),
			new Complex(2.1243567907977204E9, -3.9905571384347335E7),
			new Complex(2.131710212147185E9, -7.217329011702344E7),
			new Complex(2.149934884591198E9, -1.1316321646024546E8),
			new Complex(2.131932502330072E9, -1.648074721853953E8),
			new Complex(2.1396724416017756E9, -2.372187608916782E8),
			new Complex(2.1168301383645694E9, -3.2972884142016023E8) };
	Complex[] c1 = new Complex[] { new Complex(1183.4811092656041, 54.53415586609628),
			new Complex(1184.9606765400522, 46.249150759273384), new Complex(1186.9772807901377, 40.73636648956867),
			new Complex(1189.941131660027, 37.35926909900169), new Complex(1194.3279821681538, 35.39259978714278),
			new Complex(1200.5899221481363, 33.86863347014187), new Complex(1208.9311273209687, 31.488615308410612),
			new Complex(1219.004897388175, 26.751983615333945), new Complex(1229.7831878186496, 18.370930925791175),
			new Complex(1239.863202414269, 5.707108691250403), new Complex(1248.061255961549, -11.216358970777952),
			new Complex(1253.7811428301923, -32.18044321893795), new Complex(1256.9125822536791, -57.29655397164274),
			new Complex(1257.472175585309, -87.27110986040677), new Complex(1255.2495467375454, -123.43064872257025),
			new Complex(1249.520934033564, -167.65249647503936), new Complex(1238.7573352375096, -222.27032689043853),
			new Complex(1220.2297470563985, -289.9147994552606), new Complex(1189.4431063158916, -373.14981899814615) };
	Complex[] c2 = new Complex[] { new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0),
			new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0),
			new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0),
			new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0),
			new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0),
			new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0),
			new Complex(1677720.0, 0.0), new Complex(1677720.0, 0.0) };
	Complex[] c3 = new Complex[] { new Complex(1.0000878162362992, 0.0018670741715635295),
			new Complex(1.000146218260741, 0.002411449350996749), new Complex(1.0002431290862883, 0.003114571124121179),
			new Complex(1.000403373577477, 0.004022759355024661), new Complex(1.0006667727890446, 0.005195876083535621),
			new Complex(1.0010954027508263, 0.006711303132011956), new Complex(1.001781045011371, 0.008669077666574022),
			new Complex(1.0028455889181425, 0.011198433143651052),
			new Complex(1.0044124372160252, 0.014465755110351128), new Complex(1.0064957447015956, 0.01868288512369856),
			new Complex(1.0087166640693224, 0.02411125707064785), new Complex(1.009841456492754, 0.03105023015516588),
			new Complex(1.0078771092869403, 0.03980296120426792), new Complex(1.0034335010224449, 0.05072490651549386),
			new Complex(1.0058726588144755, 0.06463801623978055), new Complex(1.0145414174067942, 0.08214332477072271),
			new Complex(1.007600770854379, 0.10149425322842438), new Complex(1.0153814638692378, 0.12537030770133967),
			new Complex(1.0129196176024418, 0.1525393513357451) };
	Complex[] d0 = new Complex[] { new Complex(-5.9821106205817334E7, 1.24756003633069E9),
			new Complex(-6.6866768708390936E7, 1.6133813237967656E9),
			new Complex(-7.814961687454928E7, 2.087483938135871E9),
			new Complex(-9.575148080459866E7, 2.703201848724532E9),
			new Complex(-1.2208559706171331E8, 3.505016370570765E9),
			new Complex(-1.5897326207589874E8, 4.552426316729386E9),
			new Complex(-2.0562404678431123E8, 5.924374255846788E9),
			new Complex(-2.5580408651996616E8, 7.723429832207203E9),
			new Complex(-2.9579934416908085E8, 1.0079417300735342E10),
			new Complex(-3.047357745795549E8, 1.315370068769003E10),
			new Complex(-2.5566093064716923E8, 1.7143869079137316E10),
			new Complex(-1.131197205578439E8, 2.228122722162746E10),
			new Complex(1.7530573025633717E8, 2.8822156484208725E10),
			new Complex(6.976825268433509E8, 3.714084430581386E10),
			new Complex(1.6297159933922434E9, 4.813528966714532E10),
			new Complex(3.300287830798318E9, 6.270062091349048E10),
			new Complex(6.207754656603724E9, 8.030287549108548E10),
			new Complex(1.1540318374088722E10, 1.0409168777179326E11),
			new Complex(2.0717474117644985E10, 1.3300436023167194E11) };
}
