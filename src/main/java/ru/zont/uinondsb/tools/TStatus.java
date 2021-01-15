package ru.zont.uinondsb.tools;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.uinondsb.Globals;

import java.awt.*;
import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static ru.zont.dsbot.core.tools.Strings.STR;
import static ru.zont.uinondsb.tools.Commons.trimNick;

public class TStatus {
    public static final int ONLINE_TIMEOUT = 45000;
    private static final long INTERVAL = 19000;
    private static long nextRetrieve = 0;
    private static ServerInfoStruct cached = null;

    private static final SavedData<Integer> record = new SavedData<>(Integer.class, "record");

    public static synchronized ServerInfoStruct retrieveInfo() {
        if (System.currentTimeMillis() < nextRetrieve && cached != null) return cached;

        ServerInfoStruct res;
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
            Statement st = connection.createStatement()) {
            final ResultSet rs1 = st.executeQuery("SELECT sv_players, sv_time, sv_lastupd " +
                    "FROM serverinf WHERE sv_port = 2302 LIMIT 1");
            if (!rs1.next()) throw new RuntimeException("No result from database");

            final ArrayList<String> players = new Gson().fromJson(rs1.getString("sv_players"),
                    new TypeToken<ArrayList<String>>() {}.getType());
            int count = players.size();
            long time = rs1.getTimestamp("sv_lastupd").getTime();
            long uptime = (long) (rs1.getFloat("sv_time") * 1000F);
            final Integer storedRecord = record.restore((Integer) 86);
            int rec = Math.max(count, storedRecord);

            final ResultSet rs2 = st.executeQuery("SELECT COUNT(*) FROM profiles");
            if (!rs2.next()) throw new RuntimeException("No result from database");
            int total = rs2.getInt(1);

            ResultSet rs3 = st.executeQuery("SELECT CURRENT_TIMESTAMP()");
            if (!rs3.next()) throw new RuntimeException("No result from database");
            long currentTime = rs3.getTimestamp(1).getTime();

            if (rec > storedRecord)
                record.save(rec);
            final ArrayList<TGameMasters.GM> gms = TGameMasters.retrieve();
            gms.removeIf(gm -> (System.currentTimeMillis() - gm.lastlogin) > 120000);
            res = new ServerInfoStruct(count, currentTime - time, uptime, gms, rec, total);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        nextRetrieve = System.currentTimeMillis() + INTERVAL;
        cached = res;
        return res;
    }

    public static class ServerInfoStruct {
        public int count;
        public long time;
        public long uptime;
        public List<TGameMasters.GM> gms;
        public int record;
        public int total;

        public ServerInfoStruct(int count, long time, long uptime, List<TGameMasters.GM> gms, int record, int total) {
            this.count = count;
            this.time = time;
            this.uptime = uptime;
            this.gms = gms;
            this.record = record;
            this.total = total;
        }
    }


    public static class Msg {
        public static MessageEmbed status(ServerInfoStruct serverInfo, String gmRoleID) {
            long time = serverInfo.time;
            int online = serverInfo.count;
            int playersRecord = serverInfo.record;
            long restart = serverInfo.uptime;
            List<TGameMasters.GM> gms = serverInfo.gms;
            if (time >= ONLINE_TIMEOUT) return serverInactive();

            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(STR.getString("status.main.title"))
                    .addField(STR.getString("status.main.online"), Commons.countPlayers(online), true)
                    .addField(
                            STR.getString("status.main.restart"),
                            String.format( STR.getString("time.hm"),
                                    restart / 1000 / 60 / 60,
                                    restart / 1000 / 60 % 60 ),
                            true);

            StringBuilder sb = new StringBuilder();
            StringBuilder sb1 = new StringBuilder();
            for (TGameMasters.GM gm: gms) {
                String app = (sb1.length() > 0 ? ", " : "") + trimNick(gm.armaname);
                if (sb1.length() == 0 || sb1.length() + app.length() <= 26)
                    sb1.append(app);
                else {
                    sb.append(String.format(":zap: %26s\n", sb1.toString()));
                    sb1 = new StringBuilder();
                }
            }
            if (sb1.length() > 0)
                sb.append(String.format(":zap: %26s\n", sb1.toString()));
            if (sb.length() == 0)
                sb.append(noGmString(gmRoleID));
            builder.addField(STR.getString("status.main.gms"), sb.toString(), false);

            if (playersRecord >= 0) {
                int g = 255 * online / playersRecord;
                if (g > 255) g = 255;
                int r = 255 - g;
                builder.setColor(new Color(r, g, 0));
            }

            return builder.build();
        }

        private static String noGmString(String gm) {
            int h = LocalTime.now().getHour();
            if (h >= 15 && h <= 22) {
                return String.format(STR.getString("status.main.gms.absent.day"), gm);
            } else {
                return STR.getString("status.main.gms.absent.night");
            }
        }

        public static MessageEmbed serverInactive() {
            return new EmbedBuilder()
                    .setTitle(STR.getString("status.main.title"))
                    .setDescription(STR.getString("status.main.inactive"))
                    .build();
        }

        public static MessageEmbed serverStatisticInitial() {
            return new EmbedBuilder()
                    .setTitle(STR.getString("status.statistics.title"))
                    .setDescription(STR.getString("status.statistics.connect"))
                    .build();
        }

        public static MessageEmbed serverStatistic(int playersRecord, int playersTotal) {
            if (playersRecord <= 0) return serverStatisticInitial();
            if (playersTotal <= 0) return serverStatisticInitial();
            return new EmbedBuilder()
                    .setColor(Color.lightGray)
                    .setTitle(STR.getString("status.statistics.title"))
                    .addField(STR.getString("status.statistics.record"), Commons.countPlayers(playersRecord), false)
                    .addField(STR.getString("status.statistics.total"), Commons.countPlayers(playersTotal), false)
                    .build();
        }
    }
}
