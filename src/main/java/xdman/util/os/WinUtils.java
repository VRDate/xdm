package xdman.util.os;

import xdman.Main;
import xdman.util.FileUtils;
import xdman.util.Logger;
import xdman.util.os.win32.NativeMethods;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class WinUtils {
	public static void open(File f) throws FileNotFoundException {
		if (!f.exists()) {
			throw new FileNotFoundException();
		}
		try {
			ProcessBuilder builder = new ProcessBuilder();
			ArrayList<String> lst = new ArrayList<String>();
			lst.add("rundll32");
			lst.add("url.dll,FileProtocolHandler");
			lst.add(f.getAbsolutePath());
			builder.command(lst);
			builder.start();
		} catch (IOException e) {
			Logger.log(e);
		}
	}

	public static void openFolder(String folder, String file) {
		if (file == null) {
			openFolder2(folder);
			return;
		}
		try {
			File f = new File(folder, file);
			if (!f.exists()) {
				throw new FileNotFoundException();
			}
			ProcessBuilder builder = new ProcessBuilder();
			ArrayList<String> lst = new ArrayList<String>();
			lst.add("explorer");
			lst.add("/select,");
			lst.add(f.getAbsolutePath());
			builder.command(lst);
			builder.start();
		} catch (IOException e) {
			Logger.log(e);
		}
	}

	private static void openFolder2(String folder) {
		try {
			ProcessBuilder builder = new ProcessBuilder();
			ArrayList<String> lst = new ArrayList<String>();
			lst.add("explorer");
			lst.add(folder);
			builder.command(lst);
			builder.start();
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	public static void keepAwakePing() {
		NativeMethods.getInstance().keepAwakePing();
	}

	public static void addToStartup() {
		String launchCmd = getLaunchCmd();
		Logger.log("Launch CMD:", launchCmd);
		NativeMethods.getInstance().addToStartup("XDM", launchCmd);
	}


	public static boolean isAlreadyAutoStart() {
		String launchCmd = getLaunchCmd();
		Logger.log("Launch CMD:", launchCmd);
		return NativeMethods.getInstance().presentInStartup("XDM", launchCmd);
	}

	private static String getLaunchCmd() {
		String javaHome = System.getProperty("java.home");
		File jarFile = FileUtils.getJarFile(Main.class);
		String jarAbsolutePath = jarFile.getAbsolutePath();
		String launchCmd = String.format("\"%s\\bin\\javaw.exe\" -Xmx1024m -jar \"%s\" -m",
				javaHome,
				jarAbsolutePath);
		return launchCmd;
	}

	public static void removeFromStartup() {
		NativeMethods.getInstance().removeFromStartup("XDM");
	}

	public static void browseURL(String url) {
		try {
			ProcessBuilder builder = new ProcessBuilder();
			ArrayList<String> lst = new ArrayList<String>();
			lst.add("rundll32");
			lst.add("url.dll,FileProtocolHandler");
			lst.add(url);
			builder.command(lst);
			builder.start();
		} catch (IOException e) {
			Logger.log(e);
		}
	}

	public static void initShutdown() {
		try {
			ProcessBuilder builder = new ProcessBuilder();
			ArrayList<String> lst = new ArrayList<String>();
			lst.add("shutdown");
			lst.add("-t");
			lst.add("30");
			lst.add("-s");
			builder.command(lst);
			builder.start();
		} catch (Exception e) {
			Logger.log(e);
		}
	}

}
