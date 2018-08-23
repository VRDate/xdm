package xdman;

import xdman.util.FileManager;
import xdman.util.FileUtils;
import xdman.util.Logger;
import xdman.util.UTF8FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class DownloadEntriesManager
		extends LinkedHashMap<String, DownloadEntry>
		implements FileManager {
	public static final String DOWNLOAD_ENTRIES = "Download Entries";
	private static final File downloadEntriesFile = new File(Config.getInstance().getDataFolder(), "downloads.txt");
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static DownloadEntriesManager _this;
	private FileUtils fileUtils;

	private DownloadEntriesManager() {
		load();
	}

	public static DownloadEntriesManager getInstance() {
		if (_this == null) {
			_this = new DownloadEntriesManager();
		}
		return _this;
	}

	public boolean save() {
		boolean saved = save(downloadEntriesFile);
		return saved;
	}

	public boolean save(File downloadEntriesFile) {
		fileUtils = new UTF8FileUtils(downloadEntriesFile,
				DOWNLOAD_ENTRIES,
				this);
		boolean saved = save(fileUtils);
		return saved;
	}

	@Override
	public boolean save(FileUtils fileUtils) {
		boolean saved = fileUtils.save();
		return saved;
	}

	@Override
	public void save(BufferedWriter bufferedWriter,
	                 FileUtils fileUtils)
			throws IOException {
		int count = size();
		bufferedWriter.write(count + "");
		bufferedWriter.newLine();
		Iterator<String> keyIterator = keySet().iterator();
		while (keyIterator.hasNext()) {
			String key = keyIterator.next();
			DownloadEntry ent = get(key);
			int c = 0;
			StringBuffer sb = new StringBuffer();
			sb.append("id: ").append(ent.getId()).append(UTF8FileUtils.LINE_SEPARATOR);
			c++;
			sb.append("queuesFile: ").append(ent.getFile()).append(UTF8FileUtils.LINE_SEPARATOR);
			c++;
			sb.append("category: ").append(ent.getCategory()).append(UTF8FileUtils.LINE_SEPARATOR);
			c++;
			sb.append("state: ").append(ent.getState()).append(UTF8FileUtils.LINE_SEPARATOR);
			c++;
			if (ent.getFolder() != null) {
				sb.append("folder: ").append(ent.getFolder()).append(UTF8FileUtils.LINE_SEPARATOR);
				c++;
			}
			sb.append("date: ").append(dateFormat.format(new Date(ent.getDate()))).append(UTF8FileUtils.LINE_SEPARATOR);
			c++;
			sb.append("downloaded: ").append(ent.getDownloaded()).append(UTF8FileUtils.LINE_SEPARATOR);
			c++;
			sb.append("size: ").append(ent.getSize()).append(UTF8FileUtils.LINE_SEPARATOR);
			c++;
			sb.append("progress: ").append(ent.getProgress()).append(UTF8FileUtils.LINE_SEPARATOR);
			c++;
			if (ent.getTempFolder() != null) {
				sb.append("tempfolder: ").append(ent.getTempFolder()).append(UTF8FileUtils.LINE_SEPARATOR);
				c++;
			}
			if (ent.getQueueId() != null) {
				sb.append("queueid: ").append(ent.getQueueId()).append(UTF8FileUtils.LINE_SEPARATOR);
				c++;
			}
			sb.append("formatIndex: ").append(ent.getOutputFormatIndex()).append(UTF8FileUtils.LINE_SEPARATOR);
			c++;
			bufferedWriter.write(c + UTF8FileUtils.LINE_SEPARATOR);
			bufferedWriter.write(sb.toString());
		}
	}

	@Override
	public void saveFinally(FileUtils fileUtils,
	                        boolean saved) {
		Logger.log(fileUtils,
				"saved",
				saved);
	}

	public boolean load() {
		boolean loaded = load(downloadEntriesFile);
		return loaded;
	}

	public boolean load(File downloadEntriesFile) {
		fileUtils = new UTF8FileUtils(downloadEntriesFile,
				DOWNLOAD_ENTRIES,
				this);
		boolean loaded = load(fileUtils);
		return loaded;
	}

	@Override
	public boolean load(FileUtils fileUtils) {
		clear();
		boolean loaded = fileUtils.load();
		return loaded;
	}

	@Override
	public boolean parse(BufferedReader bufferedReader,
	                     FileUtils fileUtils)
			throws IOException,
			ParseException {
		boolean parsed = false;
		long lineIndex = 1;
		int count = Integer.parseInt(bufferedReader.readLine().trim());
		lineIndex++;
		for (int i = 0; i < count; i++) {
			int fieldCount = Integer.parseInt(bufferedReader.readLine().trim());
			lineIndex++;
			DownloadEntry downloadEntry = new DownloadEntry();
			for (int j = 0; j < fieldCount; j++) {
				String ln = bufferedReader.readLine();
				lineIndex++;
				if (ln == null) {
					return parsed;
				}
				int index = ln.indexOf(UTF8FileUtils.KEY_VALUE_DELIMITER);
				if (index > 0) {
					String key = ln.substring(0, index).trim();
					String value = ln.substring(index + 1).trim();
					parsed = parse(downloadEntry,
							lineIndex,
							key,
							value);
				}
			}
			put(downloadEntry.getId(),
					downloadEntry);
		}
		return parsed;
	}

	@Override
	public boolean parseLine(long lineIndex,
	                         String line)
			throws ParseException {
		return false;
	}

	@Override
	public boolean parse(long lineIndex,
	                     String key,
	                     String value)
			throws ParseException {
		return false;
	}

	public boolean parse(DownloadEntry downloadEntry,
	                     long lineIndex,
	                     String key,
	                     String value)
			throws ParseException {
		switch (key) {
			case "id":
				downloadEntry.setId(value);
				return true;
			case "queuesFile":
				downloadEntry.setFile(value);
				return true;
			case "category":
				downloadEntry.setCategory(Integer.parseInt(value));
				return true;
			case "state":
				int state = Integer.parseInt(value);
				int stateValue = state == XDMConstants.FINISHED
						? state
						: XDMConstants.PAUSED;
				downloadEntry.setState(stateValue);
				return true;
			case "folder":
				downloadEntry.setFolder(value);
				return true;
			case "date":
				Date date = dateFormat.parse(value);
				long time = date.getTime();
				downloadEntry.setDate(time);
				return true;
			case "downloaded":
				downloadEntry.setDownloaded(Long.parseLong(value));
				return true;
			case "size":
				downloadEntry.setSize(Long.parseLong(value));
				return true;
			case "progress":
				downloadEntry.setProgress(Integer.parseInt(value));
				return true;
			case "queueid":
				downloadEntry.setQueueId(value);
				return true;
			case "formatIndex":
				downloadEntry.setOutputFormatIndex(Integer.parseInt(value));
				return true;
			case "tempfolder":
				downloadEntry.setTempFolder(value);
				return true;
		}
		ParseException parseException = fileUtils.getParseException(lineIndex,
				key,
				value);
		throw parseException;
	}

	@Override
	public void loadFinally(FileUtils fileUtils,
	                        boolean loaded) {
		Logger.log(fileUtils,
				"loaded",
				loaded);
	}
}
