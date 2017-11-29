package bqdownloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BadPdfFormatException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;

public class PdfMergeWorker {

	private boolean deleteSource = false;

	public boolean isDeleteSource() {
		return deleteSource;
	}

	public void setDeleteSource(boolean deleteSource) {
		this.deleteSource = deleteSource;
	}

	public PdfMergeWorker() {
		this(false);
	}

	public PdfMergeWorker(boolean deleteSource) {
		this.deleteSource = deleteSource;
	}



	private Rectangle getPageSize(List<Path> files) {
		for (int i = 0; i < files.size(); i++) {
			try {
				PdfReader reader=new PdfReader(files.get(i).toString());
				Rectangle size= reader.getPageSize(1);
				reader.close();
				return size;
			} catch (IOException ex) {

			}
		}
		return null;
		
		
	}

	public boolean mergefiles(List<Path> files, Path savepath) throws IOException {
		Rectangle pageSize = getPageSize(files);
		
		if (pageSize == null) {
			System.out.println("Fail to get PageSize of input files,won't merge pdf");
			return false;
		} else {
			boolean deleteMergeTemp = false;
			try  {
				Document document = new Document(pageSize);
				PdfCopy copy = new PdfCopy(document, new FileOutputStream(savepath.toFile()));
				document.open();
				for (Path file : files) {
					try {
						PdfReader reader = new PdfReader(file.toString());
						int n = reader.getNumberOfPages();
						for (int j = 1; j <= n; j++) {
							document.newPage();
							PdfImportedPage page = copy.getImportedPage(reader, j);
							try {
								copy.addPage(page);
							} catch (BadPdfFormatException ex) {
								System.out.println(ex);
								System.out.println(file);
							}
						}
						reader.close();
					} catch (IOException e) {
						System.out.println(e);
						System.out.println(file);
						deleteMergeTemp = true;
					}
				}
				document.close();
			} catch (DocumentException ex) {
				System.out.println(ex);
				System.out.println(savepath);
				deleteMergeTemp = true;
			}
			if (deleteMergeTemp) {
				Files.deleteIfExists(savepath);
			}
			if (deleteSource) {
				for (Path file : files)
					Files.deleteIfExists(file);
			}
			return true;
		}

	}
}
