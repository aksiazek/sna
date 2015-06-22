package pl.edu.agh.ki.toik.sna.crawler.parliament;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.ComponentContext;

import pl.edu.agh.ki.toik.sna.persistence.iface.GroupData;
import pl.edu.agh.ki.toik.sna.persistence.iface.Persister;
import pl.edu.agh.ki.toik.sna.persistence.iface.Person;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

public class RestCrawler extends Thread {
	int currentPage = 1;
	Resty r = new Resty();
	final String ENTITIES_API_ENDPOINT = "https://api.mojepanstwo.pl/dane/poslowie/__PAGE__";
	boolean stop = false;
	private List<Person> peopleList = new ArrayList<Person>();

	Logger logger = Logger.getLogger("CrawlerParliament");
	Persister persister;
	
	public synchronized void setPersister(Persister persister) {
		logger.info(this.toString() + " got persister: " + persister);
		this.persister = persister;
	}
	
	public synchronized void unsetPersister(Persister persister) {
		if(this.persister == persister) {
			this.persister = null;
		}
	}
	
	protected void activate(ComponentContext context) {
		logger.info("Starting crawler...");
		this.start();
	}
	
	protected void deactivate(ComponentContext context) {
		logger.info("Stopping crawler...");
		try {
			this.requestStop();
			this.join();
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "Exception during crawler deactivation", e);
		}
	}
	
	public void requestStop() {
		stop = true;
	}
	
	public void run() {
		try {
			int retries = 3;
			while(!stop && retries > 0) {
				try {
					if(!crawlPage(currentPage))
						return;
					currentPage++;
				} catch(Exception e) {
					logger.log(Level.SEVERE, "Exception during crawling loop", e);
					retries--;
				}
			}
		} catch(IndexOutOfBoundsException e) {
			currentPage++;
			return;
		}
	}
	
	public boolean crawlPage(int page) throws NumberFormatException, Exception {
		JSONResource json = r.json(ENTITIES_API_ENDPOINT.replace("__PAGE__", Integer.toString(page)));
		Object obj = json.get(Resty.path("object"));
		if(obj instanceof JSONObject) {
			logger.info("Crawling page " + page);
			peopleList.add(getPerson((JSONObject) ((JSONObject) obj).get("data")));
			return true;
		} else {
			finish();
			return false;
		}
	}
	
	public Person getPerson(JSONObject resource) throws JSONException {
		Person person = new Person();
		person.name = resource.getString("poslowie.nazwisko")+" "+
				resource.getString("poslowie.imiona");
		person.birth = resource.getString("poslowie.data_urodzenia");
		if(resource.has("poslowie.krs_osoba_id")) {
			person.id = resource.getInt("poslowie.krs_osoba_id");
		}
		return person;
	}
	
	public void finish() throws Exception {
		GroupData group = new GroupData();
		group.id = "Posłowie";
		group.name = "Posłowie";
		group.source = "Posłowie";
		persister.persist(group, peopleList);
	}
}
