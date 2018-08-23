package xdman.util.os;

import xdman.Config;
import xdman.XDMConstants;
import xdman.util.Logger;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class OSUtils {
	public static final String DOWNLOADS = "Downloads";
	public static final int WINDOWS = 10;
	public static final int MAC = 20;
	public static final int LINUX = 30;
	private static int screenType = -1;

	public static final int detectOS() {
		String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
		if (os.contains("mac")
				|| os.contains("darwin")
				|| os.contains("os x")) {
			return MAC;
		} else if (os.contains("linux")) {
			return LINUX;
		} else if (os.contains("windows")) {
			return WINDOWS;
		} else {
			return -1;
		}
	}

	public static final int getOsArch() {
		if (System.getProperty("os.arch").contains("64")) {
			return 64;
		} else {
			return 32;
		}
	}

	public static void openFile(String file, String folder) throws Exception {
		int os = detectOS();
		File f = new File(folder, file);
		switch (os) {
			case WINDOWS:
				WinUtils.open(f);
				break;
			case LINUX:
				LinuxUtils.open(f);
				break;
			case MAC:
				MacUtils.open(f);
				break;
			default:
				Desktop.getDesktop().open(f);
		}
	}

	public static void openFolder(String file, String folder) throws Exception {
		int os = detectOS();
		switch (os) {
			case WINDOWS:
				WinUtils.openFolder(folder, file);
				break;
			case LINUX:
				File f = new File(folder);
				LinuxUtils.open(f);
				break;
			case MAC:
				MacUtils.openFolder(folder, file);
				break;
			default:
				File ff = new File(folder);
				Desktop.getDesktop().open(ff);
		}
	}

	public static boolean exec(String args) {
		try {
			Logger.log("Launching: " + args);
			Runtime.getRuntime().exec(args);
		} catch (IOException e) {
			Logger.log(e);
			return false;
		}
		return true;
	}

	public static long getFreeSpace(String folder) {
		if (folder == null)
			return new File(Config.getInstance().getTemporaryFolder()).getFreeSpace();
		else
			return new File(folder).getFreeSpace();
	}

	public static void addToStartup() {
		try {
			int os = detectOS();
			if (os == LINUX) {
				LinuxUtils.addToStartup();
			} else if (os == WINDOWS) {
				WinUtils.addToStartup();
			} else if (os == MAC) {
				MacUtils.addToStartup();
			}
		} catch (Throwable e) {
			Logger.log(e);
		}
	}

	public static void removeFromStartup() {
		try {
			int os = detectOS();
			if (os == LINUX) {
				LinuxUtils.removeFromStartup();
			} else if (os == WINDOWS) {
				WinUtils.removeFromStartup();
			} else if (os == MAC) {
				MacUtils.removeFromStartup();
			}
		} catch (Throwable e) {
			Logger.log(e);
		}
	}

	public static boolean below7() {
		try {
			int version = Integer.parseInt(System.getProperty("os.version").split("\\.")[0]);
			return (version < 6);
		} catch (Exception e) {

		}
		return false;
	}

	public static String getDownloadsFolder() {
		if (detectOS() == LINUX) {
			String path = LinuxUtils.getDownloadsFolder();
			if (path != null) {
				return path;
			}
		}
		File downloadsFolder = new File(System.getProperty("user.home"),
				DOWNLOADS);
		String downloadsFolderAbsolutePath = downloadsFolder.getAbsolutePath();
		return downloadsFolderAbsolutePath;
	}

	public static boolean isMacPopupTrigger(MouseEvent e) {
		if (detectOS() == MAC) {
			return (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0 && (e.getModifiers() & InputEvent.CTRL_MASK) != 0;
		}
		return false;
	}

	public static void forceScreenType(int type) {
		screenType = type;
	}

	public static float getScaleFactor() {
		if (screenType == XDMConstants.XHDPI) {
			return 2.0f;
		} else if (screenType == XDMConstants.HDPI) {
			return 1.3f;
		} else {
			return 1.0f;
		}
	}

	public static int detectScreenType() {
		if (screenType < 0) {
			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			double height = d.getHeight();
			if (height > 2000) {
				screenType = XDMConstants.XHDPI;
			} else if (height > 900) {
				screenType = XDMConstants.HDPI;
			} else {
				screenType = XDMConstants.NORMAL;
			}
		}
		return screenType;
	}

	public static final int getScaledInt(int size) {
		detectScreenType();
		return (int) (size * getScaleFactor());
	}

	public static String getEXEFileName(String fileName) {
		boolean isOSWindows = detectOS() == WINDOWS;
		String exeFileName = isOSWindows
				? String.format("%s.exe", fileName)
				: fileName;
		return exeFileName;
	}
}
