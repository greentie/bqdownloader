package bqdownloader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.ws.rs.core.HttpHeaders;



public class FileDownloader {

	private static final int DEFAULT_TIME_OUT = 30000;

	private int readTimeOut = 0;
	private int connectTimeOut = 0;

	public FileDownloader() {
		this(DEFAULT_TIME_OUT,DEFAULT_TIME_OUT);
	}
	
	public FileDownloader(int timeOut) {
		this(timeOut,timeOut);
	}

	public FileDownloader(int readTimeOut, int connectTimeOut) {
		this.readTimeOut = readTimeOut;
		this.connectTimeOut = connectTimeOut;

	}
	
	public boolean downloadFile(URL r, String filename) {
		return downloadFile(r,Paths.get(filename));
	}

	public boolean downloadFile(URL r, Path path) {
		HttpURLConnection uc;
		try {
			uc = (HttpURLConnection) r.openConnection();
			uc.setRequestMethod("GET");
			uc.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/pdf");
			uc.setConnectTimeout(connectTimeOut);
			uc.setReadTimeout(readTimeOut);
			uc.setDoOutput(true);
			long filesize = uc.getContentLength();	
			long count = Files.copy(uc.getInputStream(), path,StandardCopyOption.REPLACE_EXISTING);
			if (filesize == -1) {
				System.out.println("Can't get the filesize for url:" + uc);
			}
			return filesize == -1 ? true : count == filesize;
		} catch (IOException ex) {
			ex.printStackTrace(System.out);
			return false;
		}
	}
}
