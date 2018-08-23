package xdman;

import xdman.util.*;
import xdman.util.os.OSUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Config implements FileManager {
	public static final int SORT_BY_DATE = 0;
	public static final int SORT_BY_SIZE = 1;
	public static final int SORT_BY_NAME = 2;
	public static final int SORT_BY_TYPE = 3;
	public static final int MIN_SEGMENT_SIZE = 256 * 1024;
	public static final int MIN_VID_SIZE = 1024 * 1024;
	public static final int MAX_SEGMENTS = 8;
	public static final int PARALLEL_DOWNLOADS = 100;
	public static final String LANGUAGE_EN = "en";
	public static final String[] FILE_TYPES = {
			"3GP",
			"7Z",
			"AVI",
			"BZ2",
			"DEB",
			"DOC",
			"DOCX",
			"EXE",
			"GZ",
			"ISO",
			"MSI",
			"PDF",
			"PPT",
			"PPTX",
			"RAR",
			"RPM",
			"XLS",
			"XLSX",
			"SIT",
			"SITX",
			"TAR",
			"JAR",
			"ZIP",
			"XZ"};
	public static final String[] BLOCKED_HOSTS = {
			"update.microsoft.com",
			"windowsupdate.com",
			"thwawte.com"};
	public static final String[] VIDEO_TYPES = {
			"MP4",
			"M3U8",
			"F4M",
			"WEBM",
			"OGG",
			"MP3",
			"AAC",
			"FLV",
			"MKV",
			"DIVX",
			"MOV",
			"MPG",
			"MPEG",
			"OPUS"
	};
	public static final String[] VID_URLS = {
			".facebook.com|pagelet",
			"player.vimeo.com/",
			"instagram.com/p/"
	};
	public static final int NETWORK_TIMEOUT = 60;
	public static final String NO_SOCKS_HOST = "";
	public static final boolean SHOW_VIDEO_NOTIFICATION = true;
	public static final boolean SHOW_DOWNLOAD_COMPLETE_WINDOW = true;
	public static final boolean FIRST_RUN = true;
	public static final boolean MONITOR_CLIPBOARD = true;
	public static final boolean NO_TRANSPARENCY = false;
	public static final boolean HIDE_TRAY = true;
	public static final boolean AUTO_SHUTDOWN = false;
	public static final boolean SHOW_DOWNLOAD_WINDOW = true;
	public static final boolean MONITORING = true;
	public static final String COMPRESSED = "Compressed";
	public static final String PROGRAMS = "Programs";
	public static final String TEMP = "temp";
	public static final String VIDEO = "Video";
	public static final String MUSIC = "Music";
	public static final String DOCUMENTS = "Documents";
	public static final String METADATA = "metadata";
	public static final String CONFIG_TXT = "config.txt";
	public static final String XDMAN = ".xdman";
	public static final String KEY_VALUE_DELIMITER = ":";
	private static final int DEFAULT_XDM_PORT = 9614;
	private static Config _config;
	private final FileUtils fileUtils;
	private Integer xdmPort;
	private boolean forceSingleFolder;
	private boolean monitoring = true;
	private File metadataDir;
	private File temporaryDir;
	private File downloadDir;
	private File dataDir;
	private File configFile;
	private int sortField;
	private boolean sortAsc;
	private int categoryFilter;
	private int stateFilter;
	private String searchText;
	private int maxSegments;
	private int minSegmentSize;
	private int speedLimit; // in kb/sec
	private boolean showDownloadWindow;
	private boolean showDownloadCompleteWindow;
	private int parallelDownloads;
	private boolean autoShutdown;
	private int duplicateAction;
	private String[] blockedHosts;
	private String[] vidUrls;
	private String[] fileExts;
	private String[] vidExts;
	private String[] defaultFileTypes;
	private String[] defaultVideoTypes;
	private int networkTimeout;
	private int tcpWindowSize;
	private int proxyMode;// 0 no-proxy,1 pac, 2 http, 3 socks
	private String proxyPac;
	private String proxyHost;
	private String socksHost;
	private int proxyPort;
	private int socksPort;
	private String proxyUser;
	private String proxyPass;
	private boolean showVideoNotification;
	private int minVidSize;
	private boolean keepAwake;
	private boolean execCmd;
	private boolean execAntivirus;
	private boolean autoStart;
	private String customCmd;
	private String antivirusCmd;
	private String antivirusExe;
	private boolean firstRun;
	private String language;
	private boolean monitorClipboard;
	private File otherDir;
	private File documentsDir;
	private File musicDir;
	private File videosDir;
	private File programsDir;
	private File compressedDir;
	private boolean downloadAutoStart;
	private boolean fetchTs;
	private boolean noTransparency;
	private boolean hideTray;
	private String lastFolder;
	private List<MonitoringListener> listeners;
	private String queueIdFilter;
	private boolean isTraceEnabled;

	private Config() {
		setXDMPort(DEFAULT_XDM_PORT);
		setForceSingleFolder(false);
		String userHome = System.getProperty("user.home");
		setDataDir(getDir(new File(userHome, XDMAN).getAbsolutePath()));
		setConfigFile(new File(getDataDir(), CONFIG_TXT));
		setMetadataFolder(new File(getDataDir(), METADATA).getAbsolutePath());
		setTemporaryFolder(new File(getDataDir(), TEMP).getAbsolutePath());

		setDownloadFolder(new File(OSUtils.getDownloadsFolder()).getAbsolutePath());
		setDocumentsFolder(new File(getDownloadFolder(), DOCUMENTS).getAbsolutePath());
		setMusicFolder(new File(getDownloadFolder(), MUSIC).getAbsolutePath());
		setVideosFolder(new File(getDownloadFolder(), VIDEO).getAbsolutePath());
		setProgramsFolder(new File(getDownloadFolder(), PROGRAMS).getAbsolutePath());
		setCompressedFolder(new File(getDownloadFolder(), COMPRESSED).getAbsolutePath());
		setOtherFolder(getDownloadFolder());

		setMonitoring(MONITORING);
		setShowDownloadWindow(SHOW_DOWNLOAD_WINDOW);
		setMaxSegments(MAX_SEGMENTS);
		setMinSegmentSize(MIN_SEGMENT_SIZE);
		setParallelDownloads(PARALLEL_DOWNLOADS);
		setMinVidSize(MIN_VID_SIZE);
		setDefaultFileTypes(FILE_TYPES);
		setFileExts(getDefaultFileTypes());
		setAutoShutdown(AUTO_SHUTDOWN);
		setBlockedHosts(BLOCKED_HOSTS);
		setDefaultVideoTypes(VIDEO_TYPES);
		setVidExts(getDefaultVideoTypes());
		setVidUrls(VID_URLS);
		setNetworkTimeout(NETWORK_TIMEOUT);
		setTcpWindowSize(0);
		setSpeedLimit(0);
		setProxyMode(0);
		setProxyPort(0);
		setSocksPort(0);
		setSocksHost(NO_SOCKS_HOST);
		setProxyPass(NO_SOCKS_HOST);
		setProxyUser(NO_SOCKS_HOST);
		setProxyHost(NO_SOCKS_HOST);
		setProxyPac(NO_SOCKS_HOST);
		setShowVideoNotification(SHOW_VIDEO_NOTIFICATION);
		setShowDownloadCompleteWindow(SHOW_DOWNLOAD_COMPLETE_WINDOW);
		setFirstRun(FIRST_RUN);
		setLanguage(LANGUAGE_EN);
		setMonitorClipboard(MONITOR_CLIPBOARD);
		setNoTransparency(NO_TRANSPARENCY);
		setHideTray(HIDE_TRAY);
		setListeners(new ArrayList<>());
		enabledTraceLogs(false);
		fileUtils = new UTF8FileUtils(getConfigFile(),
				"Config",
				this);
	}

	private static Config get_config() {
		return _config;
	}

	private static void set_config(Config _config) {
		Config._config = _config;
	}

	public static Config getInstance() {
		if (get_config() == null) {
			set_config(new Config());
		}
		return get_config();
	}

	private static File getDir(String folder) {
		if (folder == null) {
			return null;
		}
		File dir = new File(folder);
		if (!dir.exists()) {
			dir.mkdir();
		}
		return dir;
	}

	public int getXDMPort() {
		int port = xdmPort == null
				? DEFAULT_XDM_PORT
				: xdmPort;
		return port;
	}

	public void setXDMPort(int xdmPort) {
		this.xdmPort = xdmPort;
	}

	public void addConfigListener(MonitoringListener listener) {
		getListeners().add(listener);
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public boolean save() {
		return save(fileUtils);
	}

	public boolean save(FileUtils fileUtils) {
		boolean saved = fileUtils.save();
		return saved;
	}

	@Override
	public void save(BufferedWriter bufferedWriter,
	                 FileUtils fileUtils)
			throws IOException {
		fileUtils.save(bufferedWriter,
				"monitoring",
				isMonitoring());

		fileUtils.save(bufferedWriter,
				"downloadFolder",
				getDownloadFolder());

		fileUtils.save(bufferedWriter,
				"temporaryFolder",
				getTemporaryFolder());

		fileUtils.save(bufferedWriter,
				"parallelDownloads",
				getParallelDownloads());

		fileUtils.save(bufferedWriter,
				"maxSegments",
				getMaxSegments());

		fileUtils.save(bufferedWriter,
				"networkTimeout",
				getNetworkTimeout());

		fileUtils.save(bufferedWriter,
				"tcpWindowSize2",
				getTcpWindowSize());

		fileUtils.save(bufferedWriter,
				"minSegmentSize2",
				getMinSegmentSize());

		fileUtils.save(bufferedWriter,
				"minVidSize",
				getMinVidSize());

		fileUtils.save(bufferedWriter,
				"duplicateAction",
				getDuplicateAction());

		fileUtils.save(bufferedWriter,
				"speedLimit",
				getSpeedLimit());

		fileUtils.save(bufferedWriter,
				"showDownloadWindow",
				isShowDownloadWindow());

		fileUtils.save(bufferedWriter,
				"showDownloadCompleteWindow",
				isShowDownloadCompleteWindow());

		fileUtils.save(bufferedWriter,
				"blockedHosts",
				XDMUtils.appendArray2Str(getBlockedHosts()));

		fileUtils.save(bufferedWriter,
				"vidUrls",
				XDMUtils.appendArray2Str(getVidUrls()));

		fileUtils.save(bufferedWriter,
				"fileExts",
				XDMUtils.appendArray2Str(getFileExts()));

		fileUtils.save(bufferedWriter,
				"vidExts",
				XDMUtils.appendArray2Str(getVidExts()));

		fileUtils.save(bufferedWriter,
				"proxyMode",
				getProxyMode());

		fileUtils.save(bufferedWriter,
				"proxyPac",
				getProxyPac());

		fileUtils.save(bufferedWriter,
				"proxyHost",
				getProxyHost());

		fileUtils.save(bufferedWriter,
				"proxyPort",
				getProxyPort());

		fileUtils.save(bufferedWriter,
				"socksHost",
				getSocksHost());

		fileUtils.save(bufferedWriter,
				"socksPort",
				getSocksPort());

		fileUtils.save(bufferedWriter,
				"proxyUser",
				getProxyUser());

		fileUtils.save(bufferedWriter,
				"proxyPass",
				getProxyPass());

		fileUtils.save(bufferedWriter,
				"autoShutdown",
				isAutoShutdown());

		fileUtils.save(bufferedWriter,
				"keepAwake",
				isKeepAwake());

		fileUtils.save(bufferedWriter,
				"execCmd",
				isExecCmd());

		fileUtils.save(bufferedWriter,
				"execAntivir",
				isExecAntivirus());

		fileUtils.save(bufferedWriter,
				"version",
				XDMApp.APP_VERSION);

		fileUtils.save(bufferedWriter,
				"autoStart",
				isAutoStart());

		fileUtils.save(bufferedWriter,
				"language",
				getLanguage());

		fileUtils.save(bufferedWriter,
				"downloadAutoStart",
				isDownloadAutoStart());

		fileUtils.save(bufferedWriter,
				"antivirExe",
				getAntivirusExe());

		fileUtils.save(bufferedWriter,
				"antivirCmd",
				getAntivirusCmd());

		fileUtils.save(bufferedWriter,
				"customCmd",
				getCustomCmd());

		fileUtils.save(bufferedWriter,
				"showVideoNotification",
				isShowVideoNotification());

		fileUtils.save(bufferedWriter,
				"monitorClipboard",
				isMonitorClipboard());

		fileUtils.save(bufferedWriter,
				"categoryOther",
				getOtherFolder());

		fileUtils.save(bufferedWriter,
				"compressedFolder",
				getCompressedFolder());

		fileUtils.save(bufferedWriter,
				"documentsFolder",
				getDocumentsFolder());

		fileUtils.save(bufferedWriter,
				"musicFolder",
				getMusicFolder());

		fileUtils.save(bufferedWriter,
				"videosFolder",
				getVideosFolder());

		fileUtils.save(bufferedWriter,
				"programsFolder",
				getProgramsFolder());

		fileUtils.save(bufferedWriter,
				"fetchTs",
				isFetchTs());

		fileUtils.save(bufferedWriter,
				"noTransparency",
				isNoTransparency());

		fileUtils.save(bufferedWriter,
				"forceSingleFolder",
				isForceSingleFolder());

		fileUtils.save(bufferedWriter,
				"hideTray",
				isHideTray());

		fileUtils.save(bufferedWriter,
				"lastFolder",
				getLastFolder());
	}

	@Override
	public void saveFinally(FileUtils fileUtils,
	                        boolean saved) {
		Logger.log(fileUtils,
				"saved",
				saved);
	}

	public boolean load() {
		return load(fileUtils);
	}

	public boolean load(FileUtils fileUtils) {
		boolean loaded = fileUtils.load();
		return loaded;
	}

	@Override
	public boolean parse(BufferedReader bufferedReader,
	                     FileUtils fileUtils)
			throws IOException,
			ParseException {
		boolean loaded = fileUtils.parseLines(bufferedReader,
				this);
		return loaded;
	}

	@Override
	public boolean parseLine(long lineIndex,
	                         String line) throws ParseException {
		boolean loaded = fileUtils.parseLine(lineIndex,
				line,
				this);
		return loaded;
	}

	@Override
	public boolean parse(long lineIndex,
	                     String key,
	                     String value) throws ParseException {
		switch (key) {
			case "monitoring":
				setMonitoring(value.equals("true"));
				return true;
			case "downloadFolder":
				setDownloadFolder(value);
				return true;
			case "temporaryFolder":
				setTemporaryFolder(value);
				return true;
			case "maxSegments":
				setMaxSegments(Integer.parseInt(value));
				return true;
			case "minSegmentSize2":
				setMinSegmentSize(Integer.parseInt(value));
				return true;
			case "networkTimeout":
				setNetworkTimeout(Integer.parseInt(value));
				return true;
			case "tcpWindowSize2":
				setTcpWindowSize(Integer.parseInt(value));
				return true;
			case "duplicateAction":
				setDuplicateAction(Integer.parseInt(value));
				return true;
			case "speedLimit":
				setSpeedLimit(Integer.parseInt(value));
				return true;
			case "showDownloadWindow":
				setShowDownloadWindow(value.equals("true"));
				return true;
			case "showDownloadCompleteWindow":
				setShowDownloadCompleteWindow(value.equals("true"));
				return true;
			case "downloadAutoStart":
				setDownloadAutoStart(value.equals("true"));
				return true;
			case "minVidSize":
				setMinVidSize(Integer.parseInt(value));
				return true;
			case "parallelDownloads":
				setParallelDownloads(Integer.parseInt(value));
				return true;
			case "blockedHosts":
				setBlockedHosts(value.split(","));
				return true;
			case "vidUrls":
				setVidUrls(value.split(","));
				return true;
			case "fileExts":
				setFileExts(value.split(","));
				return true;
			case "vidExts":
				setVidExts(value.split(","));
				return true;
			case "proxyMode":
				setProxyMode(Integer.parseInt(value));
				return true;
			case "proxyPort":
				setProxyPort(Integer.parseInt(value));
				return true;
			case "socksPort":
				setSocksPort(Integer.parseInt(value));
				return true;
			case "proxyPac":
				setProxyPac(value);
				return true;
			case "proxyHost":
				setProxyHost(value);
				return true;
			case "socksHost":
				setSocksHost(value);
				return true;
			case "proxyUser":
				setProxyUser(value);
				return true;
			case "proxyPass":
				setProxyPass(value);
				return true;
			case "showVideoNotification":
				setShowVideoNotification("true".equals(value));
				return true;
			case "keepAwake":
				setKeepAwake("true".equals(value));
				return true;
			case "autoStart":
				setAutoStart("true".equals(value));
				return true;
			case "execAntivir":
				setExecAntivirus("true".equals(value));
				return true;
			case "execCmd":
				setExecCmd("true".equals(value));
				return true;
			case "antivirExe":
				setAntivirusExe(value);
				return true;
			case "antivirCmd":
				setAntivirusCmd(value);
				return true;
			case "customCmd":
				setCustomCmd(value);
				return true;
			case "autoShutdown":
				setAutoShutdown("true".equals(value));
				return true;
			case "version":
				setFirstRun(!XDMApp.APP_VERSION.equals(value));
				return true;
			case "language":
				setLanguage(value);
				return true;
			case "monitorClipboard":
				setMonitorClipboard("true".equals(value));
				return true;
			case "categoryOther":
				setOtherFolder(value);
				return true;
			case "documentsFolder":
				setDocumentsFolder(value);
				return true;
			case "compressedFolder":
				setCompressedFolder(value);
				return true;
			case "musicFolder":
				setMusicFolder(value);
				return true;
			case "videosFolder":
				setVideosFolder(value);
				return true;
			case "programsFolder":
				setProgramsFolder(value);
				return true;
			case "fetchTs":
				setFetchTs("true".equals(value));
				return true;
			case "noTransparency":
				setNoTransparency("true".equals(value));
				return true;
			case "forceSingleFolder":
				setForceSingleFolder("true".equals(value));
				return true;
			case "hideTray":
				setHideTray("true".equals(value));
				return true;
			case "lastFolder":
				setLastFolder(value);
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
		if (!isForceSingleFolder()) {
			createFolders();
		}
		Logger.log(fileUtils,
				"loaded",
				loaded);
	}

	public void createFolders() {
		Logger.log("Creating folders");
		getDocumentsFolder();
		getMusicFolder();
		getCompressedFolder();
		getProgramsFolder();
		getVideosFolder();
	}

	public final String getMetadataFolder() {
		if (getMetadataDir() == null) {
			setMetadataFolder(new File(getDataDir(), METADATA).getAbsolutePath());
		}
		return getMetadataDir().getAbsolutePath();
	}

	private void setMetadataFolder(String metadataFolder) {
		setMetadataDir(getDir(metadataFolder));
	}

	public final String getDataFolder() {
		return getDataDir().getAbsolutePath();
	}

	public int getX() {
		return -1;
	}

	public int getY() {
		return -1;
	}

	public int getWidth() {
		return -1;
	}

	public int getHeight() {
		return -1;
	}

	public boolean getSortAsc() {
		return isSortAsc();
	}

	public boolean isBrowserMonitoringEnabled() {
		return isMonitoring();
	}

	public void enableMonitoring(boolean enable) {
		setMonitoring(enable);
		for (MonitoringListener mon : getListeners()) {
			if (mon != null) {
				mon.configChanged();
			}
		}
	}

	public int getSortField() {
		return sortField;
	}

	public void setSortField(int sortField) {
		this.sortField = sortField;
	}

	public int getCategoryFilter() {
		return categoryFilter;
	}

	public void setCategoryFilter(int categoryFilter) {
		this.categoryFilter = categoryFilter;
	}

	public int getStateFilter() {
		return stateFilter;
	}

	public void setStateFilter(int stateFilter) {
		this.stateFilter = stateFilter;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

	public String getDownloadFolder() {
		if (getDownloadDir() == null) {
			setDownloadFolder(new File(OSUtils.getDownloadsFolder()).getAbsolutePath());
		}
		return getDownloadDir().getAbsolutePath();
	}

	public void setDownloadFolder(String downloadFolder) {
		this.setDownloadDir(getDir(downloadFolder));
	}

	public int getMaxSegments() {
		return maxSegments;
	}

	public void setMaxSegments(int maxSegments) {
		this.maxSegments = maxSegments;
	}

	public int getMinSegmentSize() {
		return minSegmentSize;
	}

	private void setMinSegmentSize(int minSegmentSize) {
		this.minSegmentSize = minSegmentSize;
	}

	public final int getSpeedLimit() {
		return speedLimit;
	}

	public final void setSpeedLimit(int speedLimit) {
		this.speedLimit = speedLimit;
	}

	public final boolean showDownloadWindow() {
		return isShowDownloadWindow();
	}

	public final int getMaxDownloads() {
		return getParallelDownloads();
	}

	public final void setMaxDownloads(int maxDownloads) {
		this.setParallelDownloads(maxDownloads);
	}

	public final boolean isAutoShutdown() {
		return autoShutdown;
	}

	public final void setAutoShutdown(boolean autoShutdown) {
		this.autoShutdown = autoShutdown;
	}

	public String[] getBlockedHosts() {
		return blockedHosts;
	}

	public void setBlockedHosts(String[] blockedHosts) {
		this.blockedHosts = blockedHosts;
	}

	public String[] getVidUrls() {
		return vidUrls;
	}

	private void setVidUrls(String[] vidUrls) {
		this.vidUrls = vidUrls;
	}

	public String[] getFileExts() {
		return fileExts;
	}

	public void setFileExts(String[] fileExts) {
		this.fileExts = fileExts;
	}

	public String[] getVidExts() {
		return vidExts;
	}

	public void setVidExts(String[] vidExts) {
		this.vidExts = vidExts;
	}

	public final boolean showDownloadCompleteWindow() {
		return isShowDownloadCompleteWindow();
	}

	public final int getDuplicateAction() {
		return duplicateAction;
	}

	public final void setDuplicateAction(int duplicateAction) {
		this.duplicateAction = duplicateAction;
	}

	public final String[] getDefaultFileTypes() {
		return defaultFileTypes;
	}

	private final void setDefaultFileTypes(String[] defaultFileTypes) {
		this.defaultFileTypes = defaultFileTypes;
	}

	public final String[] getDefaultVideoTypes() {
		return defaultVideoTypes;
	}

	private final void setDefaultVideoTypes(String[] defaultVideoTypes) {
		this.defaultVideoTypes = defaultVideoTypes;
	}

	public final int getNetworkTimeout() {
		return networkTimeout;
	}

	public final void setNetworkTimeout(int networkTimeout) {
		this.networkTimeout = networkTimeout;
	}

	public final int getTcpWindowSize() {
		return tcpWindowSize;
	}

	public final void setTcpWindowSize(int tcpWindowSize) {
		this.tcpWindowSize = tcpWindowSize;
	}

	public final int getProxyMode() {
		return proxyMode;
	}

	public final void setProxyMode(int proxyMode) {
		this.proxyMode = proxyMode;
	}

	public final String getProxyUser() {
		return proxyUser;
	}

	public final void setProxyUser(String proxyUser) {
		this.proxyUser = proxyUser;
	}

	public final String getProxyPass() {
		return proxyPass;
	}

	public final void setProxyPass(String proxyPass) {
		this.proxyPass = proxyPass;
	}

	public final String getProxyPac() {
		return proxyPac;
	}

	public final void setProxyPac(String proxyPac) {
		this.proxyPac = proxyPac;
	}

	public final String getProxyHost() {
		return proxyHost;
	}

	public final void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public final int getProxyPort() {
		return proxyPort;
	}

	public final void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public boolean isShowVideoNotification() {
		return showVideoNotification;
	}

	public void setShowVideoNotification(boolean showVideoNotification) {
		this.showVideoNotification = showVideoNotification;
	}

	public int getMinVidSize() {
		return minVidSize;
	}

	public void setMinVidSize(int minVidSize) {
		this.minVidSize = minVidSize;
	}

	public String getSocksHost() {
		return socksHost;
	}

	public void setSocksHost(String socksHost) {
		this.socksHost = socksHost;
	}

	public int getSocksPort() {
		return socksPort;
	}

	public void setSocksPort(int socksPort) {
		this.socksPort = socksPort;
	}

	public boolean isKeepAwake() {
		return keepAwake;
	}

	public void setKeepAwake(boolean keepAwake) {
		this.keepAwake = keepAwake;
	}

	public boolean isExecCmd() {
		return execCmd;
	}

	public void setExecCmd(boolean execCmd) {
		this.execCmd = execCmd;
	}

	public boolean isExecAntivirus() {
		return execAntivirus;
	}

	public void setExecAntivirus(boolean execAntivirus) {
		this.execAntivirus = execAntivirus;
	}

	private boolean isAutoStart() {
		return autoStart;
	}

	private void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	public String getCustomCmd() {
		return customCmd;
	}

	public void setCustomCmd(String customCmd) {
		this.customCmd = customCmd;
	}

	public String getAntivirusCmd() {
		return antivirusCmd;
	}

	public void setAntivirusCmd(String antivirusCmd) {
		this.antivirusCmd = antivirusCmd;
	}

	public String getAntivirusExe() {
		return antivirusExe;
	}

	public void setAntivirusExe(String antivirusExe) {
		this.antivirusExe = antivirusExe;
	}

	String getAntivirusExeCmd() {
		String antivirusExe = getAntivirusExe();
		String antivirusCmd = getAntivirusCmd() == null
				? ""
				: getAntivirusCmd();
		return String.format("%s %s", antivirusExe,
				antivirusCmd);
	}

	public boolean isFirstRun() {
		return firstRun;
	}

	private void setFirstRun(boolean firstRun) {
		this.firstRun = firstRun;
	}

	public boolean isMonitorClipboard() {
		return monitorClipboard;
	}

	public void setMonitorClipboard(boolean monitorClipboard) {
		this.monitorClipboard = monitorClipboard;
	}

	public void addBlockedHosts(String host) {
		List<String> list = new ArrayList<>(Arrays.asList(getBlockedHosts()));
		if (list.contains(host)) {
			return;
		}
		list.add(host);
		setBlockedHosts(list.toArray(new String[list.size()]));
	}

	public String getOtherFolder() {
		if (this.getOtherDir() == null) {
			setOtherFolder(getDownloadFolder());
		}
		return this.getOtherDir().getAbsolutePath();
	}

	public void setOtherFolder(String otherFolder) {
		this.setOtherDir(getDir(otherFolder));
	}

	public String getDocumentsFolder() {
		if (this.getDocumentsDir() == null) {
			setDocumentsFolder(new File(getDownloadFolder(), DOCUMENTS).getAbsolutePath());
		}
		return getDocumentsDir().getAbsolutePath();
	}

	public void setDocumentsFolder(String documentsFolder) {
		this.setDocumentsDir(getDir(documentsFolder));
	}

	public String getMusicFolder() {
		if (this.getMusicDir() == null) {
			setMusicFolder(new File(getDownloadFolder(), MUSIC).getAbsolutePath());
			getMusicDir().mkdirs();
		}
		return getMusicDir().getAbsolutePath();
	}

	public void setMusicFolder(String musicFolder) {
		this.setMusicDir(getDir(musicFolder));
	}

	public String getVideosFolder() {
		if (this.getVideosDir() == null) {
			setVideosFolder(new File(getDownloadFolder(), VIDEO).getAbsolutePath());
		}
		return getVideosDir().getAbsolutePath();
	}

	public void setVideosFolder(String videosFolder) {
		this.setVideosDir(getDir(videosFolder));
	}

	public final String getTemporaryFolder() {
		if (this.getTemporaryDir() == null) {
			setTemporaryFolder(new File(getDataDir(), TEMP).getAbsolutePath());
		}
		return getTemporaryDir().getAbsolutePath();
	}

	public void setTemporaryFolder(String temporaryFolder) {
		this.setTemporaryDir(getDir(temporaryFolder));
	}

	public String getProgramsFolder() {
		if (this.getProgramsDir() == null) {
			setProgramsFolder(new File(getDownloadFolder(), PROGRAMS).getAbsolutePath());
		}
		return getProgramsDir().getAbsolutePath();
	}

	public void setProgramsFolder(String programsFolder) {
		this.setProgramsDir(getDir(programsFolder));
	}

	public String getCompressedFolder() {
		if (this.getCompressedDir() == null) {
			setCompressedFolder(new File(getDownloadFolder(), COMPRESSED).getAbsolutePath());
		}
		return getCompressedDir().getAbsolutePath();
	}

	public void setCompressedFolder(String compressedFolder) {
		this.setCompressedDir(getDir(compressedFolder));
	}

	public boolean isDownloadAutoStart() {
		return downloadAutoStart;
	}

	public void setDownloadAutoStart(boolean downloadAutoStart) {
		this.downloadAutoStart = downloadAutoStart;
	}

	public boolean isFetchTs() {
		return fetchTs;
	}

	public void setFetchTs(boolean fetchTs) {
		this.fetchTs = fetchTs;
	}

	public boolean isNoTransparency() {
		return noTransparency;
	}

	public void setNoTransparency(boolean noTransparency) {
		this.noTransparency = noTransparency;
	}

	public boolean isForceSingleFolder() {
		return forceSingleFolder;
	}

	public void setForceSingleFolder(boolean forceSingleFolder) {
		this.forceSingleFolder = forceSingleFolder;
	}

	public boolean isHideTray() {
		return hideTray;
	}

	public void setHideTray(boolean hideTray) {
		this.hideTray = hideTray;
	}

	public String getLastFolder() {
		return lastFolder;
	}

	public void setLastFolder(String lastFolder) {
		this.lastFolder = lastFolder;
	}

	public String getQueueIdFilter() {
		return queueIdFilter;
	}

	public void setQueueIdFilter(String queueIdFilter) {
		this.queueIdFilter = queueIdFilter;
	}

	private File getDataDir() {
		return dataDir;
	}

	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	private File getConfigFile() {
		return configFile;
	}

	public void setConfigFile(File configFile) {
		this.configFile = configFile;
	}

	public boolean isMonitoring() {
		return monitoring;
	}

	public void setMonitoring(boolean monitoring) {
		this.monitoring = monitoring;
	}

	private File getMetadataDir() {
		return metadataDir;
	}

	private void setMetadataDir(File metadataDir) {
		this.metadataDir = metadataDir;
	}

	private File getTemporaryDir() {
		return temporaryDir;
	}

	private void setTemporaryDir(File temporaryDir) {
		this.temporaryDir = temporaryDir;
	}

	private File getDownloadDir() {
		return downloadDir;
	}

	private void setDownloadDir(File downloadDir) {
		this.downloadDir = downloadDir;
	}

	private boolean isSortAsc() {
		return sortAsc;
	}

	public void setSortAsc(boolean sortAsc) {
		this.sortAsc = sortAsc;
	}

	private boolean isShowDownloadWindow() {
		return showDownloadWindow;
	}

	public final void setShowDownloadWindow(boolean show) {
		this.showDownloadWindow = show;
	}

	private boolean isShowDownloadCompleteWindow() {
		return showDownloadCompleteWindow;
	}

	public final void setShowDownloadCompleteWindow(boolean show) {
		this.showDownloadCompleteWindow = show;
	}

	private int getParallelDownloads() {
		return parallelDownloads;
	}

	private void setParallelDownloads(int parallelDownloads) {
		this.parallelDownloads = parallelDownloads;
	}

	private File getOtherDir() {
		return otherDir;
	}

	private void setOtherDir(File otherDir) {
		this.otherDir = otherDir;
	}

	private File getDocumentsDir() {
		return documentsDir;
	}

	private void setDocumentsDir(File documentsDir) {
		this.documentsDir = documentsDir;
	}

	private File getMusicDir() {
		return musicDir;
	}

	private void setMusicDir(File musicDir) {
		this.musicDir = musicDir;
	}

	private File getVideosDir() {
		return videosDir;
	}

	private void setVideosDir(File videosDir) {
		this.videosDir = videosDir;
	}

	private File getProgramsDir() {
		return programsDir;
	}

	private void setProgramsDir(File programsDir) {
		this.programsDir = programsDir;
	}

	private List<MonitoringListener> getListeners() {
		return listeners;
	}

	private void setListeners(List<MonitoringListener> listeners) {
		this.listeners = listeners;
	}

	private File getCompressedDir() {
		return compressedDir;
	}

	private void setCompressedDir(File compressedDir) {
		this.compressedDir = compressedDir;
	}

	public boolean isTraceLogsEnabled() {
		return Logger.isTraceEnabled();
	}

	public void enabledTraceLogs(boolean isTraceEnabled) {
		Logger.enabledTrace(isTraceEnabled);
	}
}
