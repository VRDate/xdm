package xdman.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;

public class UTF8FileUtils
		implements FileUtils {
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	public static final String KEY_VALUE_DELIMITER = ":";
	public static final String COMMENT = "#";

	private final File file;
	private final String name;
	private final FileManager fileManager;

	public UTF8FileUtils(File file,
	                     String name,
	                     FileManager fileManager) {

		this.file = file;
		this.name = name;
		this.fileManager = fileManager;
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public String getName() {
		return name;
	}

	private FileManager getFileManager() {
		return fileManager;
	}

	@Override
	public boolean save() {
		FileManager fileManager = getFileManager();
		BufferedWriter bufferedWriter = null;
		boolean saved = false;
		try {
			bufferedWriter = FileUtils.getBufferedWriter(getFile(),
					false);
			fileManager.save(bufferedWriter,
					this);
			saved = true;
		} catch (Exception e) {
			Logger.log("Cannot save",
					this,
					e);
			saved = false;
		} finally {
			try {
				if (bufferedWriter != null) {
					bufferedWriter.close();
				}
			} catch (Exception e) {
				Logger.log("Cannot save close",
						this,
						e);
			}
			fileManager.saveFinally(this,
					saved);
		}
		return saved;
	}

	@Override
	public boolean save(BufferedWriter bufferedWriter,
	                    String key,
	                    Object value)
			throws IOException {
		String valueString = value == null
				? ""
				: value.toString();
		if (StringUtils.isNullOrEmptyOrBlank(valueString)) {
			Logger.log(this,
					"save skipping",
					key,
					"=",
					value);
			return false;
		}
		int keyLength = key.length();
		int keyValueDelimiterLength = KEY_VALUE_DELIMITER.length();
		int valueStringLength = valueString.length();
		int capacity = keyLength
				+ keyValueDelimiterLength
				+ valueStringLength;
		StringBuilder lineStringBuilder = new StringBuilder(capacity);
		lineStringBuilder.append(key)
				.append(KEY_VALUE_DELIMITER)
				.append(valueString);
		String line = lineStringBuilder.toString();
		boolean saved = save(bufferedWriter,
				line);
		return saved;
	}

	@Override
	public boolean save(BufferedWriter bufferedWriter,
	                    Object value)
			throws IOException {
		String valueString = value == null
				? ""
				: value.toString();
		if (StringUtils.isNullOrEmptyOrBlank(valueString)) {
			Logger.log(this,
					"save skipping",
					valueString);
			return false;
		}
		bufferedWriter.write(valueString);
		bufferedWriter.write(LINE_SEPARATOR);
		return true;
	}

	@Override
	public boolean load() {
		boolean loaded = false;
		String name = getName();
		File file = getFile();
		FileManager fileManager = getFileManager();
		if (!file.exists()) {
			Logger.log("No saved",
					this);
			return loaded;
		}
		BufferedReader bufferedReader = null;
		try {
			Logger.log("Loading",
					this,
					"...");
			bufferedReader = FileUtils.getBufferedReader(file);
			loaded = fileManager.parse(bufferedReader,
					this);
		} catch (Exception e) {
			Logger.log("Cannot load",
					this,
					e);
			loaded = false;
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}
			} catch (Exception e) {
				Logger.log("Cannot load close",
						this,
						e);
			}
			fileManager.loadFinally(this,
					loaded);
		}
		return loaded;
	}

	@Override
	public boolean parseLines(BufferedReader bufferedReader,
	                          FileManager fileManager)
			throws IOException,
			ParseException {
		boolean loaded = false;
		String line;
		long lineIndex = 0;
		while ((line = bufferedReader.readLine()) != null) {
			lineIndex++;
			if (line.startsWith(COMMENT)) {
				continue;
			}
			if (fileManager.parseLine(lineIndex,
					line)) {
				loaded = true;
				continue;
			} else {
				Logger.log(this,
						"load skipping",
						line);
				loaded = false;
			}
		}
		return loaded;
	}

	@Override
	public boolean parseLine(long lineIndex,
	                         String line,
	                         FileManager fileManager)
			throws ParseException {
		int index = line.indexOf(KEY_VALUE_DELIMITER);
		if (index < 1)
			return true;
		String key = line.substring(0, index);
		String value = line.substring(index + 1);
		boolean loaded = fileManager.parse(lineIndex,
				key,
				value);
		return loaded;
	}

	@Override
	public String toString() {
		FileManager fileManager = getFileManager();
		String utf8FileManagerName = fileManager == null
				? "UTF8 File Manager Not Set!"
				: "UTF8 File";

		String name = getName();
		String nameString = name == null
				? "Name Not Set!"
				: name;

		File file = getFile();
		String fileAbsolutePath = file == null
				? "File Not Set!"
				: file.getAbsolutePath();


		String string = String.format("%s %s %s",
				utf8FileManagerName,
				nameString,
				fileAbsolutePath);
		return string;
	}
}
