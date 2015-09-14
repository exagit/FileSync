package fileDataTypes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

public class FileSyncInfo {

	public static enum Parameters {
		BASEDIR, DESTDIR, DESTINATION, USER, PASSWORD
	}

	public String Basedir;
	public String Destdir;
	public String Destination;
	public String User;
	public String Password;
	
	public String configFile;
	
	Properties p;

	public FileSyncInfo() {
		Basedir = Destdir = Destination = User = Password = "";
	}

	public FileSyncInfo(String configfile) {

		this();
		this.configFile = configfile;
		File f = new File(configfile);
		if(f.exists()){
			try { 
				FileInputStream inp = new FileInputStream(f);
				p = new Properties();
				p.load(inp);
				Basedir = p.getProperty(FileSyncInfo.Parameters.BASEDIR.name());
				Destdir = p.getProperty(FileSyncInfo.Parameters.DESTDIR.name());
				Destination = p.getProperty(FileSyncInfo.Parameters.DESTINATION.name());
				User = p.getProperty(FileSyncInfo.Parameters.USER.name());
				Password = base64decode(p.getProperty(FileSyncInfo.Parameters.PASSWORD.name()));
	
			} catch (Exception e) {
				System.err.println("Cannot load file " + configfile);
			}
		}

	}
	
	public static String base64encode(String text) {
		String rez = new String(Base64.getEncoder().encode(text.getBytes()));
		return rez;
	}// base64encode

	public static String base64decode(String text) {

		String rez = new String (Base64.getDecoder().decode(text.getBytes()));
		return rez;

	}// base64decode

	public void storeInfo() {

		
		
		File f = new File(configFile);
		if(!f.exists()){
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			FileOutputStream of = new FileOutputStream(f);
			p.setProperty(FileSyncInfo.Parameters.BASEDIR.name(), Basedir);
			p.setProperty(FileSyncInfo.Parameters.DESTDIR.name(), Destdir);
			p.setProperty(FileSyncInfo.Parameters.DESTINATION.name(), Destination);
			p.setProperty(FileSyncInfo.Parameters.USER.name(), User);
			p.setProperty(FileSyncInfo.Parameters.PASSWORD.name(), base64encode(Password));
			p.store(of, "Auto generated properties file");
		} catch (Exception e) {
			System.err.println("Unable to write to config.prop file");
		}
		
		
	}

}
