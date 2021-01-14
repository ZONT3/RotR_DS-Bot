package ru.zont.uinondsb.tools;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.uinondsb.listeners.StatusMain;

import java.awt.*;
import java.time.LocalTime;
import java.util.List;

import static ru.zont.dsbot.core.tools.Strings.STR;

public class TStatus {
    public static class Msg {
        public static MessageEmbed status(StatusMain.ServerInfoStruct serverInfo, String gmMention) {
            long time = serverInfo.time;
            short online = serverInfo.count;
            short playersRecord = serverInfo.record;
            long restart = serverInfo.uptime;
            List<String> gms = serverInfo.gms;
            for (int i = 0; i < gms.size(); i++)
                gms.set(i, Commons.trimNick(gms.get(i)));
            if (time >= 45000) return serverInactive();

            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(STR.getString("status.main.title"))
                    .addField(STR.getString("status.main.online"), Commons.countPlayers(online), true)
                    .addField(
                            STR.getString("status.main.restart"),
                            String.format( STR.getString("time.hm"),
                                    restart / 60 / 60,
                                    restart / 60 % 60 ),
                            true);

            StringBuilder sb = new StringBuilder();
            StringBuilder sb1 = new StringBuilder();
            for (String gm: gms) {
                String app = (sb1.length() > 0 ? ", " : "") + gm;
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
                sb.append(noGmString(gmMention));
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

        public static MessageEmbed serverStatistic(short playersRecord, int playersTotal) {
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
