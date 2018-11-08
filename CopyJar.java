import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class CopyJar {
	private static final String BACKUP_TEMP_DIR = "backup_temp_dir";

	public enum Command {
		BACKUPJAR, COPYJAR, RESTOREJAR
	}

	private static String separator;

	public static void main(String args[]) {
		try {

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			File f = new File("C:\\Users\\prsrivas\\Desktop\\idea\\CopyUtils\\configs\\config.json");
			Targets nodeList = mapper.readValue(f, Targets.class);

			String pwd = args[0];
			if (pwd.contains("\\")) {
				separator = "\\";
			} else {
				separator = "/";
			}

			String command = args[1];

			new CopyJar().copyFromTargetDir(Command.valueOf(command), pwd, nodeList);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private Session initJschSession(TargetNode targetNode) throws JSchException {
		Session session = new JSch().getSession(targetNode.getUser(), targetNode.getHost(), 22);
		session.setPassword(targetNode.getPassword());
		session.setConfig("StrictHostKeyChecking", "no");
		// to skip kerberos authentication
		session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
		session.connect();
		System.out.println("session connected..");
		return session;
	}

	void copyFromTargetDir(Command cmd, String dir, Targets nodeList)
			throws XPathExpressionException, IOException, InterruptedException {

		if (isTargetDirPresent(dir)) {
			System.out.println("Copying jar from dir:" + dir + separator + "target");

			ArtifactInfo artifactInfo = obtainArtifactInfoFromPom(dir);
			String fullyQualifiedArtifactName = getFileNameUnderTarget(dir, artifactInfo.getArtifactFilename());
			if (fullyQualifiedArtifactName.isEmpty()) {
				System.err.println(
						"Could not find artifact file with name containing " + artifactInfo.getArtifactFilename());
				return;
			}
			System.out.println("source artifact:" + fullyQualifiedArtifactName);
			JSch jsch;
			Session session = null;

			try {

				for (TargetNode node : nodeList.getNodes()) {
					if (!node.getEnabled())
						continue;
					System.out.println(
							"Processing node : " + node.getUser() + "@" + node.getHost() + ":" + node.getTarget());

					session = initJschSession(node);

					List<String> targetFilePath = getTargetFilePath(artifactInfo, node.getTarget(), session);
					if (cmd.equals(Command.BACKUPJAR)) {
						backupFile(session, targetFilePath);
					}
					if (cmd.equals(Command.COPYJAR)) {
						copyFile(session, fullyQualifiedArtifactName, targetFilePath);
					}
					if (cmd.equals(Command.RESTOREJAR)) {
						restoreFile(session, targetFilePath);
					}

					session.disconnect();
				}
			} catch (JSchException e) {
				e.printStackTrace();
			}
		} else {
			// recursively enter each directory and check for artifact dir with target
			// folder in it
			for (String file : new File(dir).list((d, file) -> new File(d, file).isDirectory())) {
				String childDirPath = dir + separator + file;
				copyFromTargetDir(cmd, childDirPath, nodeList);
			}
		}
	}

	private List<String> getTargetFilePath(ArtifactInfo artifactInfo, String targetdir, Session session) {

		String targetArtifactName;
		if (isArtifactWar(artifactInfo)) {
			if (isIHSArtifact(artifactInfo)) {
				targetArtifactName = "ihs.war";
			} else {
				targetArtifactName = "access.war";
			}
		} else {
			if (artifactInfo.getArtifactFilename().equalsIgnoreCase("com.infa.products.ldm.ingestion.server.scala")) {
				targetArtifactName = "ingest.jar";
			} else {
				targetArtifactName = artifactInfo.getArtifactFilename();
			}
		}
		System.out.println("target Artifact name is " + targetArtifactName);

		String targetArtifactPath = lookupArtifactPath(targetArtifactName, targetdir, session);

		System.out.println("looked up artifact path is " + targetArtifactPath);

		if (targetArtifactPath.isEmpty()) {
			return new ArrayList<>();
		}

		// if(isArtifactWar(artifactInfo))
		// cleanupExtractedWar(targetArtifactPath, session);

		List<String> list = new ArrayList<String>(Arrays.asList(targetArtifactPath.split("\n")));
		list.removeIf((path) -> path.contains("EBF") || path.contains(BACKUP_TEMP_DIR));
		return list;
	}

	private static boolean isIHSArtifact(ArtifactInfo artifactInfo) {
		return artifactInfo.getArtifactFilename().contains("com.infa.products.ihs");
	}

	private static boolean isArtifactWar(ArtifactInfo artifactInfo) {
		if (artifactInfo.getPackagingType().equalsIgnoreCase("war"))
			return true;
		return false;
	}

	private static boolean isTargetDirPresent(String path) {
		File targetDir = new File(path + separator + "target");
		if (!targetDir.exists()) {
			return false;
		}
		return true;
	}

	private static ArtifactInfo obtainArtifactInfoFromPom(String dir) throws XPathExpressionException {
		ArtifactInfo artifactInfo;
		try {
			File pomFile = new File(dir, "pom.xml");

			if (!pomFile.exists()) {
				System.err.println("Could not determine artifact id.. since pom.xml does not exist");
				System.exit(1);
			}

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(pomFile);

			XPathFactory xpf = XPathFactory.newInstance();
			XPath xpath = xpf.newXPath();
			Node projectNode = getNodeByName(doc.getChildNodes(), "project");
			String artifactId = xpath.evaluate("artifactId", projectNode);
			String artifactVersion = xpath.evaluate("version", projectNode);
			String packagingType = xpath.evaluate("packaging", projectNode);
			if (packagingType.equals("infa-bundle"))
				packagingType = "jar";
			artifactInfo = new ArtifactInfo(artifactId, artifactVersion, packagingType);

			if (artifactId.isEmpty()) {
				System.err.println("Could not determine artifact id.. since no tag artifactId present in pom.xml");
				System.exit(1);
			}
			return artifactInfo;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Node getNodeByName(NodeList list, String nodeName) {
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeName().equals(nodeName)) {
				return list.item(i);
			}
		}
		System.out.println("node not found:" + nodeName);
		System.exit(1);
		return null;
	}

	private static String getFileNameUnderTarget(String dir, String artifactName) {
		System.out.println("looking for artifact " + artifactName + " under dir:" + dir);
		File locFile = new File(dir, "target");
		for (File f : locFile.listFiles((f) -> f.isFile())) {
			if (f.getName().contains(artifactName) && (f.getName().endsWith("jar") || f.getName().endsWith("war"))
					&& !f.getName().contains("sources") && !f.getName().contains("uber")) {
				return f.getAbsolutePath();
			}
		}
		return "";
	}

	private static String executeSessionCommand(Session session, String command) {
		try {
			System.out.println("Executing command '" + command + "' : session" + session);
			ChannelExec ch = (ChannelExec) session.openChannel("exec");
			ch.setCommand(command);
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			InputStream inp = ch.getInputStream();
			ch.connect();
			int readByte;
			while ((readByte = inp.read()) != -1) {
				bo.write(readByte);
			}
			ch.disconnect();
			return bo.toString().trim();
		} catch (Exception e) {
			System.err.println(e);
			return "";
		}
	}

	private static String lookupArtifactPath(String targetArtifactName, String artifactTargetDir, Session session) {
		System.out.println("Searching for artifact " + targetArtifactName + " in dir : " + artifactTargetDir);
		String targetArtifactPath = executeSessionCommand(session,
				"find " + artifactTargetDir + "|grep " + targetArtifactName);
		return targetArtifactPath;
	}

	private static String getParentDir(String targetJarFile) {
		return Paths.get(targetJarFile).getParent().toString().replaceAll("\\\\", "/");
	}

	private static String getFilename(String targetJarFile) {
		return Paths.get(targetJarFile).getFileName().toString().replaceAll("\\\\", "/");
	}

	private static String getPathAll(String... paths) {
		return Paths.get("", paths).toString().replaceAll("\\\\", "/");
	}

	private static void backupFile(Session session, List<String> targetFilePath) {
		if (targetFilePath.size() == 0)
			return;
		for (String targetJarFile : targetFilePath) {
			String targetParent = getParentDir(targetJarFile);
			String backupDir = getPathAll(targetParent, BACKUP_TEMP_DIR);
			System.out.println("Backing up file " + targetJarFile + " to " + backupDir);
			executeSessionCommand(session, "mkdir -p " + backupDir);
			executeSessionCommand(session, "cp " + targetJarFile + " " + backupDir);
		}
	}

	private static void restoreFile(Session session, List<String> targetFilePath) {
		if (targetFilePath.size() == 0)
			return;
		for (String targetJarFile : targetFilePath) {
			String targetParent = getParentDir(targetJarFile);
			String jarName = getFilename(targetJarFile);
			String backedUpFile = getPathAll(targetParent, BACKUP_TEMP_DIR, jarName);

			System.out.println("Restoring the file " + backedUpFile + " to " + targetParent);
			executeSessionCommand(session, "cp " + backedUpFile + " " + targetParent);
		}
	}

	private static void copyFile(Session session, String sourceJarName, List<String> targetFilePath) {
		if (sourceJarName.isEmpty() || targetFilePath.size() == 0)
			return;
		ChannelSftp ch = null;
		try {
			for (String targetJarName : targetFilePath) {
				System.out.println("Copying file " + sourceJarName + " to " + targetJarName);
				ch = (ChannelSftp) session.openChannel("sftp");
				ch.connect();
				ch.put(sourceJarName, targetJarName);
			}
		} catch (JSchException | SftpException e) {
			System.err.println(e);
		} finally {
			if (ch != null)
				ch.disconnect();
		}
	}

	static class ArtifactInfo {

		String getArtifactFilename() {
			return artifactFilename;
		}

		String getPackagingType() {
			return packagingType;
		}

		String getFullArtifactName() {
			return artifactFilename + "-" + artifactVersion + "." + packagingType;
		}

		private final String artifactFilename;
		private final String artifactVersion;
		private final String packagingType;

		ArtifactInfo(String artifactFilename, String artifactVersion, String packagingType) {
			this.artifactFilename = artifactFilename;
			this.artifactVersion = artifactVersion;
			this.packagingType = packagingType;
		}

	}
}
