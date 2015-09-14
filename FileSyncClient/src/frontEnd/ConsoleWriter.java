package frontEnd;

import java.io.IOException;
import java.io.PipedInputStream;

import javax.swing.JTextArea;

public class ConsoleWriter implements Runnable {
	JTextArea Jt;
	private PipedInputStream reader;

	public ConsoleWriter(JTextArea Jt, PipedInputStream reader) {
		this.Jt = Jt;
		this.reader = reader;
	}

	@Override
	public void run() {
		int temp;
		char c;
		 try {
			while ((temp = reader.read())!= -1) {
				c = (char) temp;
				
					Jt.append(c+"");
			    }
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

}
