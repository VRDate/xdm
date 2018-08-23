package xdman;

import xdman.downloaders.metadata.HttpMetadata;
import xdman.util.FileUtils;
import xdman.util.Logger;
import xdman.util.StringUtils;
import xdman.util.XDMUtils;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URL;

public class ClipboardMonitor implements Runnable {

	private static ClipboardMonitor _this;
	private String lastContent;
	private Thread t;

	private ClipboardMonitor() {

	}

	public static ClipboardMonitor getInstance() {
		if (_this == null) {
			_this = new ClipboardMonitor();
		}
		return _this;
	}

	public static void copyURL(String url) {
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	public void startMonitoring() {
		try {
			if (t == null) {
				t = new Thread(this);
				t.start();
			}
		} catch (Exception e) {
			Logger.log(e);
		}

	}

	public void stopMonitoring() {
		try {
			if (t != null && t.isAlive()) {
				t.interrupt();
				t = null;
			}
		} catch (Exception e) {
			Logger.log(e);
		}

	}

	@Override
	public void run() {
		try {
			while (true) {
				String txt = XDMUtils.getClipBoardText();
				if (StringUtils.isNullOrEmptyOrBlank(txt)) {
					return;
				}
				if (!txt.equals(lastContent)) {
					Logger.log("New content:", txt);
					lastContent = txt;
					try {
						new URL(txt);
						HttpMetadata md = new HttpMetadata();
						md.setUrl(txt);
						String file = FileUtils.getFileName(txt);
						String ext = FileUtils.getExtension(file);
						if (!StringUtils.isNullOrEmptyOrBlank(ext)) {
							ext = ext.toUpperCase().replace(".", "");
						}

						String[] arr = Config.getInstance().getFileExts();
						boolean found = false;
						for (int i = 0; i < arr.length; i++) {
							if (arr[i].contains(ext)) {
								found = true;
								break;
							}
						}
						if (found) {
							XDMApp.getInstance().addDownload(md, file);
						}
					} catch (Exception e) {
					}

				}
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			Logger.log(e);
		}
	}
}
