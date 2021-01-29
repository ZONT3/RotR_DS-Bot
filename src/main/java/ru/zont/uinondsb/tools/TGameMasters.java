package ru.zont.uinondsb.tools;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.dsbot.core.tools.Configs;
import ru.zont.dsbot.core.tools.Messages;
import ru.zont.uinondsb.Globals;

import java.sql.Date;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static ru.zont.dsbot.core.tools.Strings.STR;

public class TGameMasters {
    private static final long INTERVAL = 10000;
    private static long nextRetrieve = 0;
    private static ArrayList<GM> cached = null;

    public static ArrayList<GM> retrieve() {
        if (System.currentTimeMillis() < nextRetrieve && cached != null) return new ArrayList<>(cached);

        ArrayList<GM> res = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
             Statement st = connection.createStatement()) {
            ResultSet resultSet = st.executeQuery(
                    "SELECT p_guid, p_id_dis, p_name, p_lastupd, p_roles FROM profiles " +
                        "WHERE p_roles != '[]' " +
                        "ORDER BY p_lastupd"
            );
            while (resultSet.next()) {
                if (!TRoles.fromString(resultSet.getString("p_roles")).contains(TRoles.ZEUS))
                    continue;
                GM gm = new GM();
                gm.steamid64 = resultSet.getString("p_guid");
                gm.userid = resultSet.getLong("p_id_dis");
                gm.armaname = resultSet.getString("p_name");
                gm.lastlogin = resultSet.getTimestamp("p_lastupd").getTime();
                res.add(gm);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        nextRetrieve = System.currentTimeMillis() + INTERVAL;
        cached = res;
        return new ArrayList<>(cached);
    }

    public static void setGm(GM gm) {
        String cond = resolveCondition(gm);
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
             Statement st = connection.createStatement()) {
            final HashSet<Integer> roles = getRolesUpdProfile(cond, st, gm);
            roles.add(1);

            String lastupd;
            if (gm.p_lastupd != null) lastupd = "'"+gm.p_lastupd.toInstant().atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))+"'";
            else lastupd = "NULL";

            st.executeUpdate("UPDATE profiles SET p_roles = '" + TRoles.fromSet(roles) + "', " +
                    "p_id_dis = '"+gm.userid+"' WHERE " + cond);
            st.executeUpdate("UPDATE profiles SET p_lastupd = "+lastupd+" " +
                    "WHERE " + cond);
            st.executeUpdate("INSERT INTO assign_log (guid, role, action) " +
                    "VALUES ('" + gm.steamid64 + "', 1, 'add')");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String resolveCondition(GM gm) {
        if (gm.userid <= 0) throw new IllegalArgumentException("Discord ID not stated");
        if (gm.steamid64 != null)
            return "p_guid = '" + gm.steamid64 + "'";
        else return "p_id_dis = '" + gm.userid + "'";
    }

    private static HashSet<Integer> getRolesUpdProfile(String cond, Statement st, GM gm) throws SQLException {
        final ResultSet resultSet = st.executeQuery(
                "SELECT p_roles, p_guid, p_name, p_lastupd FROM profiles " +
                    "WHERE " + cond);
        if (!resultSet.next()) {
            if (gm.steamid64 == null) throw new NoSuchElementException();
            st.executeUpdate("INSERT INTO profiles (p_roles, p_guid, p_id_dis) " +
                    "VALUES ('[ ]', '"+gm.steamid64+"', '"+gm.userid+"')");
            return new HashSet<>();
        }

        gm.armaname = resultSet.getString("p_name");
        gm.p_lastupd = resultSet.getTimestamp("p_lastupd");
        gm.lastlogin = gm.p_lastupd != null ? gm.p_lastupd.toInstant().getEpochSecond() * 1000 : 0;

        return TRoles.fromString(resultSet.getString("p_roles"));
    }

    public static void removeGm(String id) throws NoUpdateException {
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
            Statement st = connection.createStatement()) {

            String cond;
            GM gm = new GM();
            if (id.matches("<@!?\\d+>")) {
                final long rId = getId(id);
                cond = "p_id_dis = '" + rId + "'";
                gm.userid = rId;
            } else if (id.matches("\\d+")) {
                cond = "p_guid = '" + id + "'";
                gm.steamid64 = id;
            }
            else throw new IllegalArgumentException("Invalid discord raw mention or steamid64");

            final HashSet<Integer> roles = getRolesUpdProfile(cond, st, gm);
            roles.remove(1);

            String lastupd;
            if (gm.p_lastupd != null) lastupd = "'"+gm.p_lastupd.toInstant().atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))+"'";
            else lastupd = "NULL";

            st.executeUpdate("UPDATE profiles SET p_roles = '" + TRoles.fromSet(roles) +
                    "', p_id_dis = " + gm.userid + " " +
                    "WHERE p_guid = " + gm.steamid64);
            st.executeUpdate("UPDATE profiles SET p_lastupd = "+lastupd+" " +
                    "WHERE " + cond);
            st.executeUpdate("INSERT INTO assign_log (guid, role, action) " +
                    "VALUES ('" + gm.steamid64 + "', 1, 'rm')");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Timestamp getAssignedDate(String steamid64) {
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
            Statement st = connection.createStatement()) {
            final ResultSet resultSet = st.executeQuery(
                    "SELECT timestamp FROM assign_log " +
                    "WHERE guid = '" + steamid64 + "' AND role = 1 AND action != 'rm'" +
                    "ORDER BY id DESC LIMIT 1");
            if (!resultSet.next()) return null;
            return resultSet.getTimestamp("timestamp");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static long getId(String raw) {
        return Long.parseLong(raw.substring(raw.matches("<@\\d+>") ? 2 : 3, raw.length() - 1));
    }

    public static class GM {
        public String steamid64;
        public long userid;
        public String armaname;
        public long lastlogin;
        public Timestamp p_lastupd;
    }

    public static class Msg {
        public static MessageEmbed statusGMsInitial() {
            return new EmbedBuilder()
                    .setTitle(STR.getString("comm.gms.get.title"))
                    .setDescription(STR.getString("status.gms.retrieving"))
                    .build();
        }

        public static MessageEmbed gmList(List<GM> gms) {
            return gmListShort(gms, false, true, true, true, true);
        }

        public static MessageEmbed gmListShort(List<GM> gms,
                                               boolean s, boolean n, boolean a, boolean o, boolean hide) {
            EmbedBuilder builder = prepareGmList(gms, hide);

            int splitIndex = gms.size() - 8;
            if (splitIndex > 0) {
                int sleep = 0, anger = 0, warn = 0;
                for (GM gm: gms.subList(0, splitIndex)) {
                    long lastLogin = gm.lastlogin;
                    switch (getEmo((System.currentTimeMillis() - lastLogin) / 1000 / 60 / 60)) {
                        case " :anger:": anger++; break;
                        case " :zzz:": sleep++; break;
                        case " :octagonal_sign:": warn++; break;
                    }
                }
                builder.appendDescription(String.format(
                        STR.getString("status.gms.short.info"),
                        Commons.countGMs(splitIndex), sleep, anger, warn, Configs.getPrefix()));
                builder.appendDescription("\n\n");
            }

            for (GM gm: gms.subList(Math.max(0, splitIndex), gms.size()))
                builder.appendDescription(buildField(gm, s, n, a, o));
            return builder.build();
        }

        public static ArrayList<EmbedBuilder> gmList(List<GM> gms,
                                                     boolean s, boolean n, boolean a, boolean o) {
            EmbedBuilder builder = prepareGmList(gms);
            ArrayList<EmbedBuilder> list = new ArrayList<>();
            list.add(builder);

            for (GM gm: gms)
                Messages.appendDescriptionSplit(buildField(gm, s, n, a, o), list);
            return list;
        }

        private static EmbedBuilder prepareGmList(List<GM> gms) {
            return prepareGmList(gms, false);
        }

        private static EmbedBuilder prepareGmList(List<GM> gms, boolean hide) {
            if (hide) filterHidden(gms);

            EmbedBuilder builder = new EmbedBuilder().setColor(0x9900ff);
            builder.setTitle(STR.getString("comm.gms.get.title"));
            for (GM gm: gms)
                if (gm.armaname == null) gm.armaname = STR.getString("comm.gms.get.unknown");

            gms.sort(Comparator.comparingLong(ob -> ob.lastlogin));
            return builder;
        }

        private static String buildField(GM gm, boolean s, boolean n, boolean a, boolean o) {
            String memberMention = gm.userid > 0 ? String.format("<@%d>", gm.userid)
                    : STR.getString("comm.gms.get.unknown.person");
            StringBuilder field = new StringBuilder(memberMention).append('\n');

            if (s) field.append(String.format(STR.getString("comm.gms.get.steamid"), gm.steamid64)).append("\n");
            if (n) field.append(String.format(STR.getString("comm.gms.get.armaname"), gm.armaname)).append("\n");
            if (a) field.append(String.format(STR.getString("comm.gms.get.assigned"), getAssigned(gm))).append("\n");
            if (o) field.append(getOnline(gm)).append("\n");

            if (s || n || a || o) field.append('\n');
            return field.toString();
        }

        private static String getAssigned(GM gm) {
            Timestamp assignedDate = TGameMasters.getAssignedDate(gm.steamid64);
            if (assignedDate == null) return STR.getString("comm.gms.get.unknown");
            return assignedDate.toInstant().atZone(ZoneId.of("GMT+3"))
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }

        private static String getOnline(GM gm) {
            long lastLogin = gm.lastlogin;
            if (lastLogin == 0) return STR.getString("comm.gms.get.lastlogin.unk");

            long diff = System.currentTimeMillis() - lastLogin;
            long min = diff / 1000 / 60;
            long hr  = min / 60;
            long day = hr / 24;

            String emo = getEmo(hr);

            if (min < 2) return STR.getString("comm.gms.get.lastlogin.n");
            else return String.format(STR.getString("comm.gms.get.lastlogin"), day, hr % 24, min % 60) + emo;
        }

        private static String getEmo(long hr) {
            if (hr > 47) return " :octagonal_sign:";
            else if (hr > 18) return " :anger:";
            else if (hr > 11) return  " :zzz:";
            else return "";
        }
    }

    public static void filterHidden(List<GM> gms) {
        final ArrayList<TRoles.Profile> profiles = TRoles.fetchProfilesWithRoles();
        profiles.removeIf(profile -> !profile.roles.contains(101));
        gms.removeIf(gm -> profiles.stream().anyMatch(profile -> profile.uid.equals(gm.steamid64)));
    }

    public static class NoUpdateException extends Exception {
        public NoUpdateException(int code) {
            super("Error code " + code);
        }
    }
}
