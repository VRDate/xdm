package xdman.util;

import xdman.XDMConstants;
import xdman.downloaders.metadata.HttpMetadata;
import xdman.mediaconversion.FFmpeg;
import xdman.util.os.LinuxUtils;
import xdman.util.os.MacUtils;
import xdman.util.os.OSUtils;
import xdman.util.os.WinUtils;
import xdman.videoparser.YoutubeDLHandler;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class XDMUtils {
	//	private static Map<Integer, String> categoryFolderMap;
//
//	static {
//		categoryFolderMap = new HashMap<>();
//		categoryFolderMap.put(XDMConstants.DOCUMENTS, "Documents");
//		categoryFolderMap.put(XDMConstants.MUSIC, "Music");
//		categoryFolderMap.put(XDMConstants.VIDEO, "Videos");
//		categoryFolderMap.put(XDMConstants.PROGRAMS, "Programs");
//		categoryFolderMap.put(XDMConstants.COMPRESSED, "Compressed");
//	}
//
//	public static String getFolderForCategory(int category) {
//		return categoryFolderMap.get(category);
//	}

	public static boolean validateURL(String url) {
		try {
			url = url.toLowerCase();
			if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("ftp://")) {
				new URL(url);
				return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	static String doc[] = {".doc", ".docx", ".txt", ".pdf", ".rtf", ".xml", ".c", ".cpp", ".java", ".cs", ".vb",
			".html", ".htm", ".chm", ".xls", ".xlsx", ".ppt", ".pptx", ".js", ".css"};
	static String cmp[] = {".7z", ".zip", ".rar", ".gz", ".tgz", ".tbz2", ".bz2", ".lzh", ".sit", ".z"};
	static String music[] = {".mp3", ".wma", ".ogg", ".aiff", ".au", ".mid", ".midi", ".mp2", ".mpa", ".wav", ".aac",
			".oga", ".ogx", ".ogm", ".spx", ".opus"};
	static String vid[] = {".mpg", ".mpeg", ".avi", ".flv", ".asf", ".mov", ".mpe", ".wmv", ".mkv", ".mp4", ".3gp",
			".divx", ".vob", ".webm", ".ts"};
	static String prog[] = {".exe", ".msi", ".bin", ".sh", ".deb", ".cab", ".cpio", ".dll", ".jar", "rpm", ".run",
			".py"};

	public static int findCategory(String filename) {
		String file = filename.toLowerCase();
		for (int i = 0; i < doc.length; i++) {
			if (file.endsWith(doc[i])) {
				return XDMConstants.DOCUMENTS;
			}
		}
		for (int i = 0; i < cmp.length; i++) {
			if (file.endsWith(cmp[i])) {
				return XDMConstants.COMPRESSED;
			}
		}
		for (int i = 0; i < music.length; i++) {
			if (file.endsWith(music[i])) {
				return XDMConstants.MUSIC;
			}
		}
		for (int i = 0; i < prog.length; i++) {
			if (file.endsWith(prog[i])) {
				return XDMConstants.PROGRAMS;
			}
		}
		for (int i = 0; i < vid.length; i++) {
			if (file.endsWith(vid[i])) {
				return XDMConstants.VIDEO;
			}
		}
		return XDMConstants.OTHER;
	}

	public static String appendArray2Str(String[] arr) {
		boolean first = true;
		StringBuffer buf = new StringBuffer();
		for (String s : arr) {
			if (!first) {
				buf.append(",");
			}
			buf.append(s);
			first = false;
		}
		return buf.toString();
	}

	public static String[] appendStr2Array(String str) {
		String[] arr = str.split(",");
		ArrayList<String> arrList = new ArrayList<String>();
		for (String s : arr) {
			String txt = s.trim();
			if (txt.length() > 0) {
				arrList.add(txt);
			}
		}
		arr = new String[arrList.size()];
		return arrList.toArray(arr);
	}

	public static void copyStream(InputStream instream, OutputStream outstream, long size) throws Exception {
		byte[] b = new byte[8192];
		long rem = size;
		while (true) {
			int bs = (int) (size > 0 ? (rem > b.length ? b.length : rem) : b.length);
			int x = instream.read(b, 0, bs);
			if (x == -1) {
				if (size > 0) {
					throw new EOFException("Unexpected EOF");
				} else {
					break;
				}
			}
			outstream.write(b, 0, x);
			rem -= x;
			if (size > 0) {
				if (rem <= 0)
					break;
			}
		}
	}

	public static void keepAwakePing() {
		try {
			int os = OSUtils.detectOS();
			if (os == OSUtils.LINUX) {
				LinuxUtils.keepAwakePing();
			} else if (os == OSUtils.WINDOWS) {
				WinUtils.keepAwakePing();
			} else if (os == OSUtils.MAC) {
				MacUtils.keepAwakePing();
			}
		} catch (Throwable e) {
			// Logger.log(e);
		}
	}

	public static boolean isAlreadyAutoStart() {
		try {
			int os = OSUtils.detectOS();
			if (os == OSUtils.LINUX) {
				return LinuxUtils.isAlreadyAutoStart();
			} else if (os == OSUtils.WINDOWS) {
				return WinUtils.isAlreadyAutoStart();
			} else if (os == OSUtils.MAC) {
				return MacUtils.isAlreadyAutoStart();
			}
			return false;
		} catch (Throwable e) {
			Logger.log(e);
		}
		return false;
	}

	public static boolean areComponentsInstalled() {
		boolean isFFmpegInstalled = FFmpeg.isFFmpegInstalled();
		boolean isYouTubeDLInstalled = YoutubeDLHandler.isYouTubeDLInstalled();
		boolean componentsInstalled = isFFmpegInstalled
				&& isYouTubeDLInstalled;
		return componentsInstalled;
	}

	public static String getClipBoardText() {
		try {
			return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		} catch (Exception e) {
			Logger.log(e);
		}
		return "";
	}

	public static void browseURL(String url) {
		int os = OSUtils.detectOS();
		if (os == OSUtils.WINDOWS) {
			WinUtils.browseURL(url);
		} else if (os == OSUtils.LINUX) {
			LinuxUtils.browseURL(url);
		} else if (os == OSUtils.MAC) {
			MacUtils.browseURL(url);
		}
	}

	// public static boolean isYdlInstalled() {
	// return (new File(Config.getInstance().getDataFolder(),
	// "youtube-dl" + (XDMUtils.detectOS() == XDMUtils.WINDOWS ? ".exe" :
	// "")).exists());
	// }

	public static void mkdirs(String folder) {
		File outFolder = new File(folder);
		if (!outFolder.exists()) {
			outFolder.mkdirs();
		}
	}

	public static List<HttpMetadata> toMetadata(List<String> urls) {
		List<HttpMetadata> list = new ArrayList<>();
		for (String url : urls) {
			HttpMetadata md = new HttpMetadata();
			md.setUrl(url);
			list.add(md);
		}
		return list;
	}

}