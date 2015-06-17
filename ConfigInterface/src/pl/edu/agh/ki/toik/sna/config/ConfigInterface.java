package pl.edu.agh.ki.toik.sna.config;

public interface ConfigInterface {
	public String getProperty(String key);
	public String getProperty(String key, String defaultValue);
}
