package pl.edu.agh.ki.toik.sna.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ConfigService implements ConfigInterface, BundleActivator {

	public static final String CONFIG_FILE = "config.ini";
	
	private static BundleContext context;
	private static Properties props;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		context = bundleContext;
		
        FileInputStream in = new FileInputStream(CONFIG_FILE);
        props = new Properties();
        props.load(in);
        in.close();
	}
	
	public String getProperty(String key) {
		return props.getProperty(key);
	}
	
	public String getProperty(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		FileOutputStream out = new FileOutputStream(CONFIG_FILE);
		props.store(out, null);
		out.close();
		
		context = null;
	}

}
