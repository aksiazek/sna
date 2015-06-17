package pl.edu.agh.ki.toik.sna.persistence.neo4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import pl.edu.agh.ki.toik.sna.config.iface.ConfigInterface;
import pl.edu.agh.ki.toik.sna.persistence.iface.GroupData;
import pl.edu.agh.ki.toik.sna.persistence.iface.Persister;
import pl.edu.agh.ki.toik.sna.persistence.iface.Person;

public class Neo4jPersister implements Persister {

	private Connection connection;
	private ConfigInterface config = null;
	
	public void setConfig(ConfigInterface config) {
		this.config = config;
	}
	
	public void unsetConfig() {
		this.config = null;
	}
	
	@Activate
	protected void activate() {
		try {
			Class.forName("org.neo4j.jdbc.Driver");
			connection = DriverManager.getConnection(
				config.getProperty("neo4j.url", "jdbc:neo4j://localhost:7474/"),
				config.getProperty("neo4j.user", "neo4j"),
				config.getProperty("neo4j.pass", "neo4j")
			);
			connection.setAutoCommit(false);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
            throw new RuntimeException(e);
        }
	}
	
	@Deactivate
	protected void deactivate() {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void println(String x) {
		System.out.println(x);
	}
	
	@Override
	public void persist(GroupData groupData, List<Person> people) {
		println(groupData.toString());
		for(Person person : people) {
			println(person.toString());
		}
		
		String personSearch = "MATCH (p:Person) WHERE p.name = {1} RETURN p.name as name";
		String personCreate = "CREATE (p:Person { name: {1} })";
		String grouping = "MATCH (p1:Person {name: {1}}), (p2:Person {name: {2}})"
				+ " CREATE UNIQUE (p1)-[:RELATED {id: {3}, source: {4}, name: {5}}]-(p2)";
		
		for(Person person : people) {
			
			try(PreparedStatement stmt = connection.prepareStatement(personSearch))
			{
				stmt.setString(1, person.name);
				try (ResultSet rs = stmt.executeQuery()) {
					if(!rs.next()) {
						try(PreparedStatement st = connection.prepareStatement(personCreate))
						{
						    st.setString(1, person.name);
						    st.execute();
						    connection.commit();
						} catch (SQLException e) {
							connection.rollback();
				        	throw new RuntimeException(e);
				        }
					}
				}
			} catch (SQLException e) {
	        	throw new RuntimeException(e);
	        }

			for(Person collegue : people) {
				if(person == collegue)
					continue;
				
				try(PreparedStatement st = connection.prepareStatement(grouping))
				{
				    st.setString(1, person.name);
				    st.setString(2, collegue.name);
				    st.setString(3, groupData.id);
				    st.setString(4, groupData.source);
				    st.setString(5, groupData.name);
				    st.execute();
				    connection.commit();
				} catch (SQLException e) {
					try {
						connection.rollback();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
		        	throw new RuntimeException(e);
		        }
				
			}
		}
		
		
	}

}
