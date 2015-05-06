package pl.edu.agh.ki.toik.sna.crawler.krs;

import java.util.ArrayList;
import java.util.List;

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
				} catch(Exception e) {
					retries--;
				}
			}
		} catch(IndexOutOfBoundsException e) {
			currentPage++;
			return;
		}
	}
	
	public void crawlPage(int page) throws NumberFormatException, Exception {
		JSONResource json = r.json(ENTITIES_API_ENDPOINT.replace("__PAGE__", Integer.toString(page)));
		int count = Integer.parseInt(json.get(Resty.path("search.pagination.count")).toString());
		int total = Integer.parseInt(json.get(Resty.path("search.pagination.count")).toString());
		int from = Integer.parseInt(json.get(Resty.path("search.pagination.from")).toString());
		
		if(from > total) {
			throw new IndexOutOfBoundsException("Page number is out of a valid range");
		}
		
		System.out.println(json.get(Resty.path("search.dataobjects")));
		JSONArray results = (JSONArray)json.get(Resty.path("search.dataobjects"));
		assert count == results.length();
		
		for(int i = 0; i < count; i++) {
			crawlEntity(Integer.parseInt(results.getJSONObject(i).get("id").toString()));
		}
	}
	
	public Person getPerson(JSONObject resource, String defaultRole) throws JSONException {
		Person person = new Person();
		person.name = resource.getString("nazwa");
		if(resource.getString("data_urodzenia") != null) {
			person.birth = resource.getString("data_urodzenia");
		}
		if(resource.getString("funkcja") != null) {
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
		JSONResource json = r.json(ENTITY_API_ENDPOINT.replace("__ID__", Integer.toString(id)));
		
		GroupData group = new GroupData();
		group.id = "KRS " + json.get("object.id").toString();
		group.name = ((JSONObject)json.get(Resty.path("object.data"))).getString("krs_podmioty.nazwa_skrocona");
		group.source = "KRS";
		
		List<Person> peopleList = new ArrayList<Person>();
		addPeople(peopleList, (JSONArray)json.get(Resty.path("object.layers.reprezentacja")), "Reprezentant");
		addPeople(peopleList, (JSONArray)json.get(Resty.path("object.layers.nadzor")), "Członek Rady Nadzorczej");
		
		// TODO: save GroupData and List<Person> in Neo4J
	}
}
