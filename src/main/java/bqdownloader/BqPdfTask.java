package bqdownloader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BqPdfTask implements Task,Runnable{

	private static final String ROOT = "http://epaper.ynet.com/";
	
	String baseUrl;
	String targetDestintaion;
	List<String> fileList;
	boolean deleteSource=true;
	
	boolean done=false;
	public boolean isDeleteSource() {
		return deleteSource;
	}

	public void setDeleteSource(boolean deleteSource) {
		this.deleteSource = deleteSource;
	}

	String timeStamp;
	
	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public BqPdfTask(String url) {
		this(url,null,new ArrayList<String>());
	}
	
	public BqPdfTask(String baseUrl, String targetDestintaion, List<String> fileList) {
		super();
		this.baseUrl = baseUrl;
		this.targetDestintaion = targetDestintaion;
		this.fileList = fileList;
	}
	public String getBaseUrl() {
		return baseUrl;
	}
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	public String getTargetDestintaion() {
		return targetDestintaion;
	}
	public void setTargetDestintaion(String targetDestintaion) {
		this.targetDestintaion = targetDestintaion;
	}
	public List<String> getFileList() {
		return fileList;
	}

	public void setFileList(List<String> fileList) {
		this.fileList = fileList;
	}
	
	private void getDownloadList() throws IOException {
		Document doc = Jsoup.connect(this.baseUrl).get();
		Element list = doc.body().getElementById("artcile_list_wapper");
		Elements items = list.getElementsByClass("default");
		items.stream().forEach((a) -> {
			fileList.add(ROOT+a.child(1).attr("href").replaceAll("\\.\\./", ""));
		});

	}

	@Override
	public void run() {
		if(targetDestintaion==null) {
			System.out.println(String.format("Didn't set targetDestintaion for %s", this.baseUrl));
		}else {
			try {
				getDownloadList();
			} catch (IOException e) {
				System.out.println(e);
			}
			if(fileList.isEmpty()) {
				System.out.println(String.format("No files need to download for %s", this.baseUrl));
			}else {
				List<Path> paths=new ArrayList<Path>();
				FileDownloader worker=new FileDownloader();
				Path dest=Paths.get(targetDestintaion,timeStamp);
				try {
					Files.createDirectories(dest);
				} catch (IOException e1) {
					e1.printStackTrace(System.out);
				}
				fileList.stream().forEach((String httpurl) -> {
			            try {
			                URL r = new URL(httpurl);
			                String filename = httpurl.substring(httpurl.lastIndexOf("/") + 1);
			                if (filename.matches("bj[\\w\\w]{2}b.*")) {
			                    filename = filename.replaceFirst("bj[\\w\\w]{2}b", "bjqnb");
			                }
			                Path path= Paths.get(targetDestintaion,timeStamp,filename);
			                paths.add(path);
			                boolean isdownloaded = false;
			                for (int i = 0; i < 10 && !isdownloaded; i++) {
			                    isdownloaded = worker.downloadFile(r, path);
			                }
			                if (!isdownloaded) {
			                    throw new IOException();
			                } else {
			                    System.out.println("Finished " + httpurl);
			                }

			            } catch (Exception ex) {
			            	ex.printStackTrace(System.out);
			                System.out.println("fail to download " + httpurl);
			            }
			        });
				 PdfMergeWorker mergeWorker=new PdfMergeWorker(deleteSource);
			     try {
					mergeWorker.mergefiles(paths,Paths.get(targetDestintaion,"bjqnb" + timeStamp+ ".pdf"));
				} catch (IOException e) {
					System.out.println(e);
				}
			     try {
						Files.deleteIfExists(dest);
					} catch (IOException e1) {
						e1.printStackTrace(System.out);
					}
			     
			}
		}
		this.done=true;
	}
	
	public boolean isDone() {
		return this.done;
	}
}
