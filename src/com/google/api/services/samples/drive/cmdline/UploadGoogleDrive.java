package com.google.api.services.samples.drive.cmdline;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

public class UploadGoogleDrive {
	private static final String KEYFILE = "/gestioneservertempogara-4e11d07e6efd.json";

	public static void main(String[] args) {
		String dirpathUpload = args[0];
		if (!dirpathUpload.endsWith("/")) {
			dirpathUpload = dirpathUpload + "/";
		}
		int giorniDiAnzianita = Integer.parseInt(args[1]);
		try {
			InputStream inputStream = UploadGoogleDrive.class.getResourceAsStream(KEYFILE);
			
			System.out.println(inputStream.available());
			
			GoogleCredential credential = GoogleCredential
					.fromStream(inputStream)
					.createScoped(Collections.singleton("https://www.googleapis.com/auth/drive"));

			HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

			Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential)
					.setApplicationName("GestioneServerTempogara").build();

			SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

			String pageToken = null;
			do {
				FileList result = (FileList) drive.files().list().setSpaces("drive")
						.setFields("nextPageToken, files(id, name)").setPageToken(pageToken).execute();
				for (com.google.api.services.drive.model.File file : result.getFiles()) {
					String name = file.getName();
					System.out.printf("Found file: %s (%s)\n", new Object[] { file.getName(), file.getId() });
					if ((!name.endsWith("zip")) && (!name.equals("DB")) && (!name.equals("BackupSito"))) {
						System.out.println("###cancello:" + file.getName() + " (" + file.getId() + ")");
						deleteFile(drive, file.getId());
						System.out.println("cancellato:" + file.getName() + " (" + file.getId() + ")");
					} else {
						Calendar cal = Calendar.getInstance();
						cal.add(5, -1 * giorniDiAnzianita);
						Date dataLimite = new Date(cal.getTimeInMillis());
						if (file.getName().endsWith(".sql.zip")) {
							int idxStart = file.getName().indexOf(".");
							String part = file.getName().substring(idxStart + 1);
							int idxEnd = part.indexOf(".");
							String dataPart = part.substring(0, idxEnd);
							System.out.println("Verifico se:" + dataPart + " e' anteriore a:" + dataLimite);
							Date dtfile = df.parse(dataPart);
							if (dtfile.before(dataLimite)) {
								System.out.println("###cancello:" + file.getName() + " (" + file.getId() + ")");
								deleteFile(drive, file.getId());
								System.out.println("cancellato:" + file.getName() + " (" + file.getId() + ")");
							}
						}
					}
				}
				pageToken = result.getNextPageToken();
			} while (pageToken != null);
			String currDate = df.format(new Date());
			String fileName = dirpathUpload + "dump-icron." + currDate + ".sql.zip";

			View.header1("Avvio upload di " + fileName);
			com.google.api.services.drive.model.File uploadedFile = uploadFile(drive, fileName, "application/zip",
					false);

			Calendar cal = Calendar.getInstance();
			cal.add(5, -1 * giorniDiAnzianita);
			String oldDate = df.format(new Date(cal.getTimeInMillis()));
			String fileNameToDelete = dirpathUpload + "dump-icron." + oldDate + ".sql.zip";

			Properties props = new Properties();
			java.io.File fileProps = new java.io.File("./fileUploaded.propertis");
			if (fileProps.exists()) {
				FileInputStream is = new FileInputStream(fileProps);
				props.load(is);
				is.close();
			}
			props.setProperty(fileName, uploadedFile.getId());
			FileOutputStream os = new FileOutputStream(fileProps);
			props.store(os, new Date().toString());
			os.close();

			View.header1("Success!");
			return;
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
		}
		System.exit(1);
	}

	private static com.google.api.services.drive.model.File uploadFile(Drive drive, String pathname, String mimeType,
			boolean useDirectUpload) throws IOException {
		java.io.File UPLOAD_FILE = new java.io.File(pathname);
		com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
		fileMetadata.setName(UPLOAD_FILE.getName());
		fileMetadata.setParents(Collections.singletonList("1y8Zlzq98WMXoTHpMJ4TYwn6OFqiG6ylG"));

		FileContent mediaContent = new FileContent(mimeType, UPLOAD_FILE);

		Drive.Files.Create insert = drive.files().create(fileMetadata, mediaContent);
		MediaHttpUploader uploader = insert.getMediaHttpUploader();
		uploader.setDirectUploadEnabled(useDirectUpload);
		uploader.setProgressListener(new FileUploadProgressListener());

		com.google.api.services.drive.model.File file = (com.google.api.services.drive.model.File) insert.execute();

		Permission permission = new Permission();
		permission.setEmailAddress("atatarano@gmail.com");
		permission.setRole("owner");
		drive.permissions().create(file.getId(), permission).execute();

		return file;
	}

	private static void deleteFile(Drive drive, String fileId) throws IOException {
		drive.files().delete(fileId).execute();
	}
}
