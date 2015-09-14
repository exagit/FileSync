package connectors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshConnection{
	
	String User;
	String Password;
	String Destination;
	InputStream inp;
	OutputStream op;
	private Channel ch;
	private Session s;
	private PrintStream ps;
	PipedOutputStream writer;
	
	public SshConnection(String destination, String user, String password, PipedOutputStream writer) {
		this.User = user;
		this.Password = password;
		this.Destination = destination;		
		this.writer = writer;
	}
	
	
	public InputStream getReadStream(){
		return inp;
	}
	
	public OutputStream getWriteStream(){
		return op;
	}


	public void start() {
		JSch sch = new JSch();
		try {
			s = sch.getSession(User, Destination);
			s.setConfig("StrictHostKeyChecking", "no");
			s.setPassword(Password);
			s.connect(30000);
			ch = s.openChannel("shell");
//			ch.setOutputStream(writer);
			op = ch.getOutputStream();
			ps = new PrintStream(op,true);
			ch.connect();
			}
			catch(IOException | JSchException e){
				e.printStackTrace();
			}
		
	}
	
	public void issueCommand(String... cmd){
		for(int i=0; i<cmd.length;i++){
			ps.println(cmd[i]);
			
		}
	}
	public void stop(){
		ps.print("exit");
		ch.disconnect();
		s.disconnect();
	}

}
