package fileSyncer;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.io.PipedOutputStream;
import java.net.SocketException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import connectors.FTPOperations;
import connectors.SshConnection;
import fileDataTypes.FileSyncInfo;
/**
 * Example to watch a directory (or tree) for changes to files.
 */

public class FileSyncer implements Runnable{

	private WatchService watcher;
	private Map<WatchKey, Path> keys;
	private boolean trace = false;
	private String FileBeingModified;

	private FileSyncInfo fInfo;
	private PipedOutputStream writer;
	private SshConnection sconn;

	@SuppressWarnings("unchecked")
	<T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	/**
	 * Register the given directory with the WatchService
	 */
	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		if (trace) {
			Path prev = keys.get(key);
			if (prev == null) {
				writeln(String.format("register: %s\n", dir));
			} else {
				if (!dir.equals(prev)) {
					writeln(String.format("update: %s -> %s\n", prev, dir));
				}
			}
		}
		keys.put(key, dir);
	}

	/**
	 * Register the given directory, and all its sub-directories, with the
	 * WatchService.
	 */
	private void registerAll(Path path) {
		// register directory and sub-directories
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override 
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					register(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a WatchService and registers the given directory
	 */
	private void init() {
		try {
			this.watcher = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.keys = new HashMap<WatchKey, Path>();

			writeln(String.format("Scanning %s ...\n", fInfo.Basedir));
			registerAll(Paths.get(fInfo.Basedir));
			writeln("Running...");
		
		// enable trace after initial registration
		this.trace = true;
		
	}

	public FileSyncer(FileSyncInfo fInfo, PipedOutputStream writer, SshConnection sconn) {
		// TODO Auto-generated constructor stub
		watcher = null;
		keys = null;
		this.fInfo = fInfo;
		this.writer = writer;
		this.sconn = sconn;
		sconn.start();
		String arr[] = fInfo.Destdir.split("/");
		if(arr.length>=5){
			if(arr[3].equals("view_storage")){
			System.out.println("Going to enter view "+arr[4]);
			sconn.issueCommand("ade useview "+arr[4]);
			}
		}
		init();
		
	}
 
	/**
	 * Process all events for keys queued to the watcher
	 * 
	 * @throws IOException
	 * @throws SocketException
	 */
	void processEvents() {
		FTPOperations fOp = new FTPOperations(writer, sconn, fInfo);
		for (;;) {
			
			WatchKey key;
			try {
				key = watcher.take();
				Thread.sleep(3000);
			} catch (InterruptedException x) {
				try {
					writeln("Stopped");
					Thread.sleep(1000);
					writer.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}

			Path dir = keys.get(key);
			if (dir == null) {
				writeln("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {

				Kind<?> kind = event.kind();
				if (kind == OVERFLOW) {
					continue;
				}

				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir.resolve(name);

				// if directory is created, and watching recursively, then
				// register it and its sub-directories

				if (kind == ENTRY_MODIFY) {
					if (child.toString().endsWith(".java")) {
						writeln("Trying to upload " + child);
						fOp.ftpUploadFile(child);
					}
				}
				if (kind == ENTRY_CREATE) {
					if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
						registerAll(child);
					}
				}
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);

				// all directories are inaccessible
				if (keys.isEmpty()) {
					break;
				}
			}
		}
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
			System.out.println("Going to write "+str); 
			writer.write(str.getBytes());
			writer.write("\n".getBytes());
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void run() {
		
		writeln("BaseDir to be monitored is " + fInfo.Basedir);
		processEvents();
		
		
		
	}
}