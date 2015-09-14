package fileDataTypes;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class FileDirStatus implements Serializable{
	
	private static final long serialVersionUID = 2145233230138616975L;
	private String filename;
	private String contentHash;
	List<FileDirStatus> childrenlist;
	private transient MessageDigest md;

	public FileDirStatus(String filePath) {
		File f = new File(filePath);
		if(f.isFile())
			filename = filePath + "|f";
		else
			filename = filePath + "|d";
		childrenlist = new ArrayList<FileDirStatus>();
		try {
			md = MessageDigest.getInstance("SHA1");
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		populateChildren();
		computeHash();
	}

	private void populateChildren() {
		File f = new File(getActualFilePath());
		if (!f.isDirectory()){
			return;
		}
		String[] children = f.list();
		for (String child : children) {
			if(child.startsWith(".ade_path"))
				continue;
			FileDirStatus childfile = new FileDirStatus(getActualFilePath() + File.separator + child);
			childrenlist.add(childfile);
		}
	}

	private void computeHash() {
		File f = new File(getActualFilePath());
		if(f.isFile()){
			try {
				md.update(Files.readAllBytes(Paths.get(getActualFilePath())));
				contentHash = bytesToHex(md.digest());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		List<String> hashList = new ArrayList<String>();
		for(FileDirStatus child: childrenlist){
			hashList.add(child.getHash());
		}
		Collections.sort(hashList);
		
		md.update(Arrays.toString(hashList.toArray()).getBytes());
		contentHash = bytesToHex(md.digest());
	}

	 public String bytesToHex(byte[] b) {
	      char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7',
	                         '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	      StringBuffer buf = new StringBuffer();
	      for (int j=0; j<b.length; j++) {
	         buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
	         buf.append(hexDigit[b[j] & 0x0f]);
	      }
	      return buf.toString();
	   }
	 
	public String getHash() {
		return contentHash;
		
	}
	
	public String getFilePath() {
		return filename;
	}
	
	public String getFileName() {
		String temp = filename.replace('\\', '/');
		String arr [] = temp.split(Pattern.quote("/"));
		for(int i = arr.length-1 ; i>=0 ; i-- )
		{
			if(!arr[i].equals(""))
				return arr[i];
		}
		return arr[0];
		
	}

	public String getActualFilePath(){
		return filename.split("[|]")[0];
	}
	public static void main(String args[]) {
		FileDirStatus f = new FileDirStatus(args[0]);
		System.out.println("content hash for "+f.getFileName()+" is "+f.getHash()
		+" and children are "+Arrays.toString(f.getChildren().toArray()));
	}

	public List<FileDirStatus> getChildren() {

		File f = new File(getActualFilePath());
		List<FileDirStatus> al = this.childrenlist;
		if(f.isFile())
			return al;
		return al;
	}
}
