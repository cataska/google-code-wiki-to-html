package cn.org.rapid_framework.tools.google_wiki_to_html;

import java.io.IOException;

import junit.framework.TestCase;

public class GoogleWikiTest extends TestCase {
	public void test() throws IOException {
		GoogleWiki g = new GoogleWiki("rapid-framework",null,"gif|js|css");
		g.execute();
	}
}
