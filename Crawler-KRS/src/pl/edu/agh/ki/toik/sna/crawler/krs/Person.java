package pl.edu.agh.ki.toik.sna.crawler.krs;

import java.util.ArrayList;
import java.util.List;

public class Person {
	public Integer id = null;
	public String name = null;
	public String birth = null;
	public List<String> tags = new ArrayList<String>();
	
	public void validate() {
		assert name != null;
	}
	
	public String toString() {
		return name;
	}
}
