package connectors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import fileDataTypes.FileSyncInfo;
import swingWorkers.FtpWorker;

public class FTPOperations {

	
	private String FileBeingModified;
	private PipedOutputStream writer;
	private SshConnection sconn;
	private FileSyncInfo fInfo;
	private FtpWorker worker;
	
	public FTPOperations(PipedOutputStream writer, SshConnection sconn, FileSyncInfo fInfo) {
		this.writer = writer;
		this.sconn = sconn;
		this.fInfo = fInfo;
		
	}


	public void ftpUploadFile(Path source) {
		FTPClient ftc = new FTPClient();
		try {
			ftc.connect(fInfo.Destination);
			showServerReply(ftc);
			int replyCode = ftc.getReplyCode();
			if (!FTPReply.isPositiveCompletion(replyCode)) {
				writeln("Operation failed. Server reply code: " + replyCode);
				return;
			}
			boolean success = ftc.login(fInfo.User, fInfo.Password);
			showServerReply(ftc);
			if (!success) {
				writeln("Could not login to the server.. unable to upload the file " + source);
				return;
			} else {
				String finaltarget;
				finaltarget = fInfo.Destdir + File.separator + source.toString().substring(fInfo.Basedir.length());
				finaltarget = finaltarget.replace('\\', '/');
				writeln("LOGGED IN SERVER");
				writeln("Going to copy file: " + source + " to destination " + finaltarget);
				FileBeingModified = finaltarget;
				FileInputStream fi = new FileInputStream(new File(source.toString()));
				ftc.storeFile(finaltarget, fi);
				fi.close();
				
				if(showServerReply(ftc)==1)
					this.ftpUploadFile(source);

			}
		} catch (Exception e) {
			writeln("OOPS.. Something wrong happened.. Can't connect to the server!!");
			e.printStackTrace();
		}
	}
	
	public void ftpDownloadFiles(List<String> sources) {
		FTPClient ftc = new FTPClient();
		try {
			ftc.connect(fInfo.Destination);
			showServerReply(ftc);
			int replyCode = ftc.getReplyCode();
			if (!FTPReply.isPositiveCompletion(replyCode)) {
				writeln("Operation failed. Server reply code: " + replyCode);
				return;
			}
			boolean success = ftc.login(fInfo.User, fInfo.Password);
			showServerReply(ftc);
			if (!success) {
				writeln("Could not login to the server.. unable to download the files " + sources.toString());
				return;
			} else {
				for(String source: sources){
				ftpDownloadFile(ftc, source);
				if(worker.isCancelled()){
					writeln("Clone stopped");
					return;
				}
				}
			}
		} catch (Exception e) {
			writeln("OOPS.. Something wrong happened.. Can't connect to the server!!");
			e.printStackTrace();
		}
	}

	private void ftpDownloadFile(FTPClient ftc, String source) {
		
				
				String finaltarget,temptarget;
				finaltarget = fInfo.Basedir + File.separator + source.substring(fInfo.Destdir.length());
				source = source.split("[|]")[0];
				temptarget = finaltarget.replace('/', ' ');
				temptarget = temptarget.replace('\\', ' ');
				String [] arr= temptarget.split(" ");
				boolean isDir = true;
				finaltarget="";
				for(int i =0 ; i< arr.length; i++){
					if(arr[i].equals("")){
						continue;
					}
					if(i==arr.length-1){
						if(arr[i].split("[|]")[1].equalsIgnoreCase("f")){
							isDir  = false;
						}
						arr[i]=arr[i].split("[|]")[0];
						
					}
					finaltarget+=arr[i]+((i==arr.length-1)?"":"/");
				}
				
				if(isDir)
					writeln("Going to download directory: " + finaltarget + " from source " + source);
				
					
				FileBeingModified = finaltarget;
				try {
				FileOutputStream fo;
				if(isDir){
					writer.write(("Creating directory "+finaltarget+"\n").getBytes());
					boolean created = new File(finaltarget).mkdir();
					if(created){
						writeln("Directory created!!");
					}
					else{
						writeln("Directory failed to create");
					}
					return;
				}
				File f = new File(finaltarget);
					fo = new FileOutputStream(f);
					writeln("Going to download file: " + finaltarget + " from source " + source);
					boolean isReceived = ftc.retrieveFile(source, fo);
					if(isReceived)
						writeln("Successfully downloaded the file "+source+"...");
					else
						writeln("Failed to download the file "+source);
					worker.setProg(source);
					fo.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if(showServerReply(ftc)==1)
					this.ftpDownloadFile(ftc, source);
				
			
	}


	public void ftpDeleteLocalFiles(List<String> sources) {
		
		
				for(String source: sources){
					source = source.split("[|]")[0];
					if(worker.isCancelled())
						return;
					File f = new File(source);
					if(f.exists()){
						if(f.isFile())
							f.delete();
						else{
								ftpDeleteLocalFiles(Arrays.asList(f.list()));
								f.delete();
							}
						}
					else{
						System.out.println("File already deleted");
						
						}
	
					worker.setProg(source);
				}
	}
	
	private int showServerReply(FTPClient ftpClient) {
		String[] replies = ftpClient.getReplyStrings();
		if (replies != null && replies.length > 0) {
			for (String aReply : replies) {
				System.out.println("SERVER: " + aReply);
				if (aReply.contains("Could not create file.")) {
					JDialog dialog = new JDialog();
					dialog.setAlwaysOnTop(true);    
					int tobecheckedout = JOptionPane.showConfirmDialog(dialog, 
							"Error: The file " + FileBeingModified
									+ " being modified is not checked-out in your ade view at destination!!\n"
									+ "Do you want to check out this file and proceed?", "Error uploading file", JOptionPane.YES_NO_OPTION);
					
					if(tobecheckedout==0){
						checkoutFile(FileBeingModified);
						return 1;
					}
				}
				
			}
		}
		return 0;
	}

	private void checkoutFile(String file){
		sconn.issueCommand("ade co -nc "+file);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private synchronized void writeln(String str){
		try {
			writer.write(str.getBytes());
			writer.write("\n".getBytes());
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void setWorker(FtpWorker worker) {

		this.worker = worker;
		
	}
	

}
