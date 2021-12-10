package com.github.zdmhahaha;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLink(String sql) throws SQLException;

    String getNextLinkThenDeleteIt() throws SQLException;

    void updateDatabase(String link, String sql) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void storeNewsPageInToDatabase(String url, String title, String content) throws SQLException;
}