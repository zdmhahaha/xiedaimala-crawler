package com.github.zdmhahaha;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {

        //待处理的链接池
        List<String> linkPool = new ArrayList<>();
        linkPool.add("https://sina.cn/");
        //已经处理过的链接池
        Set<String> processedLinks = new HashSet<>();

        while (true) {
            //如果池子没有链接，那么结束循环
            if (linkPool.isEmpty()) {
                break;
            }
            //每拿到一个链接就从池子中删除掉
            String link = linkPool.remove(linkPool.size()-1);

            //是否已经处理过
            if (processedLinks.contains(link)) {
                continue;
            }
            //第一轮筛选： 是否包含sina.cn
            if (isInterestingLink(link)) {
                //爬它
                Document doc = getHttpGetAndParseHtml(link);
                //过滤所有的a标签 每一个都加入待处理的链接池中
                doc.select("a").stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);
                //过滤出新闻相关的内容，存入数据库
                storeIntoDatabaseIfItIsNewsPage(doc);
                //处理完的链接加入已处理的链接池
                processedLinks.add(link);

            }
        }


    }

    private static void storeIntoDatabaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articTag : articleTags) {
                String title = articTag.child(0).text();
                System.out.println(title);
            }
        }
    }

    private static Document getHttpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        //System.out.println(link);
        if(link.startsWith("//")) {
            link = "https:" + link;
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36");
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
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
        return !link.contains("passport..sina.cn");
    }

    private static boolean isIndex(String link) {
        return "https://sina.cn/".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }
}
