package pl.edu.agh.ki.toik.sna.persistence.iface;

import java.util.List;

public interface Persister {
	public void persist(GroupData groupData, List<Person> people);
}
