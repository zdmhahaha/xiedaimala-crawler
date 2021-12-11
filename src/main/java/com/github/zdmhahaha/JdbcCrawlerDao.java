package com.github.zdmhahaha;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.*;

public class JdbcCrawlerDao implements CrawlerDao {
    private static final String Username = "root";
    private static final String Password = "root";
    private final Connection connection;

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public JdbcCrawlerDao() {
        try {
            this.connection = DriverManager.getConnection("jdbc:h2:file:C:/Users/DEMAO/xiedaimala-crawler/news", Username, Password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getNextLink(String sql) throws SQLException {
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

    public String getNextLinkThenDeleteIt() throws SQLException {
        //先从数据库中待处理的表中拿出(拿出并删除)一条链接，然后处理它
        String link = getNextLink("select link from LINKS_TO_BE_PROCESSED limit 1");
        if (link != null) {
            //每拿到一个链接就从数据库-待处理的表中删除掉
            updateDatabase(link, "DELETE FROM LINKS_TO_BE_PROCESSED where link = ?");
        }
        return link;
    }

    private void updateDatabase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, link);
            statement.executeUpdate();
        }
    }

    public boolean isLinkProcessed(String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("select link FROM LINKS_ALREADY_PROCESSED where link = ?")) {
            statement.setNString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

    public void storeNewsPageInToDatabase(String url, String title, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into news (url,title,content,created_at,modified_at) values (?,?,?,now(),now())")) {
            statement.setString(1, url);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.executeUpdate();
        }
    }

    @Override
    public void insertLinkToBeProcessed(String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("Insert into LINKS_TO_BE_PROCESSED values (?)")) {
            statement.setNString(1, link);
            statement.executeUpdate();
        }
    }

    @Override
    public void insertLinkAlreadyProcessed(String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("Insert into LINKS_ALREADY_PROCESSED values (?)")) {
            statement.setNString(1, link);
            statement.executeUpdate();
        }
    }
}
