package xdman;

import xdman.ui.res.StringResource;
import xdman.util.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class QueueManager
		extends LinkedHashMap<String, DownloadQueue>
		implements FileManager {
	public static final String DEFAULT_DOWNLOAD_QUEUE_ID = "";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final String DEFAULT_DOWNLOAD_QUEUE_NAME = StringResource.get("DEF_QUEUE");
	private static QueueManager _this;
	private static int queueIndex = 0;
	private final FileUtils fileUtils;
	private File queuesFile = new File(Config.getInstance().getDataFolder(), "queues.txt");

	private QueueManager() {
		fileUtils = new UTF8FileUtils(queuesFile,
				"Queues",
				this);
		load();
	}

	public static QueueManager getInstance() {
		if (_this == null) {
			_this = new QueueManager();
		}
		return _this;
	}

	private void load() {
		load(fileUtils);
	}

	public DownloadQueue getQueueById(String queueId) {
		if (queueId == null) {
			return null;
		}
		if (StringUtils.isNullOrEmptyOrBlank(queueId)) {
			return get(DEFAULT_DOWNLOAD_QUEUE_ID);
		}
		DownloadQueue downloadQueue = get(queueId);
		return downloadQueue;
	}

	public Collection<DownloadQueue> getDownloadQueues() {
		return values();
	}

	public DownloadQueue getDefaultQueue() {
		return get(DEFAULT_DOWNLOAD_QUEUE_ID);
	}

	public void removeQueue(String queueId) {
		DownloadQueue queueById = getQueueById(queueId);
		if (queueById == null)
			return;
		if (queueById.isRunning()) {
			queueById.stop();
		}
		ArrayDeque<String> queuedItems = queueById.getQueuedItems();
		for (String queuedItem : queuedItems) {
			DownloadEntry ent = XDMApp.getInstance().getEntry(queuedItem);
			if (ent != null) {
				ent.setQueueId("");
			}
		}
		remove(queueById);
	}

	public DownloadQueue createNewQueue() {
		String queueWord = StringResource.get("Q_WORD");
		queueIndex++;
		String queueName = String.format("%s %d",
				queueWord,
				queueIndex);
		DownloadQueue downloadQueue = new DownloadQueue(UUID.randomUUID().toString(),
				queueName);
		put(downloadQueue.getQueueId(),
				downloadQueue);
		save();
		return downloadQueue;
	}

	// check and remove invalid entries from queued item list (invalid entries
	// might appear from corrupt Downloads
	public void fixCorruptEntries(Iterator<String> ids,
	                              XDMApp xdmApp) {
		DownloadQueue defaultQueue = getDefaultQueue();
		while (ids.hasNext()) {
			String id = ids.next();
			DownloadEntry downloadEntry = xdmApp.getEntry(id);
			if (downloadEntry == null) {
				continue;
			}
			String queueId = downloadEntry.getQueueId();
			if (queueId == null
					|| getQueueById(queueId) == null) {
				defaultQueue.getQueuedItems().add(id);
				downloadEntry.setQueueId(DEFAULT_DOWNLOAD_QUEUE_ID);
			}
		}
		for (DownloadQueue downloadQueue : values()) {
			ArrayList<String> corruptIds = new ArrayList<>();
			ArrayDeque<String> queuedItems = downloadQueue.getQueuedItems();
			for (String queuedItem : queuedItems) {
				if (xdmApp.getEntry(queuedItem) == null) {
					corruptIds.add(queuedItem);
				}
			}
			queuedItems.removeAll(corruptIds);
		}
	}

	public boolean save() {
		return save(fileUtils);
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
		fixCorruptEntries(keySet().iterator(),
				XDMApp.getInstance());
		int count = size();
		fileUtils.save(bufferedWriter,
				count);
		for (DownloadQueue downloadQueue : values()) {
			if (downloadQueue == null) {
				continue;
			}
			fileUtils.save(bufferedWriter,
					downloadQueue.getQueueId());
			fileUtils.save(bufferedWriter,
					downloadQueue.getName());
			ArrayDeque<String> queuedItems = downloadQueue.getQueuedItems();
			fileUtils.save(bufferedWriter,
					queuedItems.size());
			for (String queuedItem : queuedItems) {
				fileUtils.save(bufferedWriter,
						queuedItem);
			}
			if (downloadQueue.getStartTime() != -1) {
				fileUtils.save(bufferedWriter,
						"1");
				fileUtils.save(bufferedWriter,
						downloadQueue.getStartTime());
				if (downloadQueue.getEndTime() != -1) {
					fileUtils.save(bufferedWriter,
							"1");
					fileUtils.save(bufferedWriter,
							downloadQueue.getEndTime());
				} else {
					fileUtils.save(bufferedWriter,
							"0");
				}
				fileUtils.save(bufferedWriter,
						(downloadQueue.isPeriodic() ? 1 : 0));
				if (downloadQueue.isPeriodic()) {
					fileUtils.save(bufferedWriter,
							downloadQueue.getDayMask());
				} else {
					if (downloadQueue.getExecDate() != null) {
						fileUtils.save(bufferedWriter,
								"1");
						fileUtils.save(bufferedWriter,
								DATE_FORMAT.format(downloadQueue.getExecDate()));
					} else {
						fileUtils.save(bufferedWriter,
								"0");
					}
				}
			} else {
				fileUtils.save(bufferedWriter,
						"0");
			}
		}
	}

	@Override
	public void saveFinally(FileUtils fileUtils,
	                        boolean saved) {
		Logger.log(fileUtils,
				"saved",
				saved);
	}

	@Override
	public boolean load(FileUtils fileUtils) {
		clear();
		DownloadQueue defaultDownloadQueue = new DownloadQueue(DEFAULT_DOWNLOAD_QUEUE_ID,
				DEFAULT_DOWNLOAD_QUEUE_NAME);
		put(DEFAULT_DOWNLOAD_QUEUE_ID, defaultDownloadQueue);
		boolean loaded = fileUtils.load();
		return loaded;
	}

	@Override
	public boolean parse(BufferedReader bufferedReader,
	                     FileUtils fileUtils)
			throws IOException,
			ParseException {
		boolean loaded = false;
		String str = bufferedReader.readLine();
		int count = Integer.parseInt((str == null ? "0" : str).trim());
		if (count == 0)
			return loaded;
		for (int i = 0; i < count; i++) {
			String downloadQueueIdLine = bufferedReader.readLine();
			String downloadQueueId = downloadQueueIdLine == null
					? DEFAULT_DOWNLOAD_QUEUE_ID
					: downloadQueueIdLine.trim();
			String downloadQueueNameLine = bufferedReader.readLine();
			String downloadQueueName = downloadQueueNameLine == null
					? DEFAULT_DOWNLOAD_QUEUE_NAME
					: downloadQueueNameLine.trim();
			DownloadQueue downloadQueue;
			if (DEFAULT_DOWNLOAD_QUEUE_ID.equals(downloadQueueId)) {
				downloadQueue = new DownloadQueue(DEFAULT_DOWNLOAD_QUEUE_ID,
						DEFAULT_DOWNLOAD_QUEUE_NAME);
			} else {
				downloadQueue = new DownloadQueue(downloadQueueId,
						downloadQueueName);
			}
			String downloadQueueItemsCountLine = bufferedReader.readLine();
			int downloadQueueItemsCount = downloadQueueItemsCountLine == null
					? 0
					: Integer.parseInt(downloadQueueItemsCountLine.trim());
			ArrayDeque<String> queuedItems = downloadQueue.getQueuedItems();
			for (int downloadQueueItemIndex = 0; downloadQueueItemIndex < downloadQueueItemsCount; downloadQueueItemIndex++) {
				String downloadQueueItemLine = bufferedReader.readLine();
				if (downloadQueueItemLine != null) {
					String downloadQueueItem = downloadQueueItemLine.trim();
					queuedItems.add(downloadQueueItem);
				}
			}
			String hasStartTimeLine = bufferedReader.readLine();
			boolean hasStartTime = hasStartTimeLine != null
					&& Integer.parseInt(hasStartTimeLine.trim()) == 1;
			if (hasStartTime) {
				downloadQueue.setStartTime(Long.parseLong(bufferedReader.readLine()));
				boolean hasEndTime = Integer.parseInt(bufferedReader.readLine()) == 1;
				if (hasEndTime) {
					downloadQueue.setEndTime(Long.parseLong(bufferedReader.readLine()));
				}
				boolean isPeriodic = Integer.parseInt(bufferedReader.readLine()) == 1;
				downloadQueue.setPeriodic(isPeriodic);
				if (isPeriodic) {
					downloadQueue.setDayMask(Integer.parseInt(bufferedReader.readLine()));
				} else {
					if (Integer.parseInt(bufferedReader.readLine()) == 1) {
						String ln = bufferedReader.readLine();
						if (ln != null)
							downloadQueue.setExecDate(DATE_FORMAT.parse(ln));
					}
				}
			}
			if (downloadQueue.getQueueId().length() > 0) {
				put(downloadQueue.getQueueId(),
						downloadQueue);
				loaded = true;
			}
		}
		return loaded;
	}

	@Override
	public boolean parseLine(long lineIndex,
	                         String line) {
		return false;
	}

	@Override
	public boolean parse(long lineIndex, String key,
	                     String value) {
		return false;
	}

	@Override
	public void loadFinally(FileUtils fileUtils,
	                        boolean loaded) {
		Logger.log(fileUtils,
				"loaded",
				loaded);
	}
}
