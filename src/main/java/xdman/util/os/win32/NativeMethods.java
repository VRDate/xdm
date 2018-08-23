package xdman.util.os.win32;

import xdman.Config;
import xdman.Main;
import xdman.util.FileUtils;
import xdman.util.Logger;

import java.io.File;

public class NativeMethods {
	public static final String XDM_NATIVE_DLL = "xdm_native.dll";
	public static final String NOT_FOUND_AT = "not found at";
	private static NativeMethods _me;

	private NativeMethods() {
		File jarFile = FileUtils.getJarFile(Main.class);
		File xdmNativeDLLFile = new File("C:\\Program Files (x86)\\XDM",
				XDM_NATIVE_DLL);
		if (!xdmNativeDLLFile.exists()) {
			Logger.log(XDM_NATIVE_DLL,
					NOT_FOUND_AT,
					xdmNativeDLLFile.getAbsolutePath());
			xdmNativeDLLFile = new File(Config.getInstance().getDataFolder(),
					XDM_NATIVE_DLL);
			xdmNativeDLLFile = new File("C:\\Program Files\\XDM",
					XDM_NATIVE_DLL);
			if (!xdmNativeDLLFile.exists()) {
				Logger.log(XDM_NATIVE_DLL,
						NOT_FOUND_AT,
						xdmNativeDLLFile.getAbsolutePath());
				xdmNativeDLLFile = new File(Config.getInstance().getDataFolder(),
						XDM_NATIVE_DLL);
				if (!xdmNativeDLLFile.exists()) {
					Logger.log(XDM_NATIVE_DLL,
							NOT_FOUND_AT,
							xdmNativeDLLFile.getAbsolutePath());
					xdmNativeDLLFile = new File(jarFile.getParentFile(),
							XDM_NATIVE_DLL);
					if (!xdmNativeDLLFile.exists()) {
						Logger.log(XDM_NATIVE_DLL,
								NOT_FOUND_AT,
								xdmNativeDLLFile.getAbsolutePath());
						xdmNativeDLLFile = new File(jarFile,
								XDM_NATIVE_DLL);
						if (!xdmNativeDLLFile.exists()) {
							Logger.log(XDM_NATIVE_DLL,
									NOT_FOUND_AT,
									xdmNativeDLLFile.getAbsolutePath());
						}
					}
				}
			}
		}
		String dllPath = xdmNativeDLLFile.getAbsolutePath();
		Logger.log("Loading",
				XDM_NATIVE_DLL,
				"exists",
				xdmNativeDLLFile.exists(),
				"from",
				dllPath);
		try {
			System.load(dllPath);
		} catch (Exception e) {
			Logger.log(e);
		}
	}

	public static NativeMethods getInstance() {
		if (_me == null) {
			_me = new NativeMethods();
		}
		return _me;
	}

	public final native void keepAwakePing();

	public final native void addToStartup(String key, String value);

	public final native boolean presentInStartup(String key, String value);

	public final native void removeFromStartup(String key);

	public final native String getDownloadsFolder();

	public final native String stringTest(String str);
}
