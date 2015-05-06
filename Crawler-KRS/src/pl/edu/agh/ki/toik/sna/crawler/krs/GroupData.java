package pl.edu.agh.ki.toik.sna.crawler.krs;

public class GroupData {
	String id = null;
	String source = null;
	String name = null;
	
	public void validate() {
		assert id != null;
		assert source != null;
		assert name != null;
	}
	
	public String toString() {
		return name;
	}
}
