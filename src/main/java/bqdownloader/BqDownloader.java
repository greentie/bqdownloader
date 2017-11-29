package bqdownloader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



public class BqDownloader {

	private static final String ROOT = "http://epaper.ynet.com/";
	private static final String DATEPARTTEN="[\\d]{8}";
	private static final long ONEDAY=1000L*60*60*24;
	
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
	

	String packagePath="";
			
	boolean removeSource=false;
	
	public  static Map<String,BqPdfTask> getTodayTask() throws ParseException, IOException {
		Map<String,BqPdfTask> tasks;
		String url = "";
        Document doc = Jsoup.connect(ROOT).get();
        Element head = doc.head();
        Elements meta = head.getElementsByTag("META");
        Iterator<Element> iterator = meta.iterator();
        while (iterator.hasNext()) {
            Element e = iterator.next();
            if (e.hasAttr("HTTP-EQUIV")) {
                if (e.attr("HTTP-EQUIV").equals("REFRESH")) {
                    String content = e.attr("CONTENT");
                    url = content.substring(content.indexOf("URL=") + 4);
                }
            }
        }
        if(url.isEmpty()) {
        	tasks=taskBuilder(null);
        }else {
        	tasks=new HashMap<String,BqPdfTask>();
        	BqPdfTask task=new BqPdfTask(ROOT+url);
        	task.setTimeStamp(formatter.format(new Date()));
        	tasks.put(url,task);
        }
        return tasks;
	}
	
	public static Map<String,BqPdfTask> taskBuilder(String taskTime) throws ParseException {
		String date=taskTime;
		if(date==null) {
			date=formatter.format(new Date());
		}
		Date startTime=formatter.parse(date);
		Date endTime=formatter.parse(date);
		endTime.setTime(endTime.getTime()+ONEDAY);
		return taskBuilder(formatter.format(startTime),formatter.format(endTime));
	}
	
	private static String dateBuilder(String date) {	
		return String.format("html/%s-%s/%s/node_1331.htm",  date.substring(0, 4), date.substring(4, 6), date.substring(6));
	}
	
	
	public static Map<String,BqPdfTask> taskBuilder(String start,String end) throws ParseException {
		Map<String,BqPdfTask> tasks=new HashMap<String,BqPdfTask>();
		if(start==null||end==null||!start.matches(DATEPARTTEN)||!end.matches(DATEPARTTEN)) {
			throw new IllegalArgumentException();
		}
		else {
			Date startTime=formatter.parse(start);
			Date endTime=formatter.parse(end);
			for(Date time=startTime;time.before(endTime);) {
				String url=dateBuilder(formatter.format(time));
				BqPdfTask task=new BqPdfTask(ROOT+url);
				task.setTimeStamp(formatter.format(time));
				tasks.put(url,task);
				time.setTime(time.getTime()+ONEDAY);
			}
		}
		return tasks;
	}


	
	
	public static void main(String[] args) {
		Map<String,BqPdfTask> tasks;	
		ExecutorService pool=Executors.newFixedThreadPool(20);
		try {
			
			if(args.length==0) {
				tasks=getTodayTask();
			}else if(args.length==1){
				tasks=taskBuilder(args[0]);
			}else {
				tasks=taskBuilder(args[0],args[1]);
			}
			for(String key:tasks.keySet()) {
				BqPdfTask task=tasks.get(key);
				task.setTargetDestintaion("");
				pool.submit(task);
			}
			while(!isAllDown(tasks.values())) {
				pool.shutdown();
			}
		
		}catch(Exception e) {
			System.out.println(e);
		}
		
	}
	
	
	public static boolean isAllDown(Collection<BqPdfTask> tasks) {
		for(BqPdfTask task:tasks) {
			if(!task.isDone())return false;
		}
		return true;
	}
	
	
    public static void main2(String[] args) throws IOException {
        String root = "http://epaper.ynet.com/";
        String url = "";
        List<String> papers = new ArrayList<String>();
        List<Path> files = new ArrayList<Path>();
        Document doc = Jsoup.connect(root).get();
        Element head = doc.head();
        Elements meta = head.getElementsByTag("META");
        Iterator<Element> iterator = meta.iterator();
        while (iterator.hasNext()) {
            Element e = iterator.next();
            if (e.hasAttr("HTTP-EQUIV")) {
                if (e.attr("HTTP-EQUIV").equals("REFRESH")) {
                    String content = e.attr("CONTENT");
                    url = content.substring(content.indexOf("URL=") + 4);
                }
            }
        }
        if (args.length == 1 && args[0].matches("[\\d]{8}")) {
            url = "html/" + args[0].substring(0, 4) + "-" + args[0].substring(4, 6) + "/" + args[0].substring(6) + "/node_1331.htm";
        }
        //url="html/2014-11/11/node_1331.htm";
        String times[] = url.split("/");
        String rooturl = root + url;
        doc = Jsoup.connect(rooturl).get();
        Element list = doc.body().getElementById("artcile_list_wapper");
        Elements items = list.getElementsByClass("default");
        items.stream().forEach((a) -> {
            papers.add(a.child(1).attr("href").replaceAll("\\.\\./", ""));
        });
        FileDownloader worker=new FileDownloader();
        papers.stream().forEach((String httpurl) -> {
            try {
                URL r = new URL(root + httpurl);
                String filename = httpurl.substring(httpurl.lastIndexOf("/") + 1);
                if (filename.matches("bj[\\w\\w]{2}b.*")) {
                    filename = filename.replaceFirst("bj[\\w\\w]{2}b", "bjqnb");

                }
                filename="J:\\Pic_data\\"+filename;
                files.add(Paths.get(filename));
                boolean isdownloaded = false;
                for (int i = 0; i < 10 && !isdownloaded; i++) {
                    isdownloaded = worker.downloadFile(r, filename);
                }
                if (!isdownloaded) {
                    throw new Exception();
                } else {
                    System.out.println("Finished " + httpurl);
                }

            } catch (Exception ex) {
                System.out.println(ex);
                System.out.println("fail to download " + httpurl);
            }

        });
   
        PdfMergeWorker mergeWorker=new PdfMergeWorker(true);
        mergeWorker.mergefiles(files,Paths.get("J:\\Pic_data\\"+ "bjqnb" + times[1].replace("-", "") + times[2] + ".pdf"));
    }

   
}
