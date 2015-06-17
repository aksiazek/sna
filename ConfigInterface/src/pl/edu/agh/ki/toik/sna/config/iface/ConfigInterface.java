package pl.edu.agh.ki.toik.sna.config.iface;

public interface ConfigInterface {
	public String getProperty(String key);
	public String getProperty(String key, String defaultValue);
	public void setProperty(String key, String value);
}
