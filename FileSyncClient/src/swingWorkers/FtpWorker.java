package swingWorkers;

import java.util.List;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import connectors.FTPOperations;

public class FtpWorker extends SwingWorker<Void, String> {
	FTPOperations fOp;
	List<String> updateFiles;
	List<String> deleteFiles;
	private int filesDone;
	private JProgressBar jPb;
	Object ob;
	private int totalFiles;
	private String task;

	public FtpWorker(FTPOperations fOp, List<String> updateFiles, List<String> deleteFiles, JProgressBar jPb) {
		filesDone = 0;
		this.fOp = fOp;
		this.updateFiles = updateFiles;
		this.deleteFiles = deleteFiles;
		this.jPb = jPb;
		fOp.setWorker(this);
	}

	@Override
	protected Void doInBackground() throws Exception {
		if(updateFiles.size()!=0){
			totalFiles = updateFiles.size();
			task = "Downloading: ";
			System.out.println("here in do in background");
			fOp.ftpDownloadFiles(updateFiles);
			System.out.println("here2 in do in background");
		}
		if(deleteFiles.size()!=0){
			filesDone = 0;
			totalFiles = deleteFiles.size();
			task = "Deleting: ";
			fOp.ftpDeleteLocalFiles(deleteFiles);
		}
		setProgress(100);
		return null;
	}

	@Override
	protected void process(List<String> fileDone) {
		for (String file : fileDone) {
			int progress = ((++filesDone * 100) / totalFiles);
			System.out.println("Setting progress value for "+task+" and file "+file+" to " + progress);
			jPb.setString(task+file+" : "+progress+"%");
			jPb.setValue(progress);
		}
	}

	public void setProg(String fileDone) {
		publish(fileDone);
	}
	
}