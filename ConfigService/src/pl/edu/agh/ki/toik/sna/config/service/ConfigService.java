package pl.edu.agh.ki.toik.sna.config.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import pl.edu.agh.ki.toik.sna.config.iface.ConfigInterface;

public class ConfigService implements ConfigInterface {

	public static final String CONFIG_FILE = "config.ini";

	static Logger logger = Logger.getLogger("CrawlerKRS");
	
	private static Properties props;
	private File configFile;

	@Activate
	protected void activate() {
		configFile = new File(CONFIG_FILE);
		logger.info("Using config file: " + configFile.getAbsolutePath());
		try {
			if(!configFile.exists()) {
				configFile.createNewFile();
			}
			
	        FileInputStream in = new FileInputStream(configFile);
	        props = new Properties();
	        props.load(in);
	        in.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Deactivate
	protected void deactivate() {
		logger.info("Saving config...");
		try {
			FileOutputStream out = new FileOutputStream(configFile);
			props.store(out, null);
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getProperty(String key) {
		return props.getProperty(key);
	}
	
	public String getProperty(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	}
	
	public void setProperty(String key, String value) {
		props.setProperty(key, value);
		deactivate();
	}

}
