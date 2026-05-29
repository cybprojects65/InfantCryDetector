package it.cnr.infant.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class ConfigManager {

	static File configFile = new File("config.properties"); 
	
	public static String getProperty(String property) {
		try {
		Properties properties = new Properties();
		FileInputStream fis = new FileInputStream(configFile.getAbsolutePath());
		properties.load(fis);
		String value = properties.getProperty(property).replace("\"", "");
		return value;
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
		
	}
	

}
