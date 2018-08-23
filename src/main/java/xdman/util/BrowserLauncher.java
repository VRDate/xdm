package xdman.util;

import xdman.util.os.MacUtils;
import xdman.util.os.OSUtils;

import java.io.File;

public class BrowserLauncher {
	public static boolean launchFirefox(String args) {
		int os = OSUtils.detectOS();
		if (os == OSUtils.WINDOWS) {
			String programFiles = System.getenv("PROGRAMFILES");
			String programFilesX86 = System.getenv("PROGRAMFILES(X86)");
			String firefox = "Mozilla Firefox\\firefox.exe";
			File[] ffPaths = {new File(programFiles, firefox),
					new File(programFilesX86, firefox)};
			for (int i = 0; i < ffPaths.length; i++) {
				Logger.log(ffPaths[i]);
				if (ffPaths[i].exists()) {
					return OSUtils.exec(String.format("\"%s\" %s", ffPaths[i], args));
				}
			}
		}
		if (os == OSUtils.MAC) {
			File[] ffPaths = {new File("/Applications/Firefox.app")};
			for (int i = 0; i < ffPaths.length; i++) {
				if (ffPaths[i].exists()) {
					return MacUtils.launchApp(ffPaths[i].getAbsolutePath(), args);
				}
			}
		}
		if (os == OSUtils.LINUX) {
			File[] ffPaths = {new File("/usr/bin/firefox")};
			for (int i = 0; i < ffPaths.length; i++) {
				if (ffPaths[i].exists()) {
					return OSUtils.exec(String.format("%s %s", ffPaths[i], args));
				}
			}
		}
		return false;
	}

	public static boolean launchChrome(String args) {
		int os = OSUtils.detectOS();
		if (os == OSUtils.WINDOWS) {
			String programFiles = System.getenv("PROGRAMFILES");
			String programFilesX86 = System.getenv("PROGRAMFILES(X86)");
			String localAppData = System.getenv("LOCALAPPDATA");
			String chrome = "Google\\Chrome\\Application\\chrome.exe";
			File[] ffPaths = {new File(programFiles, chrome),
					new File(programFilesX86, chrome),
					new File(localAppData, chrome)};
			for (int i = 0; i < ffPaths.length; i++) {
				if (ffPaths[i].exists()) {
					return OSUtils.exec(String.format("\"%s\" %s", ffPaths[i], args));
				}
			}
		}
		if (os == OSUtils.MAC) {
			File[] ffPaths = {new File("/Applications/Google Chrome.app")};
			for (int i = 0; i < ffPaths.length; i++) {
				if (ffPaths[i].exists()) {
					return MacUtils.launchApp(ffPaths[i].getAbsolutePath(), args);
				}
			}
		}
		if (os == OSUtils.LINUX) {
			File[] ffPaths = {new File("/usr/bin/google-chrome")};
			for (int i = 0; i < ffPaths.length; i++) {
				if (ffPaths[i].exists()) {
					return OSUtils.exec(String.format("%s %s", ffPaths[i], args));
				}
			}
		}
		return false;
	}
}
