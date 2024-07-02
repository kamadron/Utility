package pl.mk.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZipConverter {

	public static final String ZIP_EXTENSION = ".zip";

	private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

	private Integer zipLevel = 4;

	private static Logger log = LogManager.getLogger(ZipConverter.class);

	public ZipConverter() {
		super();
	}

	public ZipConverter(Integer zipLevel) {
		super();
		this.zipLevel = zipLevel;
	}

	/**
	 * Returns output zipped file.
	 */
	public File zip(String outputZipFilePath, String... filePathsToZip) throws Exception {
		outputZipFilePath = prepareZipOutputFile(outputZipFilePath);
		log.info("Start zipping server files into {}", outputZipFilePath);

		try (final FileOutputStream fos = new FileOutputStream(outputZipFilePath);
				ZipOutputStream zipOut = new ZipOutputStream(fos);) {
			zipOut.setLevel(zipLevel);

			for (String filePath : filePathsToZip) {
				File file = new File(filePath);
				zipFile(zipOut, file);
			}
		}
		return returnOutputZip(outputZipFilePath);
	}

	/**
	 * Returns output zipped file.
	 */
	public File zip(String outputZipFilePath, List<File> filesToZip) throws Exception {
		outputZipFilePath = prepareZipOutputFile(outputZipFilePath);
		log.info("Start zipping server files into {}", outputZipFilePath);

		try (final FileOutputStream fos = new FileOutputStream(outputZipFilePath);
				ZipOutputStream zipOut = new ZipOutputStream(fos);) {
			zipOut.setLevel(zipLevel);

			for (File file : filesToZip) {
				zipFile(zipOut, file);
			}
		}
		return returnOutputZip(outputZipFilePath);
	}

	private String prepareZipOutputFile(String outputZipFilePath) throws Exception {
		if (outputZipFilePath == null || outputZipFilePath.isEmpty()) {
			throw new Exception("Output zip file path is empty! Aborting...");
		}
		if (!outputZipFilePath.toLowerCase().endsWith(ZIP_EXTENSION.toLowerCase())) {
			outputZipFilePath += ZIP_EXTENSION;
		}
		return outputZipFilePath;
	}

	private File returnOutputZip(String outputZipFilePath) throws Exception {
		File outputFile = new File(outputZipFilePath);
		if (!outputFile.exists()) {
			throw new Exception("Error while zipping file - zipping finished but output file do not exists??");
		}
		return outputFile;
	}

	private void zipFile(ZipOutputStream zipOut, File fileToZip) throws IOException, FileNotFoundException {
		log.info("\tzipping {}", fileToZip.getAbsolutePath());
		try (FileInputStream fis = new FileInputStream(fileToZip);) {
			ZipEntry zipEntry = new ZipEntry(fileToZip.getName());

			zipOut.putNextEntry(zipEntry);

			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zipOut.write(bytes, 0, length);
			}
		}
	}

	/**
	 * Unzip all files from zipped file under passed path.
	 * 
	 * @param zipFilePath
	 * @param destDirPath        - Unzip output directory. If null passed - files
	 *                           will be unzipped into zip file directory.
	 * @param setZipModifiedTime - pass true if want to pass zip file
	 *                           lastModificationDate into unzipped files
	 * @throws IOException
	 */
	public void unzip(String zipFilePath, String destDirPath, boolean setZipModifiedTime) throws IOException {
		File zipFile = new File(zipFilePath);
		log.info("Start unzipping server files");
		if (!zipFile.exists())
			throw new IOException("Zip file do not exists! file path: " + zipFilePath);

		if (destDirPath == null || destDirPath.isEmpty()) {
			destDirPath = zipFile.getParent();
		}

		final BasicFileAttributes zipFileAttr = Files.readAttributes(zipFile.toPath(), BasicFileAttributes.class);
		FileTime lastModifiedTime = zipFileAttr.lastModifiedTime();
		File destDir = new File(destDirPath);
		byte[] buffer = new byte[1024];

		try (FileInputStream fis = new FileInputStream(zipFilePath); ZipInputStream zis = new ZipInputStream(fis);) {
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				File newFile = newFile(destDir, zipEntry);
				if (zipEntry.isDirectory()) {
					if (!newFile.isDirectory() && !newFile.mkdirs()) {
						throw new IOException("Failed to create directory " + newFile);
					}
				} else {
					// fix for Windows-created archives
					File parent = newFile.getParentFile();
					if (!parent.isDirectory() && !parent.mkdirs()) {
						throw new IOException("Failed to create directory " + parent);
					}

					// write file content
					try (FileOutputStream fos = new FileOutputStream(newFile);) {
						int len;
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
					}
					Files.setAttribute(newFile.toPath(), "basic:lastModifiedTime", lastModifiedTime,
							LinkOption.NOFOLLOW_LINKS);
					Files.setAttribute(newFile.toPath(), "basic:creationTime", lastModifiedTime,
							LinkOption.NOFOLLOW_LINKS);
				}
				zipEntry = zis.getNextEntry();
			}
		}
	}

	public File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());

		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		log.info("preparing file {}", destFilePath);
		if (!destFilePath.startsWith(destDirPath + FILE_SEPARATOR)) {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}

		return destFile;
	}
}
