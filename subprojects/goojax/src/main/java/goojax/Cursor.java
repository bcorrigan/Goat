package goojax;

/* this would be fine as an inner class, if GSON supported inner classes nicely */

public class Cursor {
	public String moreResultsUrl;
	public int currentPageIndex;
	public long estimatedResultCount;
	public CursorPage pages[];

	Cursor() {}

}
