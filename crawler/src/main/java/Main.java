import com.google.common.collect.Lists;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Vitaly on 15.02.2018.
 */
public class Main {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/15.0.874.120 Safari/535.2";

    public static Stack<String> urls = new Stack<String>();

    public static String domain = "kolesa.ru";

    public static String main = "http://www." + domain;

    static final String DB_URL = "jdbc:postgresql://127.0.0.1:5432/test";
    static final String USER = "postgres";
    static final String PASS = "";
    static java.sql.Connection connection = null;

    public static void main(String[] args) {
        System.out.println("Testing connection to PostgreSQL JDBC");

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC Driver is not found. Include it in your library path ");
            e.printStackTrace();
            return;
        }

        System.out.println("PostgreSQL JDBC Driver successfully connected");
        try {
            connection = DriverManager
                    .getConnection(DB_URL, USER, PASS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        step(main);
        System.out.println(urls.size());
        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void step(String url) {
        if (!url.contains(main) || urls.contains(url) || url.endsWith(".jpg") || url.endsWith(".jpeg")) {
            return;
        }
        System.out.println(url);
        urls.add(url);
        Document document = null;
        try {
            Connection connect = Jsoup
                    .connect(url)
                    .followRedirects(true)
                    .userAgent(USER_AGENT)
                    .timeout(5 * 1000);
            Connection.Response response = connect.execute();
            document = response.parse();
            saveDocumentText(document, url);
            Elements links = document.getElementsByTag("a");
            for (Element element : links) {
                if (urls.size() == 100) {
                    break;
                }
                step(element.attr("href"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void print(List<String> words) {
        for (String word : words) {
            System.out.println(word);
        }
    }

    private static void saveDocumentText(Document document, String url) {
        StringBuilder stringBuilder = new StringBuilder();
        Elements elements = document.getAllElements();
        for (Element element : elements) {
            if (!element.ownText().trim().isEmpty()) {
                stringBuilder.append(element.ownText()).append(" ");
            }
        }

        try {
            PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO documents(id, data, url) values(?, ?, ?)");
            insertStatement.setLong(1, urls.size());
            insertStatement.setString(2, stringBuilder.toString());
            insertStatement.setString(3, url);
            insertStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("fail");
            e.printStackTrace();
        }
    }
}
