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

public class Main {

    private static final String Username = "root";
    private static final String Password = "root";

    private static String getNextLink(Connection connection, String sql) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return null;
    }

    private static String getNextLinkThenDeleteIt(Connection connection) throws SQLException {
        //先从数据库中待处理的表中拿出(拿出并删除)一条链接，然后处理它
        String link = getNextLink(connection, "select link from LINKS_TO_BE_PROCESSED limit 1");
        if (link != null) {
            //每拿到一个链接就从数据库-待处理的表中删除掉
            updateDatabase(connection, link, "DELETE FROM LINKS_TO_BE_PROCESSED where link = ?");
        }
        return link;
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:C:/Users/DEMAO/xiedaimala-crawler/news", Username, Password);

        String link;
        //从数据库加载下一个即将处理的链接，能加载到则进入循环
        while ((link = getNextLinkThenDeleteIt(connection)) != null) {

            updateDatabase(connection, link, "DELETE FROM LINKS_TO_BE_PROCESSED where link = ?");

            //是否已经处理过
            if (isLinkProcessed(connection, link)) {
                continue;
            }
            //第一轮筛选： 是否包含sina.cn
            if (isInterestingLink(link)) {
                //爬它
                Document doc = getHttpGetAndParseHtml(link);
                //过滤所有的a标签 每一个都加入数据库中待处理的链接表中
                parseUrlsFromPagesAndInToDatabase(connection, doc);
                //过滤出新闻相关的内容，存入数据库
                storeIntoDatabaseIfItIsNewsPage(connection, doc, link);

                //处理完的链接加入已处理的链接池
                updateDatabase(connection, link, "Insert into LINKS_ALREADY_PROCESSED values (?)");
                //processedLinks.add(link);
            }

        }

    }

    private static void parseUrlsFromPagesAndInToDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");

            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (!href.toLowerCase().startsWith("javascript")) {
                updateDatabase(connection, href, "Insert into LINKS_TO_BE_PROCESSED values (?)");
            }
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select link FROM LINKS_ALREADY_PROCESSED where link = ?")) {
            statement.setNString(1, link);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        }
        return false;
    }

    private static void updateDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, link);
            statement.executeUpdate();
        }
    }


    private static void storeIntoDatabaseIfItIsNewsPage(Connection connection, Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articTag : articleTags) {
                //得到标题
                String title = articTag.child(0).text();
                //得到正文
                String content = articTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                try (PreparedStatement statement = connection.prepareStatement("insert into news (url,title,content,created_at,modified_at) values (?,?,?,now(),now())")) {
                    statement.setString(1, link);
                    statement.setString(2, title);
                    statement.setString(3, content);
                    statement.executeUpdate();
                }
                ;


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
