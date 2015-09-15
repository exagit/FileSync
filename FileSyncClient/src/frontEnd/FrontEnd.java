package frontEnd;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.text.DefaultCaret;

import connectors.FTPOperations;
import connectors.SshConnection;
import fileDataTypes.FileDirStatus;
import fileDataTypes.FileSyncInfo;
import fileSyncer.FileSyncClient;
import fileSyncer.FileSyncer;
import swingWorkers.FtpWorker;

public class FrontEnd {

	public static JFrame Jf;
	public static JPanel Jp;
	private static boolean started;
	private static Thread fileSyncThread;
	
	static FileSyncInfo fInfo;
	private static JTextField basedir;
	private static JTextField destdir;
	private static JTextField destination;
	private static JTextField user;
	private static JPasswordField password;
	private static SshConnection sconn;

	public static void main(String args[]) {
		initComponents();
	}

	public static void retrieveFileInfo(){

		fInfo.Basedir = basedir.getText();
		fInfo.Destdir = destdir.getText();
		fInfo.Destination = destination.getText();
		fInfo.User = user.getText();
		fInfo.Password = new String(password.getPassword());
	}

	protected static PipedOutputStream pwrite;
	protected static PipedInputStream pread;
	protected static Thread consoleWriterThread;

	private static FtpWorker cw;
	public static void initComponents() {

		fInfo = new FileSyncInfo("config.prop");

		JTextArea textArea = new JTextArea("");
		DefaultCaret caret = (DefaultCaret)textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		textArea.setEditable(false);
		
		JScrollPane Js = new JScrollPane(textArea);
		Js.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		

		
		Jf = new JFrame("File Synchronizer");

		Jf.setExtendedState(JFrame.MAXIMIZED_BOTH);
		Jp = new JPanel();

		Jf.setContentPane(Jp);

		JButton startButton = new JButton("Start Sync");
		JButton stopButton = new JButton("Stop Sync");
		JButton exitButton = new JButton("Exit Sync");
		JButton cloneButton = new JButton("Clone from Server");
		JButton stopCloneButton = new JButton("Stop Clone");
		stopCloneButton.setEnabled(false);
		
		

		basedir = new JTextField(fInfo.Basedir);
		destdir = new JTextField(fInfo.Destdir);
		destination = new JTextField(fInfo.Destination);
		user = new JTextField(fInfo.User);
		password = new JPasswordField(fInfo.Password);

		JLabel basedirLabel = new JLabel("Base Directory");
		JLabel destinationdirLabel = new JLabel("Destination Directory");
		JLabel destinationLabel = new JLabel("Destination");
		JLabel userLable = new JLabel("User");
		JLabel passwordLable = new JLabel("Password");

		JProgressBar jPb = new JProgressBar(0,100);
		jPb.setVisible(false);
		
		GroupLayout gl = new GroupLayout(Jp);
		gl.setAutoCreateContainerGaps(true);
		gl.setHorizontalGroup(
				gl.createSequentialGroup()
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(gl.createSequentialGroup()
										.addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
												.addComponent(startButton)
												.addComponent(basedirLabel)
												.addComponent(destinationdirLabel)
												.addComponent(destinationLabel)
												.addComponent(userLable)
												.addComponent(passwordLable)
												.addComponent(exitButton)
												)
										.addGap(10)
										.addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
												.addGroup(gl.createSequentialGroup().addComponent(stopButton).addGap(30).addComponent(cloneButton).addGap(30).addComponent(stopCloneButton))
//												.addComponent(stopButton)
												.addComponent(basedir)
												.addComponent(destdir)
												.addComponent(destination)
												.addComponent(user)
												.addComponent(password)
												.addComponent(jPb)
												)
								)
						.addComponent(Js)
						)
				);
		gl.setVerticalGroup(
				gl.createSequentialGroup()
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(startButton)
						.addComponent(stopButton)
						.addComponent(cloneButton)
						.addComponent(stopCloneButton)
						)
				.addGap(5)
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(basedirLabel)
						.addComponent(basedir)
						)
				.addGap(5)
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(destinationdirLabel)
						.addComponent(destdir)
						)
				.addGap(5)
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(destinationLabel)
						.addComponent(destination)
						)
				.addGap(5)
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(userLable)
						.addComponent(user)
						)
				.addGap(5)
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(passwordLable)
						.addComponent(password)
						)
				.addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(exitButton)
						.addComponent(jPb)
						)				
				.addComponent(Js)
				);
		
		
		Jp.setLayout(gl);
		Jf.setVisible(true);

		startButton.addActionListener(new ActionListener() {


			@Override
			public void actionPerformed(ActionEvent e) {
				retrieveFileInfo();
				String validatemsg;
				if (!started){
					validatemsg=validateInfo(fInfo);
					if(validatemsg==null){

						pread = new PipedInputStream();
						pwrite = new PipedOutputStream();
						try {
							pwrite.connect(pread);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						
						fInfo.storeInfo();
						sconn = new SshConnection(fInfo.Destination, fInfo.User, fInfo.Password, pwrite);
						fileSyncThread = new Thread(new FileSyncer(fInfo, pwrite,sconn));
						fileSyncThread.start();
						

						consoleWriterThread = new Thread(new ConsoleWriter(textArea, pread));
						consoleWriterThread.start();
						started = true;
					}
					else {
						JOptionPane.showMessageDialog(Jp, validatemsg);
						
					}
				} else {
					JOptionPane.showMessageDialog(Jp, "Already started syncing", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}

			
		});

		stopButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (started) {
					fileSyncThread.interrupt();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					started = false;
					
				} else {
					JOptionPane.showMessageDialog(Jp, "Cannot stopped already stopped sync service");
				}

			}
		});
		
		exitButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				retrieveFileInfo();
				fInfo.storeInfo();
				if(started)
					fileSyncThread.interrupt();
				System.exit(0);
			}
		});
		
		cloneButton.addActionListener(new ActionListener() {
			

			@Override
			public void actionPerformed(ActionEvent e) {
				cloneButton.setEnabled(false);
				stopCloneButton.setEnabled(true);
				retrieveFileInfo();
				fInfo.storeInfo();
				
				if(started)
					JOptionPane.showMessageDialog(Jp, "Cannot Clone while the sync service is started!!");
				else
				{
					pread = new PipedInputStream();
					pwrite = new PipedOutputStream();
					try {
						pwrite.connect(pread);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					consoleWriterThread = new Thread(new ConsoleWriter(textArea, pread));
					consoleWriterThread.start();
					
//					sconn = new SshConnection(fInfo.Destination, fInfo.User, fInfo.Password, pwrite);
//					
					FileSyncClient dsc = new FileSyncClient(fInfo.Destination, 60000);
					dsc.findDifference(fInfo.Destdir, new FileDirStatus(fInfo.Basedir));
					System.out.println(dsc.getUpdateList());
					FTPOperations fOp = new FTPOperations(pwrite, null , fInfo);
					jPb.setForeground(Color.LIGHT_GRAY);
					jPb.setVisible(true);
					jPb.setStringPainted(true);
					System.out.println("update list: "+dsc.getUpdateList()+" and delete list: "+dsc.getDeleteList());
					cw = new FtpWorker(fOp, dsc.getUpdateList(), dsc.getDeleteList(), jPb);
					cw.addPropertyChangeListener(new PropertyChangeListener() {
						
						@Override
						public void propertyChange(PropertyChangeEvent arg0) {
							if(arg0.getPropertyName().equals("progress")){
								if((int)arg0.getNewValue()==100){
									System.out.println("Done.. cloning");
									stopCloneButton.doClick();
								}
							}
							
						}
					});
					cw.execute();
					try {
						Thread.sleep(1000);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		stopCloneButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				cw.cancel(true);
				jPb.setString("Cloning stopped..");
				jPb.setValue(100);
				try {
					pwrite.write("Done cloning..".getBytes());
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				cloneButton.setEnabled(true);
				stopCloneButton.setEnabled(false);
				
			}
		});

	}

	private static String validateInfo(FileSyncInfo fInfo) {
		if (fInfo.Basedir.equals("") || fInfo.Basedir == null) {

			return "Base directory not specified";
		}
		if (fInfo.Destdir.equals("") || fInfo.Destdir == null) {

			return "Destination directory not specified";
		}
		if (fInfo.Destination.equals("") || fInfo.Destination == null) {

			return "Destination directory not specified";
		}
		if (fInfo.User.equals("") || fInfo.User == null) {

			return "User not specified";
		}
		if (fInfo.Password.equals("") || fInfo.Password == null) {

			return "Password not specified";
		}
		return null;
	}
	
	
	
}

