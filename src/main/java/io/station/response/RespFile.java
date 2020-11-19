package io.station.response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;

import io.station.model.FDSNStationXML;
import io.station.util.ChannelIterator;

public class RespFile {

	public String extension() {
		return "resp";
	}

	public String d() {
		return "#\n" + "###################################################################################\n" + "#\n"
				+ "B050F03";
	}
	/*
	 * public static FDSNStationXML read(final Path file) throws IOException {
	 * 
	 * ContentTypeDescriptor contentTypeDescriptor = null; try {
	 * contentTypeDescriptor =
	 * DefaultContentDiscoveryService.instance().discover(file); } catch
	 * (UnkownContentTypeException | IOException e) { throw new IOException(e); }
	 * 
	 * if (contentTypeDescriptor == null) { throw new IOException(); } try
	 * (InputStream inputStream = Files.newInputStream(file);) { return
	 * RespIO.read(inputStream); } }
	 */

	public static FDSNStationXML read(Path path) throws IOException {
		Objects.requireNonNull(path, "path cannot be null.");
		File file=path.toFile();
		validateFile(file);
		try (InputStream inputStream = new FileInputStream(file);) {
			return RespIO.read(inputStream);
		}
	}
	
	public static FDSNStationXML read(File file) throws IOException {
		validateFile(file);
		try (InputStream inputStream = new FileInputStream(file);) {
			return RespIO.read(inputStream);
		}
	}

	public static ChannelIterator iterateChannels(File file) throws IOException {
		validateFile(file);

		InputStream inputStream = null;

		try {
			inputStream = new FileInputStream(file);
			return RespIO.iterateChannels(new FileInputStream(file));
		} catch (final IOException | RuntimeException ex) {
			if (inputStream != null) {
				inputStream.close();
			}
			throw ex;
		}
	}

	public static void validateFile(final File file) throws IOException {
		if (file.exists()) {
			if (file.isDirectory()) {
				throw new IOException("File '" + file + "' exists but is a directory");
			}
			if (file.canRead() == false) {
				throw new IOException("File '" + file + "' cannot be read");
			}
		} else {
			throw new FileNotFoundException("File '" + file + "' does not exist");
		}
	}
}
