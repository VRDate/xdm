package xdman.monitoring;

import xdman.Config;
import xdman.XDMApp;
import xdman.util.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class BrowserMonitor implements Runnable {
	private static BrowserMonitor _this;
	private int port;

	public BrowserMonitor(int port) {
		Logger.log("BrowserMonitor on port", port);
		this.port = port;
	}

	public static BrowserMonitor getInstance() {
		if (_this == null) {
			Config config = Config.getInstance();
			int xdmPort = config.getXDMPort();
			_this = new BrowserMonitor(xdmPort);
		}
		return _this;
	}

	public void startMonitoring() {
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		ServerSocket serverSock = null;
		try {
			serverSock = new ServerSocket();
			serverSock.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
			XDMApp.instanceStarted();
			while (true) {
				Socket sock = serverSock.accept();
				MonitoringSession session = new MonitoringSession(sock);
				session.start();
			}
		} catch (Exception e) {
			Logger.log(e);
			XDMApp.instanceAlreadyRunning();
		}
		try {
			serverSock.close();
		} catch (Exception e) {
		}
	}
}
