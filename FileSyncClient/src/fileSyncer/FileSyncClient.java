package fileSyncer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import fileDataTypes.FileDirStatus;

public class FileSyncClient {
	
	String server;
	int port;
	ArrayList<String> updateList = null;
	ArrayList<String> deleteList = null;
	public FileSyncClient(String server, int port){
		this.server = server;
		this.port = port;
	}
	
	public List<String> getUpdateList(){
		return updateList;
	}
	
	public List<String> getDeleteList(){
		return deleteList;
	}
	@SuppressWarnings("unchecked")
	public void findDifference(String RemoteDir, FileDirStatus dirToBeCompared){
		
		
		try {
			Socket scl = new Socket(server, port);
			System.out.println("Connected to server...");
			OutputStream os = scl.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject((FileDirStatus)dirToBeCompared);
			oos.writeObject(RemoteDir);
			oos.flush();
			InputStream is = scl.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
		
			try {
				updateList = (ArrayList<String>) ois.readObject();
				deleteList = (ArrayList<String>) ois.readObject();
				
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			JDialog dialog = new JDialog();
			dialog.setAlwaysOnTop(true);    
			JOptionPane.showMessageDialog(dialog, 
					"Cannot connect to the Filesync server");
			Thread.currentThread().interrupt();
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]){
		FileSyncClient dsc = new FileSyncClient("slc03gcl.us.oracle.com", 60000);
		dsc.findDifference("/scratch/prishriv/view_storage/prishriv_racview2/racdbaas/src/opc/dcs/", new FileDirStatus("C:\\Users\\prishriv.ORADEV\\DCS-new"));
		System.out.println(dsc.getUpdateList());
	}
	
}
