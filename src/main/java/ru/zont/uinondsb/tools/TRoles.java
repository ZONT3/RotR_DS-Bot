package ru.zont.uinondsb.tools;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.dsbot.core.commands.DescribedException;
import ru.zont.uinondsb.Globals;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.zont.dsbot.core.tools.Strings.*;

public class TRoles {
    public static final int INFISTAR_1 = -1;
    public static final int INFISTAR_2 = -2;
    public static final int ZEUS = 1;
    public static final int ZEUS_HIDDEN = 101;
    public static final int COLOR = 0xd700e7;

    public static ArrayList<Profile> fetchProfilesWithRoles() {
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
             Statement st = connection.createStatement()) {
            ResultSet set = st.executeQuery("SELECT " +
                    "p_id, p_name, p_id_dis, p_guid, p_equipment, " +
                    "p_lastloc, p_lastupd, p_lastservertime, p_roles, " +
                    "p_side " +
                    "FROM profiles WHERE p_roles!='[]'");

            ArrayList<Profile> res = new ArrayList<>();
            while (set.next()) {
                res.add(new Profile(set.getString("p_guid"),
                        set.getLong("p_id_dis"),
                        fromString(set.getString("p_roles"))));
            }
            return res;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Profile getProfileByDisID(long userid) {
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
             Statement st = connection.createStatement()) {
            final ResultSet res = st.executeQuery("SELECT " +
                    "p_id, p_name, p_id_dis, p_guid, p_equipment, " +
                    "p_lastloc, p_lastupd, p_lastservertime, p_roles, " +
                    "p_side " +
                    "FROM profiles WHERE p_id_dis='" + userid + "'");
            if (!res.next())
                throw new DescribedException(STR.getString("comms.err.unknown_person.title"),
                        STR.getString("comms.err.unknown_person"));
            long disID = res.getLong("p_id_dis");
            String steamid = res.getString("p_guid");
            HashSet<Integer> roles = fromString(res.getString("p_roles"));
            return new Profile(steamid, disID, roles);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Profile getProfileByUID(String userid) {
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
             Statement st = connection.createStatement()) {
            final ResultSet res = st.executeQuery("SELECT " +
                    "p_id, p_name, p_id_dis, p_guid, p_equipment, " +
                    "p_lastloc, p_lastupd, p_lastservertime, p_roles, " +
                    "p_side " +
                    "FROM profiles WHERE p_guid='" + userid + "'");
            if (!res.next()) return null;
            long disID = res.getLong("p_id_dis");
            String steamid = res.getString("p_guid");
            HashSet<Integer> roles = fromString(res.getString("p_roles"));
            return new Profile(steamid, disID, roles);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void commitRoles(HashSet<Integer> newRoles, String uid, long userid, int id, String act) {
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
             Statement st = connection.createStatement()) {
            final String roles = fromSet(newRoles);
            st.executeUpdate(
                "INSERT INTO profiles (p_roles, p_id_dis, p_guid) " +
                    "VALUES ('"+roles+"', '"+userid+"', '"+uid+"') " +
                    "ON DUPLICATE KEY UPDATE " +
                    "p_roles='"+ roles +"', p_id_dis='"+userid+"' ");
            st.executeUpdate("INSERT INTO assign_log (guid, role, action) " +
                    "VALUES ('" + uid + "', '"+id+"', '"+act+"')");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashSet<Integer> getRoles(ResultSet profile) {
        try {
            return fromString(profile.getString("p_roles"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashSet<Integer> fromString(String list) {
        final Matcher matcher = Pattern.compile("[-+]?\\d+").matcher(list);
        HashSet<Integer> res = new HashSet<>();
        while (matcher.find())
            res.add(Integer.parseInt(matcher.group()));
        return res;
    }

    public static String fromSet(HashSet<Integer> set) {
        StringBuilder sb = new StringBuilder("[ ");
        boolean first = true;
        for (Integer integer: set) {
            if (!first)
                sb.append(", ");
            else first = false;
            sb.append(integer);
        }
        return sb.append(" ]").toString();
    }

    public static MessageEmbed msgList() {
        return new EmbedBuilder()
                .setTitle(STR.getString("comm.roles.get.title"))
                .setDescription(STR.getString("comm.roles.get"))
                .setColor(COLOR)
                .build();
    }

    public static MessageEmbed msgListByID(ArrayList<Profile> profiles, int id) {
        StringBuilder users = new StringBuilder();
        for (Profile profile: profiles)
            users.append(String.format("<@%d>\n", profile.userid));
        return new EmbedBuilder().setTitle(String.format(STR.getString("comm.roles.users"), id))
                .setColor(COLOR)
                .setDescription(users)
                .build();
    }

    public static void msgDescribeUpdate(Profile profile, MessageChannel channel) {
        msgDescribeUpdate(profile, channel, STR.getString("comm.roles.updated.title"));
    }

    public static void msgDescribeUpdate(Profile profile, MessageChannel channel, String title) {
        channel.sendMessage(new EmbedBuilder()
                .setTitle(title)
                .setColor(0x11E011)
                .setDescription(String.format(STR.getString("comm.roles.updated"),
                        profile.userid, profile.uid, profile.roles.toString()))
                .build()).queue();
    }

    static long getLastLogin(String steamid64) {
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
             Statement st = connection.createStatement()) {
            ResultSet resultSet = st.executeQuery("SELECT c_lastupd FROM characters WHERE c_uid='" + steamid64 + "'");

            long time = 0;
            while (resultSet.next()) {
                Timestamp timestamp = resultSet.getTimestamp(1);
                long nt = timestamp != null ? timestamp.getTime() : 0;
                if (nt > time) time = nt;
            }

            return time;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Profile {
        public final String uid;
        public final long userid;
        public final HashSet<Integer> roles;

        public Profile(String uid, long userid, HashSet<Integer> roles) {
            this.uid = uid;
            this.userid = userid;
            this.roles = roles;
        }
    }
}
