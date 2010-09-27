package cn.org.rapid_framework.tools.google_wiki_to_html;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.org.rapid_framework.tools.google_wiki_to_html.util.FileUtils;
import cn.org.rapid_framework.tools.google_wiki_to_html.util.FilenameUtils;
import cn.org.rapid_framework.tools.google_wiki_to_html.util.IOUtils;
import cn.org.rapid_framework.tools.google_wiki_to_html.util.MD5Utils;
import cn.org.rapid_framework.tools.google_wiki_to_html.util.URLDownloader;

/**
 * @author badqiu
 */
public class GoogleWiki {
	public static final String IMG_EXTENSIONS = "jpg|jpeg|gif|png|bmp";
	public String downloadResourceExtensions = "js|css";
	
	String projectHomeURL = "http://code.google.com/p/%s/";
	String wikiDownloadBaseURL = "http://code.google.com/p/%s/wiki/";
	String wikiListURL = "http://%s.googlecode.com/svn/wiki/";
	public String outputFolder = "d:/google-wiki-html/";
	String wikiLinkPrefix = "/p/%s/wiki/";
	String projectName = null;
	
	public GoogleWiki(String _projectName,String _outputFolder,String _downloadResourceExtensions) throws IOException {
		this.projectName = _projectName;
		projectHomeURL = String.format(projectHomeURL, projectName);
		wikiDownloadBaseURL = String.format(wikiDownloadBaseURL, projectName);
		wikiListURL = String.format(wikiListURL, projectName);
		wikiLinkPrefix = String.format(wikiLinkPrefix, projectName);
		this.outputFolder = _outputFolder == null ? new File(this.outputFolder,projectName).getAbsolutePath() : _outputFolder;
		if(_downloadResourceExtensions == null) {
			downloadResourceExtensions = downloadResourceExtensions+"|"+IMG_EXTENSIONS;
		}else {
			downloadResourceExtensions = _downloadResourceExtensions;
		}
		new File(this.outputFolder).mkdirs();
	}
	
	public void execute() throws IOException {
		
		List<String> wikiList = getWikiList();
		generateIndexHTML(wikiList);
		for(int i = 0; i < wikiList.size(); i++) {
			String wiki = wikiList.get(i);
			try {
				System.out.println(getDownloadInfo(wikiList, i, wiki));
				String content = downloadWiki(wiki);
				content = downloadContentResources(content);
				content = processWikiContent(content);
				saveWikiAsHtml(wiki,content);
				downloadWikiSource(wiki);
			}catch(Exception e) {
				System.err.println("process wiki error:"+wiki+" cause:"+e);
			}
		}

	}

	private void downloadWikiSource(String wiki) throws MalformedURLException, IOException {
		ByteArrayOutputStream content = URLDownloader.downloadAsByteArrayOutputStream(new URL(wikiListURL+"/"+wiki+".wiki"));
		FileOutputStream output = null;
		try {
			File outputFile = new File(outputFolder,"wiki/"+wiki+".wiki");
			outputFile.getParentFile().mkdirs();
			output = new FileOutputStream(outputFile);
			IOUtils.write(content.toByteArray(), output);
		}finally {
			IOUtils.closeQuietly(output);
		}
	}

	private void generateIndexHTML(List<String> wikis)
			throws UnsupportedEncodingException, FileNotFoundException,
			IOException {
		BufferedWriter indexHtml = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFolder,"index.html")),"UTF-8"));
		indexHtml.write(String.format("<html><head><title>wiki of %s</title><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'></head><body>",projectName));
		indexHtml.write(String.format("<h1><a href='%s'>%s<a></h1>",projectHomeURL,projectName));
		for(String wiki : wikis) {
			indexHtml.write(String.format("<li><a href='%s.html'>%s</a></li>",wiki,wiki));
		}
		indexHtml.write("</body><html>");
		indexHtml.close();
	}

	private String getDownloadInfo(List<String> wikis, int i, String wiki)
			throws MalformedURLException {
		double percent = (i+1.0)/wikis.size() * 100;
		String percentInfo = String.format("%.1f", percent) + "% "+(i+1)+"/"+wikis.size();
		String downloadInfo = percentInfo+" download wiki:"+getDownloadWikiURL(wiki)+"  ==> "+getOutputHtmlFile(wiki);
		return downloadInfo;
	}
	
	private static Pattern wikiListPattern = Pattern.compile(">([\\w_]+\\.wiki)</a>",Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);
	private List<String> getWikiList() throws IOException {
		URL downloadURL = new URL(wikiListURL);
		Reader r = new InputStreamReader(downloadURL.openStream(),"UTF-8");
		String str = IOUtils.toString(r);
		Matcher m = wikiListPattern.matcher(str);
		List results = new ArrayList();
		while(m.find()) {
			String group = m.group(1);
			results.add(FilenameUtils.removeExtension(group));
		}
		return results;
	}
	
	private String downloadWiki(String wiki) throws IOException {
		URL downloadURL = getDownloadWikiURL(wiki);
		return URLDownloader.downloadAsString(downloadURL, "UTF-8");
	}

	private URL getDownloadWikiURL(String wiki) throws MalformedURLException {
		return new URL(wikiDownloadBaseURL+wiki);
	}

	private String processWikiContent(String content) throws IOException {
		
		//content = StringUtils.remove(content, removedContent);
		content = appendHtmlExtensionToWikiLink(content);
		//content = insertSomeTagToHeadTag(content);
		
		return content;
	}
	
	/*
	 * append html extension,example: roadmap => roadmap.html
	 */
	private String appendHtmlExtensionToWikiLink(String content) {
		StringBuffer newContent = new StringBuffer();
		Pattern pattern = Pattern.compile("href=['\"](.*?"+wikiLinkPrefix+")([^\\s]*)['\"]",Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);
		Matcher m = pattern.matcher(content);
		while(m.find()) {
			String wikiLinkPrefix = m.group(1);
			String wiki = m.group(2);
			m.appendReplacement(newContent,"href=\""+wiki+".html\"");
		}
		m.appendTail(newContent).toString();
		content = newContent.toString();
		return content;
	}

	//insert <base href='http://code.google.com' />
	private String insertSomeTagToHeadTag(String content) {
		int indexOfHead = content.indexOf("<head>");
		String before = content.substring(0,indexOfHead+6);
		String after = content.substring(indexOfHead+6,content.length());
		content = before+"<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>"+after;
		return content;
	}
	
	private String downloadContentResources(String content) {
		Pattern downloadResourcePattern = Pattern.compile("http://[^\\s]+\\.("+downloadResourceExtensions+")",Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);
		
		Matcher m = downloadResourcePattern.matcher(content);
		StringBuffer newContent = new StringBuffer();
		while(m.find()) {
			String http = m.group();
			try {
				URL url = new URL(http);
				if(!URLDownloader.isDownloaded(url))
					System.out.println("\tdownload resource:"+http);
				ByteArrayOutputStream resourceContent = URLDownloader.downloadAsByteArrayOutputStream(url);
				File resourceDir = new File(outputFolder,"resource");
				resourceDir.mkdirs();
				
				String filename = "url_md5_"+MD5Utils.getMD5(http)+"_"+ new File(url.getFile()).getName();
				FileUtils.writeByteArrayToFile(new File(resourceDir,filename), resourceContent.toByteArray());
				
				m.appendReplacement(newContent,"resource/"+filename);
			}catch(IOException e) {
				System.out.println("[ERROR] download resource:"+http);
			}
		}
		return m.appendTail(newContent).toString();
	}

	private void saveWikiAsHtml(String wiki, String content) throws IOException {
		OutputStreamWriter output = null;
		try {
			File outputHtml = getOutputHtmlFile(wiki);
			output = new OutputStreamWriter(new FileOutputStream(outputHtml),"UTF-8");
			IOUtils.write(content, output);
		}finally {
			IOUtils.closeQuietly(output);
		}
	}

	private File getOutputHtmlFile(String wiki) {
		return new File(outputFolder,wiki+".html");
	}

}
