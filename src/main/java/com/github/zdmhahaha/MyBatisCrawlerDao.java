package com.github.zdmhahaha;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MyBatisCrawlerDao implements CrawlerDao {
    private SqlSessionFactory sqlSessionFactory;

    public MyBatisCrawlerDao() {
        String resource = "db/mybatis/config.xml";
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }


    @Override
    public String getNextLinkThenDeleteIt() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String link = session.selectOne("com.github.zdmhahaha.BlogMapper.selectNextLink");
            if (link != null) {
                session.delete("com.github.zdmhahaha.BlogMapper.deleteLink", link);
            }
            return link;
        }
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            int count = session.selectOne("com.github.zdmhahaha.BlogMapper.countLink", link);
            return count != 0;
        }
    }

    @Override
    public void storeNewsPageInToDatabase(String url, String title, String content) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.zdmhahaha.BlogMapper.insertnews", new News(url, title, content));
        }
    }

    @Override
    public void insertLinkToBeProcessed(String href) throws SQLException {
        chooseTableNameAndInsert(href,"LINKS_TO_BE_PROCESSED");
    }

    @Override
    public void insertLinkAlreadyProcessed(String link) throws SQLException {
        chooseTableNameAndInsert(link, "LINKS_ALREADY_PROCESSED");
    }

    private void chooseTableNameAndInsert(String link, String tableName) {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", tableName);
        param.put("link", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.zdmhahaha.BlogMapper.insertlink", param);
        }
    }

}
