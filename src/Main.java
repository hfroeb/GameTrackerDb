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

    static HashMap<String, User> users = new HashMap<>();

    public static void main(String[] args) throws SQLException {

        // Game(int id, String name, String genre, String platform, int releaseYear)

        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS games (id IDENTITY, name VARCHAR, genre VARCHAR, platform VARCHAR, releaseYear INT)");

        Spark.init();
        Spark.get(
                "/",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());

                    HashMap m = new HashMap<>();
                    if (user == null) {
                        return new ModelAndView(m, "login.html");
                    }
                    else {
                       // selectGames(conn);
                        user.games = selectGames(conn);
                        return new ModelAndView(user, "home.html");
                    }
                }),
                new MustacheTemplateEngine()
        );

        Spark.post(
                "/create-user",
                ((request, response) -> {
                    String name = request.queryParams("loginName");
                    User user = users.get(name);
                    if (user == null) {
                        user = new User(name);
                        users.put(name, user);
                    }

                    Session session = request.session();
                    session.attribute("userName", name);

                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/create-game",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
                    if (user == null) {
                        //throw new Exception("User is not logged in");
                        Spark.halt(403);
                    }
                    String gameName = request.queryParams("gameName");
                    String gameGenre = request.queryParams("gameGenre");
                    String gamePlatform = request.queryParams("gamePlatform");
                    int gameYear = Integer.valueOf(request.queryParams("gameYear"));

                   // Game game = new Game(gameName, gameGenre, gamePlatform, gameYear);
                 //   user.games.add(game);

                    insertGame(conn, gameName, gameGenre, gamePlatform, gameYear);

                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/logout",
                ((request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/delete-game",
                ((request, response) -> {
                    Session session = request.session();
                    String name = session.attribute("userName");
                    int id = Integer.parseInt(request.queryParams("deleteIdNum"));
                    User user = users.get(name);
                    user.games.remove(id);// DELETE global array? need this?
                    deleteGame(conn, id);
                    response.redirect("/");
                    return "";
                }));

        Spark.get(
                "/editGame",
                ((request, response) -> {
                    HashMap m = new HashMap();
                    Session session = request.session();
                    String name = session.attribute("userName");
                    int id = session.attribute("editGameId");
                    User user = users.get(name);
                    Game game = user.games.get(id);
                    m.put("game", game);
                    m.put("id", id);
                    return new ModelAndView(m, "editGame.html");
                }),
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/editGame", // page to edit game
                ((request, response) -> {
                    Session session = request.session();
                    String name = session.attribute("userName");
                    int editId = session.attribute("editGameId");
                    User user = users.get(name);
                    Game game = user.games.get(editId);

                    String editName = request.queryParams("editGameName");
                    String editGenre = request.queryParams("editGameGenre");
                    String editPlatform = request.queryParams("editGamePlatform");
                    int editReleaseYear = Integer.parseInt(request.queryParams("editGameYear"));

                    updateGame(conn, editId, editName, editGenre, editPlatform, editReleaseYear);

                    response.redirect("/");
                    return "";
                })
        );
        Spark.post( //button to edit page
                "/edit-game",
                ((request, response) -> {
                    Session session = request.session();
                    String name = session.attribute("userName");
                    User user = users.get(name);
                    int editId = Integer.parseInt(request.queryParams("editGameId"));
                    Game game = user.games.get(editId);
                    session.attribute("editGameId", editId);
                    response.redirect("/editGame");
                    return "";
                }));

    }
    public static void insertGame(Connection conn, String name, String genre, String platform, int releaseYear ) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO games VALUES (NULL, ?, ?, ?, ?)");
        stmt.setString(1, name);
        stmt.setString(2, genre);
        stmt.setString(3, platform);
        stmt.setInt(4, releaseYear);
        stmt.execute();
    }

    public static ArrayList<Game> selectGames(Connection conn) throws SQLException{
        ArrayList<Game> games = new ArrayList<>(); //remove (GLOBAL)?
        Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery("SELECT * FROM games");
        while(results.next()){
            int id = results.getInt("id");
            String name = results.getString("name");
            String genre = results.getString("genre");
            String platform = results.getString("platform");
            int releaseYear = results.getInt("releaseYear");
            games.add(new Game(id, name, genre, platform, releaseYear));
        }
        return games;
    }

    public static void deleteGame(Connection conn, int id)throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("DELETE games WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }
    public static void updateGame(Connection conn, int editId, String editName, String editGenre, String editPlatform, int editReleaseYear)throws SQLException{
        PreparedStatement stmt = conn.prepareStatement
                ("UPDATE games SET name = ?, genre = ?, platform = ?, releaseYear = ? WHERE id = ?");
        stmt.setString(1, editName);
        stmt.setString(2, editGenre);
        stmt.setString(3, editPlatform);
        stmt.setInt(4, editReleaseYear);
        stmt.setInt(5, editId);
        stmt.execute();
    }


    static User getUserFromSession(Session session) {
        String name = session.attribute("userName");
        return users.get(name);
    }
}