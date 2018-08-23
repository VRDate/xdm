package xdman.util;

import xdman.Config;
import xdman.Main;
import xdman.XDMApp;
import xdman.network.http.JavaHttpClient;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Comparator;

public class UpdateChecker implements Comparator<String> {
	public static final int APP_UPDATE_AVAILABLE = 10,
			COMP_UPDATE_AVAILABLE = 20,
			COMP_NOT_INSTALLED = 30,
			NO_UPDATE_AVAILABLE = 40;
	private static final String APP_UPDATE_URL = "http://xdman.sourceforge.net/update/update_check.php",
			COMPONENTS_UPDATE_URL = "http://xdman.sourceforge.net/components/update_check.php";

	public static int getUpdateStat() {

		Integer isComponentUpdateAvailable = isComponentUpdateAvailable();
		Logger.log("isComponentUpdateAvailable: ", isComponentUpdateAvailable);
		if (isComponentUpdateAvailable == COMP_UPDATE_AVAILABLE
				|| isComponentUpdateAvailable == COMP_NOT_INSTALLED) {
			return isComponentUpdateAvailable;
		} else {
			Logger.log("checking for app update");
			int isAppUpdateAvailable = isAppUpdateAvailable();
			Logger.log("isAppUpdateAvailable: ", isAppUpdateAvailable);
			return isAppUpdateAvailable;
		}
	}

	private static int isAppUpdateAvailable() {
		String currentAppVersion = XDMApp.APP_VERSION;
		Logger.log("Current App version:", currentAppVersion);
		int isUpdateAvailable = isUpdateAvailable(APP_UPDATE_URL,
				currentAppVersion,
				APP_UPDATE_AVAILABLE);
		return isUpdateAvailable;
	}

	// return 1 is no update required
	// return 0, -1 if update required
	private static Integer isComponentUpdateAvailable() {
		String componentVersion = getComponentVersion();
		Logger.log("Current component version:", componentVersion);
		if (componentVersion == null)
			return COMP_NOT_INSTALLED;
		int isUpdateAvailable = isUpdateAvailable(COMPONENTS_UPDATE_URL,
				componentVersion,
				COMP_UPDATE_AVAILABLE);
		return isUpdateAvailable;
	}

	public static String getComponentVersion() {
		File f = new File(Config.getInstance().getDataFolder());
		String[] files = f.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".version");
			}
		});
		if (files.length < 1) {
			Logger.log("Component not installed");
			Logger.log("Checking fallback components");
			return getFallbackComponentVersion();
		}
		return files[0].split("\\.")[0];
	}

	public static String getFallbackComponentVersion() {
		File jarFile = FileUtils.getJarFile(Main.class);
		File f = jarFile.getParentFile();
		String[] files = f.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".version");
			}
		});
		if (files.length < 1) {
			Logger.log("Component not installed");
			return null;
		}
		return files[0].split("\\.")[0];
	}

	private static int isUpdateAvailable(String updateURL,
	                                     String version,
	                                     int updateAvailable) {
		JavaHttpClient client = null;
		try {
			String url = String.format("%s?ver=%s",
					updateURL,
					version);
			Logger.log("isUpdateAvailable", url, version);
			client = new JavaHttpClient(url);
			client.setFollowRedirect(true);
			client.connect();
			int resp = client.getStatusCode();
			Logger.log("manifest download response:", resp);
			if (resp == 200) {
				InputStream in = client.getInputStream();
				StringBuffer sb = new StringBuffer();
				int x;
				while ((x = in.read()) != -1) {
					sb.append((char) x);
				}
				String latestAppVersion = sb.toString();
				String currentAppVersion = XDMApp.APP_VERSION;
				Boolean isNewerVersion = isNewer(latestAppVersion,
						currentAppVersion);
				int isUpdateAvailable = isNewerVersion
						? updateAvailable
						: NO_UPDATE_AVAILABLE;
				return isUpdateAvailable;
			}
		} catch (UnknownHostException e) {
			Logger.log(e);
			return NO_UPDATE_AVAILABLE;
		} catch (Exception e) {
			Logger.log(e);
		} finally {
			try {
				client.dispose();
			} catch (Exception e) {
				Logger.log(e);
			}
		}
		return NO_UPDATE_AVAILABLE;
	}

	private static Boolean isNewer(String latestAppVersion,
	                               String currentAppVersion) {
		UpdateChecker updateChecker = new UpdateChecker();
		Boolean isNewerVersion = updateChecker.isNewerVersion(latestAppVersion,
				currentAppVersion);
		return isNewerVersion;
	}

	private Boolean isNewerVersion(String latestAppVersion,
	                               String currentAppVersion) {
		int newerVersion = compare(latestAppVersion,
				currentAppVersion);
		boolean isNewerVersion = isNewerVersion(newerVersion);
		return isNewerVersion;
	}

	@Override
	public int compare(String latestAppVersion,
	                   String currentAppVersion) {
		int newerVersion;
		try {
			if (latestAppVersion.indexOf(".") > 0 && currentAppVersion.indexOf(".") > 0) {
				String[] latestAppVersionStrings = latestAppVersion.split("\\.");
				String[] currentAppVersionStrings = currentAppVersion.split("\\.");
				int versionsMinLength = Math.min(latestAppVersionStrings.length,
						currentAppVersionStrings.length);
				for (int versionIndex = 0; versionIndex < versionsMinLength; versionIndex++) {
					int latestAppVersionInteger = Integer.parseInt(latestAppVersionStrings[versionIndex]);
					int currentAppVersionInteger = Integer.parseInt(currentAppVersionStrings[versionIndex]);
					if (latestAppVersionInteger > currentAppVersionInteger) {
						newerVersion = latestAppVersionInteger - currentAppVersionInteger;
						boolean isNewerVersion = isNewerVersion(newerVersion);
						Logger.log("Compared Versions",
								latestAppVersion,
								"vs",
								currentAppVersion,
								versionIndex,
								latestAppVersionInteger,
								"vs",
								currentAppVersionInteger,
								newerVersion,
								isNewerVersion);
						return newerVersion;
					}
				}
			}
			newerVersion = 0;
			boolean isNewerVersion = isNewerVersion(newerVersion);
			Logger.log("Compared Versions",
					latestAppVersion,
					"vs",
					currentAppVersion,
					newerVersion,
					isNewerVersion);
			return newerVersion;
		} catch (Exception e) {
			newerVersion = -2;
			boolean isNewerVersion = isNewerVersion(newerVersion);
			Logger.log("Error comparing Versions",
					latestAppVersion,
					"vs",
					currentAppVersion,
					newerVersion,
					isNewerVersion,
					e);
			return newerVersion;
		}
	}

	private boolean isNewerVersion(int newerVersion) {
		return newerVersion > 0;
	}
}
