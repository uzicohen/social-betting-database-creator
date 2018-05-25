package database;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.jboss.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.socialbetting.objectmodel.Competition;
import com.socialbetting.objectmodel.Game;
import com.socialbetting.objectmodel.Team;
import com.socialbetting.objectmodel.Tournament;
import com.socialbetting.objectmodel.User;

import http.HttpClient;

public class WorldCup2018DatabaseCreator {

	private static final Logger logger = Logger.getLogger(WorldCup2018DatabaseCreator.class);

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZ");

	private static String getDateWithoutColon(String input) {
		String prefix = input.substring(0, input.length() - 3);
		String suffix = input.substring(input.length() - 2, input.length());
		return prefix + suffix;
	}

	private static String getDate(String dateStr) {
		Date date = null;
		try {
			date = SDF.parse(getDateWithoutColon(dateStr));
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
		SimpleDateFormat formatterUTC = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		formatterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
		return formatterUTC.format(date);
	}

	public static void main(String[] args) {
		BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

		logger.info("Creating Users");

		User user1 = new User("uzicohen9@gmail.com", "uzinio", bCryptPasswordEncoder.encode("uzi636"), true,
				User.ROLE_ADMIN);

		User user2 = new User("almog.bregman@gmail.com", "almogi", bCryptPasswordEncoder.encode("abcd2106"), true,
				User.ROLE_USER);
		DatabaseHandler.save(user1);
		DatabaseHandler.save(user2);

		logger.info("Creating Competition");

		Competition competition = new Competition("World Cup 2018",
				"https://www.justreadonline.com/wp-content/uploads/2018/01/rusia-cup.jpg",
				"FIFA Soccer world cup in Russia 2018");
		DatabaseHandler.save(competition);

		logger.info("Calling to get Json");

		String dataStr = HttpClient
				.getData("https://raw.githubusercontent.com/lsv/fifa-worldcup-2018/master/data.json");

		logger.info("Creating teams");

		Gson gson = new Gson();
		try {
			JSONObject allDataObj = (JSONObject) new JSONParser().parse(dataStr);

			// Teams
			JSONArray teamArray = (JSONArray) allDataObj.get("teams");
			for (int i = 0; i < teamArray.size(); i++) {
				JsonElement currentTeam = gson.fromJson(teamArray.get(i).toString(), JsonElement.class);
				Team team = gson.fromJson(currentTeam, Team.class);
				DatabaseHandler.save(team);
				logger.info("Created " + (i + 1) + " teams");
			}

			String[] groups = new String[] { "a", "b", "c", "d", "e", "f", "g", "h" };

			int gamesCounter = 0;
			// Games - Groups
			JSONObject groupsObject = (JSONObject) allDataObj.get("groups");
			for (int i = 0; i < groups.length; i++) {
				JSONObject groupObject = (JSONObject) groupsObject.get(groups[i]);
				String groupName = (String) groupObject.get("name");
				JSONArray matchArray = (JSONArray) groupObject.get("matches");
				for (int j = 0; j < matchArray.size(); j++) {
					JSONObject matchObject = (JSONObject) matchArray.get(j);
					String level = groupName + ", game " + (j + 1);
					Team team1 = new Team((long) matchObject.get("home_team"), competition, "", "", "");
					Team team2 = new Team((long) matchObject.get("away_team"), competition, "", "", "");
					String dateStr = (String) matchObject.get("date");
					Game game = new Game(competition, team1, team2, level, Game.STATE_NOT_STARTED, getDate(dateStr));
					DatabaseHandler.save(game);
					logger.info("Created " + ++gamesCounter + " games");
					if (gamesCounter == 5) {
						break;
					}
				}
			}

			// Create tournament
			logger.info("Creating Cortana tournament");
			Tournament tournament = new Tournament(user1, "Cortana Russia 2018", new Date(), competition);
			DatabaseHandler.save(tournament);

		} catch (ParseException e) {
			e.printStackTrace();
		}
		DatabaseHandler.destroySessionFactory();

	}
}
