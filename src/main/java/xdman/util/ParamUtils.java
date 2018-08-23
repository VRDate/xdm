package xdman.util;

import xdman.Config;

import javax.swing.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;

public class ParamUtils {

	private static ParamUtils _this;
	private int port;

	public ParamUtils(int port) {
		Logger.log("ParamUtils on port", port);
		this.port = port;
	}

	public static ParamUtils getInstance() {
		if (_this == null) {
			Config config = Config.getInstance();
			int xdmPort = config.getXDMPort();
			_this = new ParamUtils(xdmPort);
		}
		return _this;
	}

	public void sendParam(Map<String, String> params) {
		StringBuffer sb = new StringBuffer();
		Iterator<String> paramIter = params.keySet().iterator();
		while (paramIter.hasNext()) {
			String key = paramIter.next();
			String value = params.get(key);
			sb.append(key).append(":").append(value).append("\n");
		}

		InetAddress loopbackAddress = InetAddress.getLoopbackAddress();

		StringBuffer reqBuf = new StringBuffer();
		reqBuf.append("GET /cmd HTTP/1.1\r\n");
		reqBuf.append("Content-Length: ").append(sb.length()).append("\r\n");
		reqBuf.append("Host: ").append(loopbackAddress.getHostName()).append("\r\n");
		reqBuf.append("Connection: close\r\n\r\n");
		reqBuf.append(sb);
		String resp = null;
		Socket sock = null;
		try {
			sock = new Socket(loopbackAddress, port);
			InputStream in = sock.getInputStream();
			OutputStream out = sock.getOutputStream();
			out.write(reqBuf.toString().getBytes());
			resp = NetUtils.readLine(in);
			Logger.log("ParamUtils",
					resp);
			resp = resp.split(" ")[1];
		} catch (Exception e) {
			Logger.log(e);
		} finally {
			if (sock != null) {
				try {
					sock.close();
				} catch (Exception e2) {
					Logger.log(e2);
				}
			}
		}

		if (!"200".equals(resp)) {
			JOptionPane.showMessageDialog(null,
					"An older version of XDM is already running.");
		}
	}
}
