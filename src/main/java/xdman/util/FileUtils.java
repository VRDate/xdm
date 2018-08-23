package xdman.util;

import xdman.Config;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.ParseException;

public interface FileUtils {
	char[] INVALID_FILE_CHARS = {'/', '\\', '"', '?', '*', '<', '>', ':', '|'};

	static String decodeFileName(String str) {
		char ch[] = str.toCharArray();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < ch.length; i++) {
			if (ch[i] == '/' || ch[i] == '\\' || ch[i] == '"' || ch[i] == '?' || ch[i] == '*' || ch[i] == '<'
					|| ch[i] == '>' || ch[i] == ':')
				continue;
			if (ch[i] == '%') {
				if (i + 2 < ch.length) {
					int c = Integer.parseInt(ch[i + 1] + "" + ch[i + 2], 16);
					buf.append((char) c);
					i += 2;
					continue;
				}
			}
			buf.append(ch[i]);
		}
		return buf.toString();
	}

	static String getFileName(String uri) {
		try {
			if (uri == null)
				return "FILE";
			if (uri.equals("/") || uri.length() < 1) {
				return "FILE";
			}
			int x = uri.lastIndexOf("/");
			String path = uri;
			if (x > -1) {
				path = uri.substring(x);
			}
			int qindex = path.indexOf("?");
			if (qindex > -1) {
				path = path.substring(0, qindex);
			}
			path = decodeFileName(path);
			if (path.length() < 1)
				return "FILE";
			if (path.equals("/"))
				return "FILE";
			return createSafeFileName(path);
		} catch (Exception e) {
			Logger.log(e);
			return "FILE";
		}
	}

	static String createSafeFileName(String str) {
		String safe_name = str;
		for (int i = 0; i < UTF8FileUtils.INVALID_FILE_CHARS.length; i++) {
			if (safe_name.indexOf(UTF8FileUtils.INVALID_FILE_CHARS[i]) != -1) {
				safe_name = safe_name.replace(UTF8FileUtils.INVALID_FILE_CHARS[i], '_');
			}
		}
		return safe_name;
	}

	static BufferedWriter getBufferedWriter(File file,
	                                        boolean append)
			throws FileNotFoundException {
		if (file == null) {
			return null;
		}
		if (!append && file.exists()) {
			file = renameOldFile(file);
		}
		FileOutputStream fileOutputStream = new FileOutputStream(file, append);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream,
				StandardCharsets.UTF_8);
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
		return bufferedWriter;
	}

	static File renameOldFile(File file) {
		String fileNameWithoutExtension = getFileNameWithoutExtension(file.getName());
		String timeStamp = DateTimeUtils.getFileNameTimeStamp();
		String extension = getExtension(file.getName());
		String oldFileName = String.format("%s_%s%s",
				fileNameWithoutExtension,
				timeStamp,
				extension);
		String temporaryFolder = Config.getInstance().getTemporaryFolder();
		File oldFile = new File(temporaryFolder, oldFileName);
		File newFile = new File(file.getParent(), file.getName());
		Logger.log("Renaming",
				file.getAbsolutePath(),
				"\nto",
				oldFile.getAbsolutePath());
		file.renameTo(oldFile);
		return newFile;
	}

	static String getExtension(String file) {
		int index = file.lastIndexOf(".");
		if (index > 0) {
			String ext = file.substring(index);
			return ext;
		} else {
			return null;
		}
	}

	static String getFileNameWithoutExtension(String fileName) {
		int index = fileName.lastIndexOf(".");
		if (index > 0) {
			fileName = fileName.substring(0, index);
			return fileName;
		} else {
			return fileName;
		}
	}

	static BufferedReader getBufferedReader(File file)
			throws FileNotFoundException {
		if (file == null) {
			return null;
		}
		FileInputStream fileInputStream = new FileInputStream(file);
		InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream,
				StandardCharsets.UTF_8);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		return bufferedReader;
	}

	static File getJarFile(Class<?> mainClass) {
		try {
			ProtectionDomain protectionDomain = mainClass.getProtectionDomain();
			CodeSource codeSource = protectionDomain.getCodeSource();
			URL url = codeSource.getLocation();
			URI uri = url.toURI();
			String path = uri.getPath();
			return new File(path);
		} catch (URISyntaxException e) {
			Logger.log(e);
		}
		return null;
	}

	File getFile();

	String getName();

	boolean save();

	boolean save(BufferedWriter bufferedWriter,
	             String key,
	             Object value)
			throws IOException;

	boolean save(BufferedWriter bufferedWriter,
	             Object value)
			throws IOException;

	boolean load();

	boolean parseLines(BufferedReader bufferedReader,
	                   FileManager fileManager)
			throws IOException,
			ParseException;

	boolean parseLine(long lineIndex,
	                  String line,
	                  FileManager fileManager)
			throws ParseException;

	@Override
	String toString();

	default ParseException getParseException(long lineIndex,
	                                         String key,
	                                         String value) {
		String message = String.format("%s line [%d]%s=%s",
				this.toString(),
				lineIndex,
				key,
				value);
		int errorOffset = Math.toIntExact(lineIndex);
		ParseException parseException = new ParseException(message,
				errorOffset);
		return parseException;
	}
}
