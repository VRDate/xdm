package xdman;

import xdman.downloaders.Downloader;
import xdman.downloaders.dash.DashDownloader;
import xdman.downloaders.ftp.FtpDownloader;
import xdman.downloaders.hds.HdsDownloader;
import xdman.downloaders.hls.HlsDownloader;
import xdman.downloaders.http.HttpDownloader;
import xdman.downloaders.metadata.DashMetadata;
import xdman.downloaders.metadata.HdsMetadata;
import xdman.downloaders.metadata.HlsMetadata;
import xdman.downloaders.metadata.HttpMetadata;
import xdman.mediaconversion.FFmpeg;
import xdman.monitoring.BrowserMonitor;
import xdman.network.http.HttpContext;
import xdman.ui.components.*;
import xdman.ui.laf.XDMLookAndFeel;
import xdman.ui.res.StringResource;
import xdman.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.net.PasswordAuthentication;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class XDMApp implements DownloadListener, DownloadWindowListener, Comparator<String> {
	private static final File downloadsFile = new File(Config.getInstance().getDataFolder(), "downloads.txt");

	public static final String APP_VERSION = "2018.08.20";

	private ArrayList<ListChangeListener> listChangeListeners;
	private Map<String, DownloadEntry> downloads;
	private static XDMApp _this;
	private HashMap<String, Downloader> downloaders;
	private HashMap<String, DownloadWindow> downloadWindows;
	private long lastSaved;
	private QueueManager queueManager;
	private LinkRefreshCallback refreshCallback;

	private ArrayList<String> pendingDownloads;// this buffer is used when there
	// is a limit on maximum
	// simultaneous downloads and
	// more downloads are started
	// than permissible limit. If
	// queues are also running then
	// this buffer will be processed
	// first
	private static HashMap<String, String> paramMap;
	private MainWindow mainWindow;
	private int pendingNotification = -1; // if main window in not created
	// notification is stored in this
	// variable

	private ArrayList<VideoPopupItem> itemList = new ArrayList<>();

	public static void instanceStarted() {
		Logger.log("instance starting...");
		final XDMApp app = XDMApp.getInstance();
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!paramMap.containsKey("background")) {
					Logger.log("showing main window.");
					app.showMainWindow();
				}
				TrayHandler.createTray();
				// if (XDMUtils.detectOS() != XDMUtils.LINUX) {
				// TrayHandler.createTray();
				// }
			}
		});
		if (Config.getInstance().isFirstRun()) {
			if (XDMUtils.detectOS() == XDMUtils.WINDOWS) {
				if (!XDMUtils.isAlreadyAutoStart()) {
					XDMUtils.addToStartup();
				}
			} else {
				XDMUtils.addToStartup();
			}
		}
		Logger.log("instance started.");
	}

	public static void instanceAlreadyRunning() {
		Logger.log("instance already runninng");
		ParamUtils.sendParam(paramMap);
		System.exit(0);
	}

	public static void start(String args[]) {
		paramMap = new HashMap<>();
		boolean expect = false;
		boolean winInstall = false;
		String key = null;
		for (int i = 0; i < args.length; i++) {
			if (expect) {
				if (key != null) {
					paramMap.put(key, args[i]);
				}
				expect = false;
				continue;
			}
			if ("-u".equals(args[i])) {
				key = "url";
				expect = true;
			} else if ("-m".equals(args[i])) {
				paramMap.put("background", "true");
				expect = false;
			} else if ("-i".equals(args[i])) {
				paramMap.put("installer", "true");
				expect = false;
				winInstall = true;
			} else if ("-s".equals(args[i])) {
				key = "screen";
				expect = true;
			}
		}

		if (winInstall) {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {

					@Override
					public void run() {
						Logger.log("wininstall");
						if (UpdateChecker.getComponentVersion() == null) {
							new ComponentInstaller().setVisible(true);
						}

					}
				});
			} catch (Exception e) {

			}
		}

		Logger.log("starting monitoring...");
		BrowserMonitor.getInstance().startMonitoring();
	}

	public void showMainWindow() {
		if (mainWindow == null) {
			mainWindow = new MainWindow();
		}
		mainWindow.setVisible(true);
		mainWindow.toFront();
	}

	private XDMApp() {
		Logger.log("Init app");
		String stype = paramMap.get("screen");
		if (stype != null) {
			if ("xxhdpi".equals(stype)) {
				XDMUtils.forceScreenType(XDMConstants.XHDPI);
			} else if ("xhdpi".equals(stype)) {
				XDMUtils.forceScreenType(XDMConstants.HDPI);
			} else if ("hdpi".equals(stype)) {
				XDMUtils.forceScreenType(XDMConstants.NORMAL);
			}
		}
		try {
			UIManager.setLookAndFeel(new XDMLookAndFeel());
		} catch (Exception e) {
			Logger.log(e);
		}
		Config.getInstance().loadConfig();
		listChangeListeners = new ArrayList<>();
		downloads = new HashMap<>();
		downloaders = new HashMap<>();
		downloadWindows = new HashMap<>();
		loadDownloads();
		lastSaved = System.currentTimeMillis();
		pendingDownloads = new ArrayList<>();
		queueManager = QueueManager.getInstance();
		queueManager.fixCorruptEntries(getDownloadIds(), this);
		QueueScheduler.getInstance().start();
		HttpContext.getInstance().init();
		if (Config.getInstance().isMonitorClipboard()) {
			ClipboardMonitor.getInstance().startMonitoring();
		}
	}

	public void exit() {
		saveDownloads();
		queueManager.saveQueues();
		Config.getInstance().save();
		System.exit(0);
		Logger.renameOldLog();
	}

	public void downloadFinished(String id) {
		DownloadEntry ent = downloads.get(id);
		ent.setState(XDMConstants.FINISHED);
		Downloader d = downloaders.remove(id);
		if (d != null && d.getSize() < 0) {
			ent.setSize(d.getDownloaded());
		}
		DownloadWindow wnd = downloadWindows.get(id);
		if (wnd != null) {
			wnd.close(XDMConstants.FINISHED, 0);
			downloadWindows.remove(id);
			if (ent.isStartedByUser()) {
				if (Config.getInstance().showDownloadCompleteWindow()) {
					new DownloadCompleteWnd(ent.getFile(), getFolder(ent)).setVisible(true);
				}
			}
		}
		notifyListeners(null);
		saveDownloads();
		if (Config.getInstance().isExecAntivirus()) {
			if (!StringUtils.isNullOrEmptyOrBlank(Config.getInstance().getAntivirusExe())) {
				execAntivirus();
			}
		}

		processNextItem(id);
		if (isAllFinished()) {
			if (Config.getInstance().isAutoShutdown()) {
				initShutdown();
			}
			if (Config.getInstance().isExecCmd()) {
				execCmd();
			}
		}
	}

	public void downloadFailed(String id) {
		Downloader d = downloaders.remove(id);
		if (id == null) {
			Logger.log("Download failed, id null");
			return;
		}
		DownloadWindow wnd = downloadWindows.get(id);
		if (wnd != null) {
			wnd.close(XDMConstants.FAILED, d.getErrorCode());
			downloadWindows.remove(id);
		} else {
			Logger.log("Wnd is null!!!");
		}
		DownloadEntry ent = downloads.get(id);
		ent.setState(XDMConstants.PAUSED);
		notifyListeners(id);
		saveDownloads();
		Logger.log("removed");
		processNextItem(id);
	}

	public void downloadStopped(String id) {
		downloaders.remove(id);
		DownloadWindow wnd = downloadWindows.get(id);
		if (wnd != null) {
			wnd.close(XDMConstants.PAUSED, 0);
			downloadWindows.remove(id);
		}
		DownloadEntry ent = downloads.get(id);
		ent.setState(XDMConstants.PAUSED);
		notifyListeners(id);
		saveDownloads();
		processNextItem(id);
	}

	public void downloadConfirmed(String id) {
		Logger.log("confirmed", id);
		Downloader d = downloaders.get(id);
		DownloadEntry ent = downloads.get(id);
		ent.setSize(d.getSize());
		if (d.isFileNameChanged()) {
			ent.setFile(d.getNewFile());
			ent.setCategory(XDMUtils.findCategory(d.getNewFile()));
			updateFileName(ent);
		}

		// if (isSameFile(ent.getFolder(), Config.getInstance().getDownloadFolder())) {
		// if (ent.getCategory() != XDMConstants.OTHER) {
		// File folderNew = new File(Config.getInstance().getDownloadFolder(),
		// XDMUtils.getFolderForCategory(ent.getCategory()));
		// if (!folderNew.exists()) {
		// folderNew.mkdirs();
		// }
		// ent.setFolder(folderNew.getAbsolutePath());
		// }
		// }

		DownloadWindow wnd = downloadWindows.get(id);
		if (wnd != null) {
			wnd.update(d, ent.getFile());
		}
		notifyListeners(id);
		saveDownloads();
	}

	public void downloadUpdated(String id) {
		try {
			DownloadEntry ent = downloads.get(id);
			Downloader d = downloaders.get(id);
			if (d == null) {
				Logger.log("################# sync error ##############");
				return;
			}
			ent.setSize(d.getSize());
			ent.setDownloaded(d.getDownloaded());
			ent.setProgress(d.getProgress());
			ent.setState(d.isAssembling() ? XDMConstants.ASSEMBLING : XDMConstants.DOWNLOADING);
			DownloadWindow wnd = downloadWindows.get(id);
			if (wnd != null) {
				wnd.update(d, ent.getFile());
			}
		} finally {
			notifyListeners(id);
			long now = System.currentTimeMillis();
			if (now - lastSaved > 5000) {
				saveDownloads();
				lastSaved = now;
			}
		}
	}

	public synchronized static XDMApp getInstance() {
		if (_this == null) {
			_this = new XDMApp();
		}
		return _this;
	}

	public void addLinks(final List<HttpMetadata> list) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				BatchDownloadWnd wnd = new BatchDownloadWnd(list);
				wnd.setVisible(true);
			}
		});
	}

	public void addDownload(final HttpMetadata metadata, final String file) {
		if (refreshCallback != null) {
			if (refreshCallback.isValidLink(metadata)) {
				return;
			}
			// HttpMetadata md = HttpMetadata.load(refreshCallback.getId());
			// if (md.getType() == metadata.getType()) {
			// if (md instanceof DashMetadata) {
			// DashMetadata dm1 = (DashMetadata) md;
			// DashMetadata dm2 = (DashMetadata) metadata;
			// if (dm1.getLen1() == dm2.getLen1() && dm1.getLen2() ==
			// dm2.getLen2()) {
			// if(refreshCallback.isValidLink(metadata))
			// dm1.setUrl(dm2.getUrl());
			// dm1.setUrl2(dm2.getUrl2());
			// dm1.setHeaders(dm2.getHeaders());
			// dm1.setLen1(dm2.getLen1());
			// dm1.setLen2(dm2.getLen2());
			// dm1.save();
			// }
			// } else if (md instanceof HlsMetadata) {
			// HlsMetadata hm1 = (HlsMetadata) md;
			// HlsMetadata hm2 = (HlsMetadata) metadata;
			// hm1
			// }
			// }
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (metadata != null && Config.getInstance().isDownloadAutoStart()) {
					String fileName = file;
					if (StringUtils.isNullOrEmptyOrBlank(file)) {
						fileName = XDMUtils.getFileName(metadata.getUrl());
					}
					createDownload(fileName, null, metadata, true, "", 0, 0);
					return;
				}
				new NewDownloadWindow(metadata, file).setVisible(true);
			}
		});
	}

	public void addVideo(final HttpMetadata metadata, final String file) {
		if (refreshCallback != null) {
			if (refreshCallback.isValidLink(metadata)) {
				return;
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!FFmpeg.isFFmpegInstalled()) {
					if (JOptionPane.showConfirmDialog(null, StringResource.get("MSG_INSTALL_ADDITIONAL_COMPONENTS"),
							StringResource.get("MSG_COMPONENT_TITLE"),
							JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						FFmpegDownloader fd = new FFmpegDownloader();
						fd.start();
					}
					return;
				}
				new VideoDownloadWindow(metadata, file).setVisible(true);
			}
		});
	}

	public void addMedia(final HttpMetadata metadata, final String file, final String info) {
		if (Config.getInstance().isShowVideoNotification()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					VideoPopup.getInstance().addVideo(metadata, file, info);
				}
			});
		}
	}

	public void youtubeVideoTitleUpdated(String url, String title) {
		if (Config.getInstance().isShowVideoNotification()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if (VideoPopup.hasInstance()) {
						VideoPopup.getInstance().updateYoutubeTitle(url, title);
					}
				}
			});
		}
	}

	public void createDownload(String file, String folder, HttpMetadata metadata, boolean now, String queueId,
	                           int formatIndex, int streamIndex) {
		metadata.save();
		DownloadEntry ent = new DownloadEntry();
		ent.setId(metadata.getId());
		ent.setOutputFormatIndex(formatIndex);
		ent.setState(XDMConstants.PAUSED);
		ent.setFile(file);
		ent.setFolder(folder);
		ent.setTempFolder(Config.getInstance().getTemporaryFolder());
		ent.setCategory(XDMUtils.findCategory(file));
		ent.setDate(System.currentTimeMillis());
		putInQueue(queueId, ent);
		ent.setStartedByUser(now);
		downloads.put(metadata.getId(), ent);
		saveDownloads();
		if (!now) {
			DownloadQueue q = queueManager.getQueueById(queueId);
			if (q != null && q.isRunning()) {
				Logger.log("Queue is running, if no pending download pickup next available download");
				q.next();
			}
		}
		if (now) {
			startDownload(metadata.getId(), metadata, ent, streamIndex);
		}
		notifyListeners(null);
	}

	// could be new or resume
	private void startDownload(String id, HttpMetadata metadata, DownloadEntry ent, int streams) {
		if (!checkAndBufferRequests(id)) {
			Logger.log("starting", id, " with:", metadata, " is dash:", (metadata instanceof DashMetadata));
			Downloader d = null;

			if (metadata instanceof DashMetadata) {
				Logger.log("Dash download with stream:", streams);
				if (streams == 1) {
					DashMetadata dm = (DashMetadata) metadata;
					dm.setUrl(dm.getUrl2());// set video url as main url
					dm.setUrl2(null);
				} else if (streams == 2) {
					DashMetadata dm = (DashMetadata) metadata;
					dm.setUrl2(null);
				} else {
					Logger.log("Dash download created");
					// create dash downloader
					DashMetadata dm = (DashMetadata) metadata;
					d = new DashDownloader(id, ent.getTempFolder(), dm);
				}
			}
			if (metadata instanceof HlsMetadata) {
				Logger.log("Hls download created");
				d = new HlsDownloader(id, ent.getTempFolder(), (HlsMetadata) metadata);
			}
			if (metadata instanceof HdsMetadata) {
				Logger.log("Hls download created");
				d = new HdsDownloader(id, ent.getTempFolder(), (HdsMetadata) metadata);
			}
			if (d == null) {
				if (metadata.getType() == XDMConstants.FTP) {
					d = new FtpDownloader(id, ent.getTempFolder(), metadata);
				} else {
					d = new HttpDownloader(id, ent.getTempFolder(), metadata);
				}
			}

			d.setOuputMediaFormat(ent.getOutputFormatIndex());
			downloaders.put(id, d);
			d.registerListener(this);
			ent.setState(XDMConstants.DOWNLOADING);
			d.start();

			if (Config.getInstance().showDownloadWindow()) {
				DownloadWindow wnd = new DownloadWindow(id, this);
				downloadWindows.put(id, wnd);
				wnd.setVisible(true);
			}
		} else {
			Logger.log(id, ": Maximum download limit reached, queueing request");
		}
	}

	public void pauseDownload(String id) {
		Downloader d = downloaders.get(id);
		if (d != null) {
			d.stop();
			d.unregisterListener();
		}
	}

	public void resumeDownload(String id, boolean startedByUser) {
		DownloadEntry ent = downloads.get(id);
		ent.setStartedByUser(startedByUser);
		if (ent.getState() == XDMConstants.PAUSED || ent.getState() == XDMConstants.FAILED) {
			if (!checkAndBufferRequests(id)) {
				ent.setState(XDMConstants.DOWNLOADING);
				HttpMetadata metadata = HttpMetadata.load(id);
				if (Config.getInstance().showDownloadWindow() && ent.isStartedByUser()) {
					DownloadWindow wnd = new DownloadWindow(id, this);
					downloadWindows.put(id, wnd);
					wnd.setVisible(true);
				}
				Downloader d = null;
				if (metadata instanceof DashMetadata) {
					DashMetadata dm = (DashMetadata) metadata;
					Logger.log("Dash download- url1:", dm.getUrl(), " url2:", dm.getUrl2());
					d = new DashDownloader(id, ent.getTempFolder(), dm);
				}
				if (metadata instanceof HlsMetadata) {
					HlsMetadata hm = (HlsMetadata) metadata;
					Logger.log("HLS download- url1:", hm.getUrl());
					d = new HlsDownloader(id, ent.getTempFolder(), hm);
				}
				if (metadata instanceof HdsMetadata) {
					HdsMetadata hm = (HdsMetadata) metadata;
					Logger.log("HLS download- url1:", hm.getUrl());
					d = new HdsDownloader(id, ent.getTempFolder(), hm);
				}
				if (d == null) {
					Logger.log("normal download");
					if (metadata.getType() == XDMConstants.FTP) {
						d = new FtpDownloader(id, ent.getTempFolder(), metadata);
					} else {
						d = new HttpDownloader(id, ent.getTempFolder(), metadata);
					}
				}
				downloaders.put(id, d);
				d.setOuputMediaFormat(ent.getOutputFormatIndex());
				d.registerListener(this);
				d.resume();

			} else {
				Logger.log(id, ": Maximum download limit reached, queueing request");
			}
			notifyListeners(null);
		}
	}

	public void restartDownload(String id) {
		DownloadEntry ent = downloads.get(id);
		if (ent.getState() == XDMConstants.PAUSED || ent.getState() == XDMConstants.FAILED
				|| ent.getState() == XDMConstants.FINISHED) {
			ent.setState(XDMConstants.PAUSED);
			clearData(ent);
			resumeDownload(id, true);
		} else {
			return;
		}
	}

	synchronized public void addListener(ListChangeListener listener) {
		listChangeListeners.add(listener);
	}

	synchronized public void removeListener(ListChangeListener listener) {
		listChangeListeners.remove(listener);
	}

	private void notifyListeners(String id) {
		if (listChangeListeners != null) {
			for (int i = 0; i < listChangeListeners.size(); i++)
				if (id != null)
					listChangeListeners.get(i).listItemUpdated(id);
				else
					listChangeListeners.get(i).listChanged();
		}
	}

	public DownloadEntry getEntry(String id) {
		return downloads.get(id);
	}

	public ArrayList<String> getDownloadList(int category,
	                                         int state,
	                                         String searchText,
	                                         String queueId) {
		Logger.log("Downloads category:", category,
				"state:", state,
				"searchText:", searchText,
				"queueId:", queueId);
		ArrayList<String> idList = new ArrayList<>();
		Iterator<String> keyIterator = downloads.keySet().iterator();
		while (keyIterator.hasNext()) {
			String key = keyIterator.next();
			DownloadEntry ent = downloads.get(key);
			if (state == XDMConstants.ALL
					|| state == (ent.getState() == XDMConstants.FINISHED
					? XDMConstants.FINISHED
					: XDMConstants.UNFINISHED)) {
				if (category == XDMConstants.ALL
						|| category == ent.getCategory()) {
					boolean matched = false;
					if (!"ALL".equals(queueId)) {
						if (queueId != null) {
							if (!queueId.equals(ent.getQueueId())) {
								continue;
							}
						}
					}

					if (searchText != null && searchText.length() > 0) {
						if (ent.getFile().contains(searchText)) {
							matched = true;
						}
					} else {
						matched = true;
					}
					if (matched) {
						idList.add(ent.getId());
					}
				}
			}
		}
		return idList;
	}

	private void clearData(DownloadEntry ent) {
		if (ent == null)
			return;
		File folder = new File(ent.getTempFolder(), ent.getId());
		File[] files = folder.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				files[i].delete();
			}
		}
		folder.delete();
	}

	@Override
	public String getOutputFolder(String id) {
		DownloadEntry ent = downloads.get(id);
		if (ent == null) {
			return Config.getInstance().getOtherFolder();
		} else {
			return getFolder(ent);
		}
	}

	@Override
	public String getOutputFile(String id, boolean update) {
		DownloadEntry ent = downloads.get(id);
		if (update) {
			updateFileName(ent);
		}
		return ent.getFile();
	}

	public String getFolder(DownloadEntry ent) {
		if (ent.getFolder() != null) {
			return ent.getFolder();
		}
		if (Config.getInstance().isForceSingleFolder()) {
			return Config.getInstance().getDownloadFolder();
		}
		int category = ent.getCategory();
		switch (category) {
			case XDMConstants.DOCUMENTS:
				return Config.getInstance().getDocumentsFolder();
			case XDMConstants.MUSIC:
				return Config.getInstance().getMusicFolder();
			case XDMConstants.VIDEO:
				return Config.getInstance().getVideosFolder();
			case XDMConstants.PROGRAMS:
				return Config.getInstance().getProgramsFolder();
			case XDMConstants.COMPRESSED:
				return Config.getInstance().getCompressedFolder();
			default:
				return Config.getInstance().getOtherFolder();
		}
	}

	private void loadDownloads() {
		loadDownloads(downloadsFile);
	}

	public void loadDownloads(File downloadsFile) {
		if (!downloadsFile.exists()) {
			Logger.log("No saved Downloads",
					downloadsFile.getAbsolutePath());
			return;
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		BufferedReader bufferedReader = null;
		try {
			Logger.log("Loading Downloads...",
					downloadsFile.getAbsolutePath());
			bufferedReader = XDMUtils.getBufferedReader(downloadsFile);
			int count = Integer.parseInt(bufferedReader.readLine().trim());
			for (int i = 0; i < count; i++) {
				int fieldCount = Integer.parseInt(bufferedReader.readLine().trim());
				DownloadEntry ent = new DownloadEntry();
				for (int j = 0; j < fieldCount; j++) {
					String ln = bufferedReader.readLine();
					if (ln == null) {
						return;
					}
					int index = ln.indexOf(":");
					if (index > 0) {
						String key = ln.substring(0, index).trim();
						String val = ln.substring(index + 1).trim();
						if (key.equals("id")) {
							ent.setId(val);
						}
						if (key.equals("queuesFile")) {
							ent.setFile(val);
						}
						if (key.equals("category")) {
							ent.setCategory(Integer.parseInt(val));
						}
						if (key.equals("state")) {
							int state = Integer.parseInt(val);
							ent.setState(state == XDMConstants.FINISHED ? state : XDMConstants.PAUSED);
						}
						if (key.equals("folder")) {
							ent.setFolder(val);
						}
						if (key.equals("date")) {
							ent.setDate(dateFormat.parse(val).getTime());
						}
						if (key.equals("downloaded")) {
							ent.setDownloaded(Long.parseLong(val));
						}
						if (key.equals("size")) {
							ent.setSize(Long.parseLong(val));
						}
						if (key.equals("progress")) {
							ent.setProgress(Integer.parseInt(val));
						}
						if (key.equals("queueid")) {
							ent.setQueueId(val);
						}
						if (key.equals("formatIndex")) {
							ent.setOutputFormatIndex(Integer.parseInt(val));
						}
						if (key.equals("tempfolder")) {
							ent.setTempFolder(val);
						}
					}
				}
				downloads.put(ent.getId(), ent);
			}
			bufferedReader.close();
		} catch (Exception e) {
			Logger.log(e);
		}
		try {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
		} catch (Exception e1) {
			Logger.log(e1);
		}
	}

	private void saveDownloads() {
		saveDownloads(downloadsFile);
	}

	public void saveDownloads(File downloadsFile) {
		int count = downloads.size();
		BufferedWriter bufferedWriter = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String newLine = System.getProperty("line.separator");
		try {
			Logger.log("Saving Downloads to", downloadsFile.getAbsolutePath());
			bufferedWriter = XDMUtils.getBufferedWriter(downloadsFile,
					false);
			bufferedWriter.write(count + "");
			bufferedWriter.newLine();
			Iterator<String> keyIterator = downloads.keySet().iterator();
			while (keyIterator.hasNext()) {
				String key = keyIterator.next();
				DownloadEntry ent = downloads.get(key);
				int c = 0;
				StringBuffer sb = new StringBuffer();
				sb.append("id: ").append(ent.getId()).append(newLine);
				c++;
				sb.append("queuesFile: ").append(ent.getFile()).append(newLine);
				c++;
				sb.append("category: ").append(ent.getCategory()).append(newLine);
				c++;
				sb.append("state: ").append(ent.getState()).append(newLine);
				c++;
				if (ent.getFolder() != null) {
					sb.append("folder: ").append(ent.getFolder()).append(newLine);
					c++;
				}
				sb.append("date: ").append(dateFormat.format(new Date(ent.getDate()))).append(newLine);
				c++;
				sb.append("downloaded: ").append(ent.getDownloaded()).append(newLine);
				c++;
				sb.append("size: ").append(ent.getSize()).append(newLine);
				c++;
				sb.append("progress: ").append(ent.getProgress()).append(newLine);
				c++;
				if (ent.getTempFolder() != null) {
					sb.append("tempfolder: ").append(ent.getTempFolder()).append(newLine);
					c++;
				}
				if (ent.getQueueId() != null) {
					sb.append("queueid: ").append(ent.getQueueId()).append(newLine);
					c++;
				}
				sb.append("formatIndex: ").append(ent.getOutputFormatIndex()).append(newLine);
				c++;
				bufferedWriter.write(c + newLine);
				bufferedWriter.write(sb.toString());

			}
			bufferedWriter.close();
		} catch (Exception e) {
			Logger.log(e);
			try {
				if (bufferedWriter != null)
					bufferedWriter.close();
			} catch (Exception e1) {
			}
		}
	}

	public void hidePrgWnd(String id) {
		DownloadWindow wnd = downloadWindows.get(id);
		if (wnd != null) {
			downloadWindows.remove(id);
			wnd.close(XDMConstants.PAUSED, 0);
		}
	}

	private synchronized int getActiveDownloadCount() {
		int count = 0;
		Iterator<String> keyIterator = downloads.keySet().iterator();
		while (keyIterator.hasNext()) {
			String key = keyIterator.next();
			DownloadEntry ent = downloads.get(key);
			int state = ent.getState();
			if (state == XDMConstants.FINISHED || state == XDMConstants.PAUSED || state == XDMConstants.FAILED)
				continue;
			count++;
		}
		return count;
	}

	private synchronized boolean checkAndBufferRequests(String id) {
		int actCount = getActiveDownloadCount();
		if (Config.getInstance().getMaxDownloads() > 0 && actCount >= Config.getInstance().getMaxDownloads()) {
			Logger.log("active:", actCount, " max:", Config.getInstance().getMaxDownloads());
			if (!pendingDownloads.contains(id)) {
				pendingDownloads.add(id);
			}
			return true;
		}
		return false;
	}

	private synchronized void processNextItem(String lastId) {
		processPendingRequests();
		if (lastId == null)
			return;
		DownloadEntry ent = getEntry(lastId);
		if (ent == null) {
			return;
		}
		DownloadQueue queue = null;
		if ("".equals(ent.getQueueId())) {
			queue = queueManager.getDefaultQueue();
		} else {
			queue = queueManager.getQueueById(ent.getQueueId());
		}
		if (queue != null && queue.isRunning()) {
			queue.next();
		}
	}

	private void processPendingRequests() {
		int activeCount = getActiveDownloadCount();
		int maxDownloadCount = Config.getInstance().getMaxDownloads();
		List<String> tobeStartedIds = new ArrayList<>();
		if (maxDownloadCount - activeCount > 0) {
			for (int i = 0; i < Math.min(maxDownloadCount, pendingDownloads.size()); i++) {
				String ent = pendingDownloads.get(i);
				tobeStartedIds.add(ent);
			}
		}
		if (tobeStartedIds.size() > 0) {
			for (int i = 0; i < tobeStartedIds.size(); i++) {
				String id = tobeStartedIds.get(i);
				pendingDownloads.remove(id);
				DownloadEntry ent = getEntry(id);
				if (ent != null) {
					resumeDownload(id, ent.isStartedByUser());
				}
			}
		}
	}

	public boolean queueItemPending(String queueId) {
		if (queueId == null)
			return false;
		for (int i = 0; i < pendingDownloads.size(); i++) {
			String id = pendingDownloads.get(i);
			DownloadEntry ent = getEntry(id);
			if (ent == null || ent.getQueueId() == null)
				continue;
			if (ent.getQueueId().equals(queueId)) {
				return true;
			}
		}
		return false;
	}

	public Collection<DownloadQueue> getDownloadQueues() {
		return queueManager.getDownloadQueues();
	}

	public DownloadQueue getQueueById(String queueId) {
		return queueManager.getQueueById(queueId);
	}

	private void putInQueue(String queueId, DownloadEntry ent) {
		DownloadQueue downloadQueue = getQueueById(queueId);
		String id = ent.getId();
		if (downloadQueue == null) {
			Logger.log("No queue found for:", queueId);
			return;
		}
		String qid = ent.getQueueId();
		DownloadQueue oldQ = getQueueById(qid);
		Logger.log("Adding to:", queueId);
		if (!downloadQueue.getQueueId().equals(qid)) {
			if (oldQ != null) {
				// remove from previous queue
				oldQ.removeFromQueue(id);
			}
			ent.setQueueId(queueId);
			downloadQueue.addToQueue(id);
		}
	}

	@Override
	public int compare(String key1, String key2) {
		DownloadEntry ent1 = getEntry(key1);
		DownloadEntry ent2 = getEntry(key2);
		if (ent1 == null)
			return -1;
		if (ent2 == null)
			return 1;
		if (ent1.getDate() > ent2.getDate()) {
			return 1;
		} else if (ent1.getDate() < ent2.getDate()) {
			return -1;
		} else {
			return 0;
		}
	}

	private Iterator<String> getDownloadIds() {
		return downloads.keySet().iterator();
	}

	public boolean isAllFinished() {
		if (getActiveDownloadCount() != 0) {
			return false;
		}
		if (pendingDownloads.size() != 0) {
			return false;
		}
		for (DownloadQueue downloadQueue : QueueManager.getInstance().getDownloadQueues()) {
			if (downloadQueue.hasPendingItems()) {
				return false;
			}
		}
		return true;
	}

	private void initShutdown() {
		Logger.log("Initiating shutdown");
		int os = XDMUtils.detectOS();
		if (os == XDMUtils.LINUX) {
			LinuxUtils.initShutdown();
		} else if (os == XDMUtils.WINDOWS) {
			WinUtils.initShutdown();
		} else if (os == XDMUtils.MAC) {
			MacUtils.initShutdown();
		}
	}

	public int deleteDownloads(ArrayList<String> ids, boolean outflie) {
		int c = 0;
		for (int i = 0; i < ids.size(); i++) {
			String id = ids.get(i);
			DownloadEntry ent = getEntry(id);
			if (ent != null) {
				if (ent.getState() == XDMConstants.FINISHED || ent.getState() == XDMConstants.PAUSED
						|| ent.getState() == XDMConstants.FAILED) {
					this.downloads.remove(id);
					pendingDownloads.remove(id);
					String qId = ent.getQueueId();
					if (qId != null) {
						DownloadQueue q = getQueueById(qId);
						if (q != null) {
							if (q.getQueueId().length() > 0) {
								q.removeFromQueue(id);
							}
						}
					}
					deleteFiles(ent, outflie);
					c++;
				}
			}
		}
		saveDownloads();
		notifyListeners(null);
		return ids.size() - c;
	}

	private void deleteFiles(DownloadEntry ent, boolean outfile) {
		// DownloadEntry ent = getEntry(id);
		if (ent == null)
			return;
		String id = ent.getId();
		Logger.log("Deleting metadata for", id);
		File mf = new File(Config.getInstance().getMetadataFolder(), id);
		boolean deleted = mf.delete();
		Logger.log("Deleted manifest", id, deleted);
		File df = new File(ent.getTempFolder(), id);
		File[] files = df.listFiles();
		if (files != null && files.length > 0) {
			for (File f : files) {
				deleted = f.delete();
				Logger.log("Deleted tmp queuesFile", id, deleted);
			}
		}
		deleted = df.delete();
		Logger.log("Deleted tmp folder", id, deleted);
		if (outfile) {
			File f = new File(XDMApp.getInstance().getFolder(ent), ent.getFile());
			f.delete();
		}
	}

	public void changeOutputFile(String id) {

	}

	public String getURL(String id) {
		try {
			HttpMetadata metadata = HttpMetadata.load(id);
			if (metadata instanceof DashMetadata) {
				DashMetadata dm = (DashMetadata) metadata;
				return dm.getUrl() + "\n" + dm.getUrl2();
			} else {
				return metadata.getUrl();
			}
		} catch (Exception e) {
			Logger.log(e);
		}
		return "";
	}

	public void registerRefreshCallback(LinkRefreshCallback callback) {
		this.refreshCallback = callback;
	}

	public void unregisterRefreshCallback() {
		this.refreshCallback = null;
	}

	public void deleteCompleted() {
		Iterator<String> allIds = downloads.keySet().iterator();
		ArrayList<String> idList = new ArrayList<>();
		while (allIds.hasNext()) {
			String id = allIds.next();
			DownloadEntry ent = downloads.get(id);
			if (ent.getState() == XDMConstants.FINISHED) {
				idList.add(id);
			}
		}
		deleteDownloads(idList, false);
	}

	public boolean promptCredential(String id, String msg, boolean proxy) {
		DownloadEntry ent = getEntry(id);
		if (ent == null)
			return false;
		if (!ent.isStartedByUser())
			return false;
		PasswordAuthentication pauth = getCredential(msg, proxy);
		if (pauth == null) {
			return false;
		}
		if (proxy) {
			Config.getInstance().setProxyUser(pauth.getUserName());
			if (pauth.getPassword() != null) {
				Config.getInstance().setProxyPass(new String(pauth.getPassword()));
			}
		} else {
			Logger.log("saving password for:", msg);
			CredentialManager.getInstance().addCredentialForHost(msg, pauth);
		}
		return true;
	}

	private PasswordAuthentication getCredential(String msg, boolean proxy) {
		JTextField user = new JTextField(30);
		JPasswordField pass = new JPasswordField(30);

		String prompt = proxy ? StringResource.get("PROMPT_PROXY")
				: String.format(StringResource.get("PROMPT_SERVER"), msg);

		Object[] obj = new Object[5];
		obj[0] = prompt;
		obj[1] = StringResource.get("DESC_USER");
		obj[2] = user;
		obj[3] = StringResource.get("DESC_PASS");
		obj[4] = pass;

		if (JOptionPane.showOptionDialog(null, obj, StringResource.get("PROMPT_CRED"), JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION) {
			PasswordAuthentication pauth = new PasswordAuthentication(user.getText(), pass.getPassword());
			return pauth;
		}
		return null;
	}

	private void execCmd() {
		if (!StringUtils.isNullOrEmptyOrBlank(Config.getInstance().getCustomCmd())) {
			XDMUtils.exec(Config.getInstance().getCustomCmd());
		}
	}

	private void execAntivirus() {
		String antivirusExeCmd = Config.getInstance().getAntivirusExeCmd();
		XDMUtils.exec(antivirusExeCmd);
	}

	private void updateFileName(DownloadEntry ent) {
		if (Config.getInstance().getDuplicateAction() == XDMConstants.DUP_ACT_OVERWRITE) {
			return;
		}
		Logger.log("checking for same named queuesFile on disk...");
		String id = ent.getId();
		String outputFolder = getOutputFolder(id);
		File f = new File(outputFolder, ent.getFile());
		int c = 1;
		String ext = XDMUtils.getExtension(f.getAbsolutePath());
		if (ext == null) {
			ext = "";
		}
		String f2 = XDMUtils.getFileNameWithoutExtension(ent.getFile());
		while (f.exists()) {
			// int index = f2.lastIndexOf("_");
			// if (index > 0) {
			// f2 = f2.substring(0, index);
			// }
			f = new File(outputFolder, f2 + "_" + c + ext);
			c++;
		}
		Logger.log("Updating queuesFile name- old:", ent.getFile(), " new:", f.getName());
		ent.setFile(f.getName());
	}

	public void importList(File file) {
		loadDownloads(file);
	}

	public void exportList(File file) {
		saveDownloads(file);
	}

	public void notifyComponentUpdate() {
		pendingNotification = UpdateChecker.COMP_UPDATE_AVAILABLE;
		if (mainWindow != null) {
			mainWindow.showNotification();
		}

		// if (mainWindow == null) {
		// pendingNotification = UpdateChecker.COMP_UPDATE_AVAILABLE;
		// } else {
		// mainWindow.showUpdateNotification(UpdateChecker.COMP_UPDATE_AVAILABLE);
		// }
	}

	public void notifyComponentInstall() {
		pendingNotification = UpdateChecker.COMP_NOT_INSTALLED;
		if (mainWindow != null) {
			mainWindow.showNotification();
		}

		// if (mainWindow == null) {
		// pendingNotification = UpdateChecker.COMP_NOT_INSTALLED;
		// } else {
		// mainWindow.showUpdateNotification(UpdateChecker.COMP_NOT_INSTALLED);
		// }
	}

	public void notifyAppUpdate() {
		pendingNotification = UpdateChecker.APP_UPDATE_AVAILABLE;
		if (mainWindow != null) {
			mainWindow.showNotification();
		}

		// if (mainWindow == null) {
		// pendingNotification = UpdateChecker.APP_UPDATE_AVAILABLE;
		// } else {
		// mainWindow.showUpdateNotification(UpdateChecker.APP_UPDATE_AVAILABLE);
		// }
	}

	public void clearNotifications() {
		pendingNotification = -1;
		if (mainWindow != null) {
			mainWindow.showNotification();
		}

		// if (mainWindow == null) {
		// pendingNotification = -1;
		// } else {
		// mainWindow.clearNotification();
		// }
	}

	public int getNotification() {
		return pendingNotification;
	}

	private void openTempFolder(String id) {
		DownloadEntry ent = getEntry(id);
		if (ent == null)
			return;
		File df = new File(ent.getTempFolder(), id);
		try {
			XDMUtils.openFolder(null, df.getAbsolutePath());
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	public void openPreview(String id) {
		DownloadEntry ent = XDMApp.getInstance().getEntry(id);
		if (ent != null && (ent.getCategory() == XDMConstants.VIDEO || ent.getCategory() == XDMConstants.MUSIC)) {
			if (FFmpeg.isFFmpegInstalled()) {
				XDMApp.getInstance().openPreviewPlayer(id);
			} else {
				JOptionPane.showMessageDialog(null, StringResource.get("LBL_COMPONENT_MISSING"));
			}
		} else if (JOptionPane.showConfirmDialog(null, StringResource.get("LBL_NOT_A_VIDEO"), "Preview",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			XDMApp.getInstance().openTempFolder(id);
		}
	}

	private void openPreviewPlayer(String id) {
		XDMUtils.browseURL("http://127.0.0.1:9614/preview/media/" + id);
	}

	public void showPrgWnd(String id) {
		DownloadEntry ent = getEntry(id);
		if (ent == null) {
			return;
		}
		if (ent.getState() == XDMConstants.FINISHED || ent.getState() == XDMConstants.PAUSED
				|| ent.getState() == XDMConstants.FAILED) {
			return;
		}
		DownloadWindow wnd = downloadWindows.get(id);
		if (wnd == null) {
			wnd = new DownloadWindow(id, this);
			downloadWindows.put(id, wnd);
			wnd.setVisible(true);
		}
	}

	public void fileNameChanged(String id) {
		notifyListeners(id);
	}

	public ArrayList<VideoPopupItem> getVideoItemsList() {
		return itemList;
	}

	public void addDownloadQueues(JMenuItem queueMenuItem,
	                              String name,
	                              Color foreground,
	                              ActionListener actionListener) {
		Collection<DownloadQueue> downloadQueues = getDownloadQueues();
		for (DownloadQueue downloadQueue : downloadQueues) {
			String text = downloadQueue.getName().isEmpty()
					? "Default queue"
					: downloadQueue.getName();
			JMenuItem jMenuItem = new JMenuItem(text);
			jMenuItem.setName(String.format("%s:%s",
					name,
					downloadQueue.getQueueId()));
			if (foreground != null) {
				jMenuItem.setForeground(foreground);
			}
			jMenuItem.addActionListener(actionListener);
			queueMenuItem.add(jMenuItem);
		}
	}
}