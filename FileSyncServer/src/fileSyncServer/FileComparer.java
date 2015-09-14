package fileSyncServer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import fileDataTypes.FileDirStatus;

public class FileComparer {

	private ArrayList<String> UpdateRemoteList;
	private ArrayList<String> DeleteRemoteList;
	FileDirStatus fdStatHome;
	FileDirStatus fdStatRemote;
	
	public FileComparer(FileDirStatus fdStatHome, FileDirStatus fdStatRemote) {
		this.fdStatHome = fdStatHome;
		this.fdStatRemote = fdStatRemote;
	}
	
	public ArrayList<String> getUpdateList(){
		return UpdateRemoteList;
	}
	public ArrayList<String> getDeleteList(){
		return DeleteRemoteList;
	}

	public void compareFileDirStat() {
		UpdateRemoteList = new ArrayList<String>();
		DeleteRemoteList = new ArrayList<String>();
		if (!fdStatHome.getHash().equals(fdStatRemote.getHash())) {
			File f = new File(fdStatHome.getActualFilePath());
			if (f.isFile()) {
				UpdateRemoteList.add(fdStatHome.getFilePath());
				return;
			}

			List<FileDirStatus> homeList = fdStatHome.getChildren();
			List<FileDirStatus> remoteList = fdStatRemote.getChildren();

			Iterator<FileDirStatus> homeit = homeList.iterator();
			Iterator<FileDirStatus> remoteIt = remoteList.iterator();

			HashMap<String, FileDirStatus> hm = new HashMap<String, FileDirStatus>();
			while (homeit.hasNext()) {
				FileDirStatus fstat = homeit.next();
				hm.put(fstat.getFileName(), fstat);
			}

			while (remoteIt.hasNext()) {
				FileDirStatus fstat = remoteIt.next();
				if (hm.containsKey(fstat.getFileName())) {
					if (!hm.get(fstat.getFileName()).getHash().equals(fstat.getHash())) {
						System.out.println();
						FileComparer subComparer = new FileComparer(hm.get(fstat.getFileName()), fstat);
						subComparer.compareFileDirStat();
						UpdateRemoteList.addAll(subComparer.getUpdateList());
						DeleteRemoteList.addAll(subComparer.getDeleteList());
					}
					hm.remove(fstat.getFileName());
				}
				else{
					addCompletePath(DeleteRemoteList,fstat);
				}
			}
			for (Iterator<String> it = hm.keySet().iterator(); it.hasNext();) {
				String child = it.next();			
				addCompletePath(UpdateRemoteList,hm.get(child));
			}
		}
		
 
	}
	
	private static void addCompletePath(List<String> list, FileDirStatus fstat){
		if(fstat.getChildren().size()==0){

			list.add(fstat.getFilePath());
		}
		else {
			list.add(fstat.getFilePath());
			for(FileDirStatus f: fstat.getChildren()){
				addCompletePath(list, f);
		}
		}
	}
}
