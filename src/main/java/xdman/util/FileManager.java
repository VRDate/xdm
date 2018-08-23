package xdman.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.ParseException;

public interface FileManager {
	boolean save(FileUtils fileUtils);

	void save(BufferedWriter bufferedWriter,
	          FileUtils fileUtils)
			throws IOException;

	void saveFinally(FileUtils fileUtils,
	                 boolean saved);

	boolean load(FileUtils fileUtils);

	boolean parse(BufferedReader bufferedReader,
	              FileUtils fileUtils)
			throws IOException,
			ParseException;

	boolean parseLine(long lineIndex,
	                  String line)
			throws ParseException;

	boolean parse(long lineIndex,
	              String key,
	              String value)
			throws ParseException;

	void loadFinally(FileUtils fileUtils,
	                 boolean loaded);
}
