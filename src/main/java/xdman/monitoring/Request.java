package xdman.monitoring;

import xdman.network.http.HeaderCollection;
import xdman.util.Logger;
import xdman.util.NetUtils;

import java.io.IOException;
import java.io.InputStream;

public class Request {
	private String url;
	private HeaderCollection headers;
	private byte[] body;
	private int method;

	public void read(InputStream in) throws IOException {
		String reqLine = NetUtils.readLine(in);
		if (Logger.isTraceEnabled()) {
			Logger.log("Request",
					reqLine);
		}
		//Logger.log(reqLine);
		if (reqLine == null || reqLine.length() < 1) {
			throw new IOException(String.format("Invalid request line: %s", reqLine));
		}
		String[] arr = reqLine.split(" ");
		if (arr.length != 3) {
			throw new IOException(String.format("Invalid request: %s", reqLine));
		}
		this.url = arr[1];
		this.method = arr[0].toLowerCase().equals("post") ? 1 : 2;
		this.headers = new HeaderCollection();
		headers.loadFromStream(in);
		String header = headers.getValue("Content-Length");
		if (header != null) {
			long len = Long.parseLong(header);
			body = new byte[(int) len];
			int off = 0;
			while (len > 0) {
				int x = in.read(body, off, body.length - off);
				if (x == -1) {
					throw new IOException("Unexpected EOF");
				}
				len -= x;
				off += x;
			}
		}
	}

	public final String getUrl() {
		return url;
	}

	public final void setUrl(String url) {
		this.url = url;
	}

	public final HeaderCollection getHeaders() {
		return headers;
	}

	public final void setHeaders(HeaderCollection headers) {
		this.headers = headers;
	}

	public final byte[] getBody() {
		return body;
	}

	public final void setBody(byte[] body) {
		this.body = body;
	}

	public final int getMethod() {
		return method;
	}

	public final void setMethod(int method) {
		this.method = method;
	}
}
