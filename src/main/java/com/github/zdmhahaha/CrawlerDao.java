package com.github.zdmhahaha;

import java.sql.SQLException;

public interface CrawlerDao {

    String getNextLinkThenDeleteIt() throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void storeNewsPageInToDatabase(String url, String title, String content) throws SQLException;

    void insertLinkToBeProcessed(String href) throws SQLException;

    void insertLinkAlreadyProcessed(String link) throws SQLException;
}