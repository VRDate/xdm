package xdman.downloaders.ftp;

import xdman.XDMConstants;
import xdman.downloaders.AbstractChannel;
import xdman.downloaders.Segment;
import xdman.network.ftp.FtpClient;
import xdman.network.http.JavaClientRequiredException;
import xdman.util.Logger;
import xdman.util.os.OSUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class FtpChannel extends AbstractChannel {
	private String url;
	private FtpClient ftpClient;
	private InputStream in;
	private boolean redirected;
	private String redirectUrl;
	private long length;

	public FtpChannel(Segment chunk, String url) {
		super(chunk);
		this.url = url;
	}

	@Override
	protected boolean connectImpl() {
		int sleepInterval = 0;
		boolean isRedirect = false;
		if (stop) {
			closeImpl();
			return false;
		}

		if (chunk.getLength() < 0 && chunk.getDownloaded() > 0) {
			errorCode = XDMConstants.ERR_NO_RESUME;
			closeImpl();
			Logger.log("server does not support resuming");
			return false;
		}
		try {
			chunk.reopenStream();
		} catch (IOException e) {
			Logger.log(e);
			closeImpl();
			errorCode = XDMConstants.ERR_NO_RESUME;
			return false;
		}

		PasswordAuthentication passwordAuthentication = new PasswordAuthentication("anonymous",
				"anonymous".toCharArray());

		while (!stop) {
			isRedirect = false;
			try {
				Logger.log("ftp Connecting to:",
						url,
						chunk.getTag(),
						"offset",
						(chunk.getStartOffset() + chunk.getDownloaded()));
				ftpClient = new FtpClient(url);

				// ftpClient.setHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0;
				// rv:51.0) Gecko/20100101 Firefox/51.0");

				long startOff = chunk.getStartOffset() + chunk.getDownloaded();

				if (startOff > 0) {
					ftpClient.setOffset(startOff);
				}

				ftpClient.setUser(passwordAuthentication.getUserName());
				ftpClient.setPassword(new String(passwordAuthentication.getPassword()));

				ftpClient.connect();

				if (stop) {
					closeImpl();
					return false;
				}

				int code = ftpClient.getStatusCode();

				Logger.log(chunk, ":", code);

				if (code != 200
						&& code != 206
						&& code != 416
						&& code != 413
						&& code != 401
						&& code != 408
						&& code != 407
						&& code != 503) {
					errorCode = XDMConstants.ERR_INVALID_RESP;
					closeImpl();
					return false;
				}

				if (code == 407 || code == 401) {
					Logger.log("asking for password");
					boolean proxy = code == 407;
					passwordAuthentication = Authenticator.requestPasswordAuthentication(null,
							ftpClient.getPort(),
							"ftp",
							"",
							"ftp");

					if (passwordAuthentication == null) {
						if (!chunk.promptCredential(ftpClient.getHost(), proxy)) {
							errorCode = XDMConstants.ERR_INVALID_RESP;
							closeImpl();
							return false;
						} else {
							passwordAuthentication = Authenticator.requestPasswordAuthentication(null,
									ftpClient.getPort(),
									"ftp",
									"",
									"ftp");
							Logger.log("Password Authentication:", passwordAuthentication);
							throw new JavaClientRequiredException();
						}
					}
				}

				if (stop) {
					closeImpl();
					return false;
				}

				if (((chunk.getDownloaded() + chunk.getStartOffset()) > 0) && code != 206) {
					closeImpl();
					errorCode = XDMConstants.ERR_NO_RESUME;
					return false;
				}

				length = ftpClient.getContentLength();

				if (ftpClient.getContentLength() > 0 && OSUtils.getFreeSpace(null) < ftpClient.getContentLength()) {
					Logger.log("Disk is full");
					errorCode = XDMConstants.DISK_FAIURE;
					closeImpl();
					return false;
				}

				in = ftpClient.getInputStream();
				Logger.log("Connection success");
				return true;

			} catch (JavaClientRequiredException e) {
				sleepInterval = 0;
			} catch (Exception e) {
				Logger.log(chunk);
				Logger.log(e);
				if (isRedirect) {
					closeImpl();
					continue;
				}
				sleepInterval = 5000;
			}

			closeImpl();

			try {
				Thread.sleep(sleepInterval);
			} catch (Exception e) {
			}
		}

		Logger.log("return as", errorCode);

		return false;
	}

	@Override
	protected InputStream getInputStreamImpl() {
		return in;
	}

	@Override
	protected long getLengthImpl() {
		return length;
	}

	@Override
	protected void closeImpl() {
		if (ftpClient != null) {
			ftpClient.dispose();
		}
	}

	public boolean isFinished() {
		return false;
	}

	public boolean isRedirected() {
		return redirected;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}
}
