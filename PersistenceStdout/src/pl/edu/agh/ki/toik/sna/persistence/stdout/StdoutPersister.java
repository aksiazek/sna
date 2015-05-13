package pl.edu.agh.ki.toik.sna.persistence.stdout;

import java.util.List;

import pl.edu.agh.ki.toik.sna.persistence.iface.GroupData;
import pl.edu.agh.ki.toik.sna.persistence.iface.Persister;
import pl.edu.agh.ki.toik.sna.persistence.iface.Person;

public class StdoutPersister implements Persister {

	@Override
	public void persist(GroupData groupData, List<Person> people) {
		System.out.println(groupData);
		for(Person person : people) {
			System.out.println(person);
		}
		System.out.println();
	}

}
