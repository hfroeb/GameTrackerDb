/**
 * Created by halleyfroeb on 9/27/16.
 */
import java.util.ArrayList;

public class User {
    String name;
    ArrayList<Game> games = new ArrayList<>();

    public User(String name) {
        this.name = name;
    }
}
