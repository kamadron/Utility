package pl.mk.utility;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerFileSecurity {

	private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

	private static Logger log = LogManager.getLogger(ServerFileSecurity.class);

	private final String serverSaveDir;

	private final String backupDirBase;

	private final SimpleDateFormat sdf;

	public ServerFileSecurity(final String serverSaveDir, final SimpleDateFormat sdf, final String backupDirBase) {
		super();
		this.serverSaveDir = serverSaveDir;
		this.sdf = sdf;
		this.backupDirBase = backupDirBase;
	}

	public void checkServerDateAndBackup(final Date serverToDownloadDate, final List<String> serverFileNames)
			throws Exception {
		String backupDir = null;

		for (final String serverFileName : serverFileNames) {
			final File serverFile = new File(this.serverSaveDir + ServerFileSecurity.FILE_SEPARATOR + serverFileName);

			if (serverFile.exists()) {
				final BasicFileAttributes attr = Files.readAttributes(serverFile.toPath(), BasicFileAttributes.class);
				final Date localServerDate = new Date(attr.lastModifiedTime().toMillis());

				checkIfServerChangeIsNeeded(serverToDownloadDate, localServerDate);

				/* first file */
				if (backupDir == null) {
					backupDir = backupDirBase + ServerFileSecurity.FILE_SEPARATOR + this.sdf.format(localServerDate);
					final File backupDirFile = new File(backupDir);
					if (backupDirFile.exists()) {
						ServerFileSecurity.log.info("Backup directory of this server already exists.");
					} else {
						backupDirFile.mkdir();
					}
				}

				ServerFileSecurity.log.info("Backup old server file (" + serverFileName + ")\n\t from: "
						+ this.serverSaveDir + ")\n\t to:   " + backupDir + "");
				if (!backup(serverFileName, serverFile, backupDir)) {
					throw new Exception("Backup finished INCORRECTLY. ABORTING OPERATION...");
				}
			} else {
				ServerFileSecurity.log.info("No file to backup! " + serverFile.getPath());
			}

		}
	}

	private void checkIfServerChangeIsNeeded(final Date serverToDownloadDate, final Date localServerDate)
			throws Exception {
		final long serverToDownloadTime = serverToDownloadDate.getTime();
		final long localServerTime = this.sdf.parse(this.sdf.format(localServerDate)).getTime();
		if (serverToDownloadTime < localServerTime) {
			throw new Exception("Local disk server is newer than on GDrive. Aborting");
		} else if (serverToDownloadTime == localServerTime) {
			throw new Exception("Local disk server is the same as on GDrive. Aborting");
		}
	}

	/**
	 * @return TRUE if backup finished successfully or server backup already exists.
	 *         FALSE if something failed.
	 */
	private boolean backup(final String fileName, final File file, final String backupDir) throws IOException {
		final File backupFile = new File(backupDir + ServerFileSecurity.FILE_SEPARATOR + fileName);
		final Path resultPath = Files.move(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		if (resultPath != null) {
			ServerFileSecurity.log.info("Backup " + fileName + " COMPLETED");
			return true;
		} else {
			ServerFileSecurity.log.info("Backup " + fileName + " FINISHED INCORRECTLY. file reverted.");
			return false;
		}
	}

}
