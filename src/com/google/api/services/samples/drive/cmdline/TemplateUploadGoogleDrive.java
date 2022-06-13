package com.google.api.services.samples.drive.cmdline;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Pattern;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

public class TemplateUploadGoogleDrive {
	private static final String KEYFILE = "/gestioneservertempogara-4e11d07e6efd.json";

	public static void main(String[] args) {
		String fileToUpload = args[0];
		
		//glob:*.{zip,tgz}
		String patterns=args[1];
		
		String driveDirectoryId=args[2];
		
		int giorniDiAnzianita = Integer.parseInt(args[3]);
		try {
			InputStream inputStream = TemplateUploadGoogleDrive.class.getResourceAsStream(KEYFILE);
			
			System.out.println(inputStream.available());
			
			GoogleCredential credential = GoogleCredential
					.fromStream(inputStream)
					.createScoped(Collections.singleton("https://www.googleapis.com/auth/drive"));

			HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

			Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential)
					.setApplicationName("GestioneServerTempogara").build();

			Calendar cal = Calendar.getInstance();
			cal.add(5, -1 * giorniDiAnzianita);
			Date dataLimite = new Date(cal.getTimeInMillis());

			String pageToken = null;
			do {
				FileList result = (FileList) drive.files().list().setSpaces("drive")
						.setFields("nextPageToken, files(id, name, modifiedTime)").setPageToken(pageToken).execute();
				for (com.google.api.services.drive.model.File file : result.getFiles()) {
					String name = file.getName();
					System.out.printf("Found file: %s (%s)\n", new Object[] { file.getName(), file.getId() });
					boolean matchTemplate=searchWithWc(Paths.get(file.getName()), patterns);
					if (!matchTemplate && (!name.equals("DB")) && (!name.equals("BackupSito"))) {
						System.out.println("###cancello:" + file.getName() + " (" + file.getId() + ")");
						deleteFile(drive, file.getId());
						System.out.println("cancellato:" + file.getName() + " (" + file.getId() + ")");
					} else {
						if (matchTemplate) {
							DateTime modtime = file.getModifiedTime();
							Date dtfile=new Date(modtime.getValue());
							System.out.println("Verifico se:" + dtfile + " e' anteriore a:" + dataLimite);
							if (dtfile.before(dataLimite) /*|| file.getName().indexOf("newgo-1.215")!=-1*/) {
								System.out.println("###cancello:" + file.getName() + " (" + file.getId() + ")");
								deleteFile(drive, file.getId());
								System.out.println("cancellato:" + file.getName() + " (" + file.getId() + ")");
							}
						}
					}
				}
				pageToken = result.getNextPageToken();
			} while (pageToken != null);
			String fileName = fileToUpload;

			View.header1("Avvio upload di " + fileName);
			com.google.api.services.drive.model.File uploadedFile = uploadFile(drive, driveDirectoryId, fileName, "application/zip",
					false);

			Properties props = new Properties();
			java.io.File fileProps = new java.io.File(fileName);
			fileProps = new java.io.File(fileProps.getParentFile().getAbsolutePath()+ "/fileUploaded.propertis");
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

	private static com.google.api.services.drive.model.File uploadFile(Drive drive, String driveDirectoryId, String pathname, String mimeType,
			boolean useDirectUpload) throws IOException {
		java.io.File UPLOAD_FILE = new java.io.File(pathname);
		com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
		fileMetadata.setName(UPLOAD_FILE.getName());
		fileMetadata.setParents(Collections.singletonList(driveDirectoryId));

		FileContent mediaContent = new FileContent(mimeType, UPLOAD_FILE);

		Drive.Files.Create insert = drive.files().create(fileMetadata, mediaContent);
		MediaHttpUploader uploader = insert.getMediaHttpUploader();
		uploader.setDirectUploadEnabled(useDirectUpload);
		uploader.setProgressListener(new FileUploadProgressListener());

		com.google.api.services.drive.model.File file = (com.google.api.services.drive.model.File) insert.execute();

		return file;
	}

	private static void deleteFile(Drive drive, String fileId) throws IOException {
		drive.files().delete(fileId).execute();
	}
	
	private static boolean searchWithWc(Path fileName, String pattern) throws IOException {
		FileSystem fs = FileSystems.getDefault();
		PathMatcher matcher = fs.getPathMatcher(pattern);
		Path name = fileName;
		return matcher.matches(name);
	}
	
}
