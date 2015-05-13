package pl.edu.agh.ki.toik.sna.persistence.iface;

public class GroupData {
	public String id = null;
	public String source = null;
	public String name = null;
	
	public void validate() {
		assert id != null;
		assert source != null;
		assert name != null;
	}
	
	public String toString() {
		return name;
	}
}
