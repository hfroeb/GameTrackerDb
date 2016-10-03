/**
 * Created by halleyfroeb on 9/27/16.
 */

import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS games1 (id IDENTITY, name VARCHAR, genre VARCHAR, platform VARCHAR, releaseYear INT, author VARCHAR)");
        stmt.execute("CREATE TABLE IF NOT EXISTS users (userId IDENTITY, userName VARCHAR)");
    }

    public static void main(String[] args) throws SQLException {

        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);

        Spark.init();
        Spark.get(
                "/",
                (request, response) -> {
                    HashMap m = new HashMap<>();
                    Session session = request.session();
                    String userName = session.attribute("userName");
                    String author = userName;
                    if (userName == null) {
                        return new ModelAndView(m, "login.html");
                    }
                    HashMap p = new HashMap<>();
                    User user = selectUser(conn, userName);
                    if (user == null) {
                        insertUser(conn, userName);
                        user = selectUser(conn, userName);
                    }
                    ArrayList<Game> games = selectGames(conn, author);
                    p.put("user", user);
                    m.put("userName", userName);
                    p.put("games", games);
                    return new ModelAndView(p, "home.html");
                },
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/create-user",
                (request, response) -> {
                    String userName = request.queryParams("loginName");
                    Session session = request.session();
                    session.attribute("userName", userName);
                    response.redirect("/");
                    return "";
                }
        );
        Spark.post(
                "/create-game",
                (request, response) -> {
                    String gameName = request.queryParams("gameName");
                    String gameGenre = request.queryParams("gameGenre");
                    String gamePlatform = request.queryParams("gamePlatform");
                    int gameYear = Integer.valueOf(request.queryParams("gameYear"));
                    Session session = request.session();
                    String author = session.attribute("userName");
                    insertGame(conn, gameName, gameGenre, gamePlatform, gameYear, author);
                    response.redirect("/");
                    return "";
                }
        );
        Spark.post(
                "/logout",
                (request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                }
        );
        Spark.post(
                "/delete-game",
                (request, response) -> {
                    int id = Integer.parseInt(request.queryParams("deleteIdNum"));
                    deleteGame(conn, id);
                    response.redirect("/");
                    return "";
                });

        Spark.get(
                "/editGame",
                (request, response) -> {
                    HashMap m = new HashMap();
                    Session session = request.session();
                    int id = session.attribute("editGameId");
//                    int id = Integer.parseInt(request.queryParams("editGameId"));
                    Game game = selectGame(conn, id);
                    m.put("game", game);
                    m.put("id", id);
                    return new ModelAndView(m, "editGame.html");
                },
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/editGame", // page to edit game
                (request, response) -> {
                    Session session = request.session();
                    int editId = session.attribute("editGameId");
                    String editName = request.queryParams("editGameName");
                    String editGenre = request.queryParams("editGameGenre");
                    String editPlatform = request.queryParams("editGamePlatform");
                    int editReleaseYear = Integer.parseInt(request.queryParams("editGameYear"));
                    String author = session.attribute("userName");
                    updateGame(conn, editId, editName, editGenre, editPlatform, editReleaseYear, author);
                    response.redirect("/");
                    return "";
                }
        );
        Spark.post( //button to edit page
                "/edit-game",
                (request, response) -> {
                    int id = Integer.parseInt(request.queryParams("editGameId"));
                    Session session = request.session();
                    session.attribute("editGameId", id);
                    response.redirect("/editGame");
                    return "";
                });

    }

    // User Functions

    public static void insertUser(Connection conn, String userName) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?)");
        stmt.setString(1, userName);
        stmt.execute();
    }

    public static User selectUser(Connection conn, String userName) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE userName = ?");
        stmt.setString(1, userName);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            int id = results.getInt("userId");
            return new User(id, userName);
        }
        return null;
    }

    public static ArrayList<User> selectUsers(Connection conn) throws SQLException {
        ArrayList<User> users = new ArrayList<>();
        Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery("SELECT * FROM users");
        while (results.next()) {
            int userId = results.getInt("userId");
            String userName = results.getString("userName");
            users.add(new User(userId, userName));
        }
        return users;
    }

    // Game Functions

    public static void insertGame(Connection conn, String name, String genre, String platform, int releaseYear, String author) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO games1 VALUES (NULL, ?, ?, ?, ?, ?)");
        stmt.setString(1, name);
        stmt.setString(2, genre);
        stmt.setString(3, platform);
        stmt.setInt(4, releaseYear);
        stmt.setString(5, author);
        stmt.execute();
    }

    public static ArrayList<Game> selectGames(Connection conn, String author) throws SQLException {
        ArrayList<Game> games = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM games1 INNER JOIN users ON userName = author WHERE author = ?");
        stmt.setString(1, author);
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
            int id = results.getInt("id");
            String name = results.getString("name");
            String genre = results.getString("genre");
            String platform = results.getString("platform");
            int releaseYear = results.getInt("releaseYear");
            Game game = new Game(id, name, genre, platform, releaseYear, author);
            if (!games.contains(game)){
                games.add(game);
            }
        }
        return games;
    }

    public static void deleteGame(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE games1 WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }

    public static void updateGame(Connection conn, int editId, String editName, String editGenre, String editPlatform, int editReleaseYear, String editAuthor) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement
                ("UPDATE games1 SET name = ?, genre = ?, platform = ?, releaseYear = ?, author = ? WHERE id = ?");
        stmt.setString(1, editName);
        stmt.setString(2, editGenre);
        stmt.setString(3, editPlatform);
        stmt.setInt(4, editReleaseYear);
        stmt.setString(5, editAuthor);
        stmt.setInt(6, editId);
        stmt.execute();
    }

    public static Game selectGame(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement
                ("SELECT * FROM games1 WHERE id = ?");
        stmt.setInt(1, id);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            String name = results.getString("name");
            String genre = results.getString("genre");
            String platform = results.getString("platform");
            int releaseYear = results.getInt("releaseYear");
            String author = results.getString("author");
            return new Game(id, name, genre, platform, releaseYear, author);
        }
        return null;
    }
}