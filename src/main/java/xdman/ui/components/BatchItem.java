package xdman.ui.components;

import xdman.downloaders.metadata.HttpMetadata;

class BatchItem {
	String file;
	boolean selected;
	HttpMetadata metadata;

	@Override
	public String toString() {
		return file;
	}
}
