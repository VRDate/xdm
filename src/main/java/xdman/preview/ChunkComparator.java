package xdman.preview;

import java.util.Comparator;

class ChunkComparator implements Comparator<Chunk> {

	@Override
	public int compare(Chunk c1, Chunk c2) {
		if (c1.startOff > c2.startOff) {
			return 1;
		} else if (c1.startOff < c2.startOff) {
			return -1;
		} else {
			return 0;
		}
	}
}
