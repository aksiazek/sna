package pl.edu.agh.ki.toik.sna.crawler.krs;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import pl.edu.agh.ki.toik.sna.config.iface.ConfigInterface;
import pl.edu.agh.ki.toik.sna.persistence.iface.GroupData;
import pl.edu.agh.ki.toik.sna.persistence.iface.Persister;
import pl.edu.agh.ki.toik.sna.persistence.iface.Person;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

public class RestCrawler extends Thread {
	int currentPage = 1;
	Resty r = new Resty();
	final String ENTITIES_API_ENDPOINT = "https://api.mojepanstwo.pl/krs/podmioty?fields[]=id&page=__PAGE__";
	final String ENTITY_API_ENDPOINT = "https://api.mojepanstwo.pl/krs/podmioty/__ID__?fields[]=nazwa_skrocona&layers[]=reprezentacja&layers[]=nadzor";
	boolean stop = false;
	
	static Logger logger = Logger.getLogger("CrawlerKRS");
	ConfigInterface config;
	Persister persister;
	
	public void setPersister(Persister persister) {
		logger.info("Got persister: " + persister);
		this.persister = persister;
	}
	
	public void unsetPersister(Persister persister) {
		if(this.persister == persister) {
			this.persister = null;
		}
	}
	
	public void setConfig(ConfigInterface config) {
		logger.info("Got config: " + config);
		this.config = config;
	}
	
	public void unsetConfig(ConfigInterface config) {
		if(this.config == config) {
			this.config = null;
		}
	}
	
	@Activate
	protected void activate() {
		logger.info("Starting crawler...");
		this.start();
		currentPage = Integer.parseInt(config.getProperty("crawler.krs.page", "1"));
	}
	
	@Deactivate
	protected void deactivate() {
		logger.info("Stopping crawler...");
		try {
			this.requestStop();
			this.join();
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "Exception during crawler deactivation", e);
		}
		config.setProperty("crawler.krs.page", Integer.toString(currentPage));
	}
	
	public void requestStop() {
		stop = true;
	}
	
	public void run() {
		try {
			int retries = 3;
			while(!stop && retries > 0) {
				try {
					crawlPage(currentPage);
					currentPage++;
					config.setProperty("crawler.krs.page", Integer.toString(currentPage));
				} catch(Exception e) {
					logger.log(Level.SEVERE, "Exception during crawling loop", e);
					retries--;
				}
			}
		} catch(IndexOutOfBoundsException e) {
			currentPage++;
			config.setProperty("crawler.krs.page", Integer.toString(currentPage));
			return;
		}
		config.setProperty("crawler.krs.page", Integer.toString(currentPage));
	}
	
	public void crawlPage(int page) throws NumberFormatException, Exception {
		logger.info("Crawling page " + page);
		
		JSONResource json = r.json(ENTITIES_API_ENDPOINT.replace("__PAGE__", Integer.toString(page)));
		int count = Integer.parseInt(json.get(Resty.path("search.pagination.count")).toString());
		int total = Integer.parseInt(json.get(Resty.path("search.pagination.total")).toString());
		int from = Integer.parseInt(json.get(Resty.path("search.pagination.from")).toString());
		
		logger.info("Processing batch " + from + " - " + (from+count - 1) + " of " + total);
		
		if(from > total) {
			throw new IndexOutOfBoundsException("Page number is out of a valid range");
		}
		
		JSONArray results = (JSONArray)json.get(Resty.path("search.dataobjects"));
		assert count == results.length();
		
		for(int i = 0; i < count; i++) {
			crawlEntity(Integer.parseInt(results.getJSONObject(i).get("id").toString()));
		}
	}
	
	public Person getPerson(JSONObject resource, String defaultRole) throws JSONException {
		Person person = new Person();
		person.name = resource.getString("nazwa");
		if(resource.has("data_urodzenia")) {
			person.birth = resource.getString("data_urodzenia");
		}
		if(resource.has("funkcja")) {
			person.tags.add(resource.getString("funkcja"));
		} else if (defaultRole != null) {
			person.tags.add(defaultRole);
		}
		return person;
	}
	
	public void addPeople(List<Person> people, JSONArray array, String role) throws JSONException {
		
		int count = array.length();
		for(int i = 0; i < count; i++) {
			people.add(getPerson(array.getJSONObject(i), role));
		}
	}
	
	public void crawlEntity(int id) throws Exception {
		logger.info("Crawling object " + id);
		
		JSONResource json = r.json(ENTITY_API_ENDPOINT.replace("__ID__", Integer.toString(id)));
		
		GroupData group = new GroupData();
		group.id = "KRS " + json.get("object.id").toString();
		group.name = ((JSONObject)json.get(Resty.path("object.data"))).getString("krs_podmioty.nazwa_skrocona");
		group.source = "KRS";
		
		List<Person> peopleList = new ArrayList<Person>();
		addPeople(peopleList, (JSONArray)json.get(Resty.path("object.layers.reprezentacja")), "Reprezentant");
		addPeople(peopleList, (JSONArray)json.get(Resty.path("object.layers.nadzor")), "Członek Rady Nadzorczej");
		
		persister.persist(group, peopleList);
	}
}
