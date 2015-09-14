package fileSyncServer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import fileDataTypes.FileDirStatus;
public class FileSyncServer {

	String fileToSync;
	public FileSyncServer(){
		init();
	}
	private void init(){
	try {
		System.out.println("FileSync Server started..");
		ServerSocket s ;

		s = new ServerSocket(60000);
		
		while(true){
			if(Thread.interrupted())
				break;
		Socket request = s.accept();
		ObjectInputStream ois = new ObjectInputStream(request.getInputStream());
		try {
			FileDirStatus reqObj = (FileDirStatus)ois.readObject();
			fileToSync = (String) ois.readObject();
			System.out.println("Got a new request for.. "+reqObj.getFilePath());
			FileDirStatus fStat = new FileDirStatus(fileToSync);
			FileComparer fc = new FileComparer(fStat, reqObj);
			fc.compareFileDirStat();
			OutputStream os = request.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(fc.getUpdateList());
			oos.writeObject(fc.getDeleteList());
			oos.flush();
			oos.close();
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		
		s.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
	}
	public static void main(String [] args){
		System.out.println("Server running on port 60000 ... ");
		FileSyncServer fs = new FileSyncServer();
		fs.init();
	}
}
