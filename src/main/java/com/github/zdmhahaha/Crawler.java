package com.github.zdmhahaha;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Crawler {

    CrawlerDao dao = new JdbcCrawlerDao();

    public static void main(String[] args) throws SQLException, IOException {
        new Crawler().run();
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public void run() throws IOException, SQLException {

        String link;
        //从数据库加载下一个即将处理的链接，能加载到则进入循环
        while ((link = dao.getNextLinkThenDeleteIt()) != null) {

            dao.updateDatabase(link, "DELETE FROM LINKS_TO_BE_PROCESSED where link = ?");

            //是否已经处理过
            if (dao.isLinkProcessed(link)) {
                continue;
            }
            //第一轮筛选： 是否包含sina.cn
            if (isInterestingLink(link)) {
                //爬它
                Document doc = getHttpGetAndParseHtml(link);
                //过滤所有的a标签 每一个都加入数据库中待处理的链接表中
                parseUrlsFromPagesAndInToDatabase(doc);
                //过滤出新闻相关的内容，存入数据库
                storeIntoDatabaseIfItIsNewsPage(doc, link);

                //处理完的链接加入已处理的链接池
                dao.updateDatabase(link, "Insert into LINKS_ALREADY_PROCESSED values (?)");
                //processedLinks.add(link);
            }

        }

    }

    private void parseUrlsFromPagesAndInToDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");

            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (!href.toLowerCase().startsWith("javascript")) {
                dao.updateDatabase(href, "Insert into LINKS_TO_BE_PROCESSED values (?)");
            }
        }
    }


    private void storeIntoDatabaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articTag : articleTags) {
                //得到标题
                String title = articTag.child(0).text();
                //得到正文
                String content = articTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                dao.storeNewsPageInToDatabase(link, title, content);


                System.out.println(title);
            }
        }
    }

    private static Document getHttpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        //System.out.println(link);
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36");
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            //System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
            //System.out.println(html);
            return Jsoup.parse(html);
        }
    }

    private static boolean isInterestingLink(String link) {
        return isNewsPage(link) || isIndex(link) && isNotLoginPage(link);
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }

    private static boolean isIndex(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }
}
