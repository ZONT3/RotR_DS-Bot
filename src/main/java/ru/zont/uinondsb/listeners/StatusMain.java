package ru.zont.uinondsb.listeners;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import ru.zont.dsbot.core.commands.NotImplementedException;
import ru.zont.dsbot.core.tools.Configs;
import ru.zont.dsbot.core.tools.Messages;
import ru.zont.dsbot.core.tools.Strings;
import ru.zont.uinondsb.tools.Commons;
import ru.zont.uinondsb.tools.TStatus;

import java.sql.*;
import java.util.List;

public class StatusMain extends ServerStatusEntry {

    @Override
    MessageEmbed getInitialMsg() {
        return Messages.notImplemented(Strings.STR.getString("status.main.title"));
//        return TStatus.Msg.serverInactive();
    }

    @Override
    void update(Message entryMessage) {
//        ServerInfoStruct struct = retrieveServerInfo();
//
//        entryMessage.getJDA().getPresence().setActivity(
//                Activity.watching(
//                        String.format(Strings.STR.getString("status.main.status"),
//                                struct.count,
//                                Configs.getPrefix() )));
//
//        entryMessage.editMessage(TStatus.Msg.status(struct, Commons.getRoleGmID())).queue();
    }

    public static ServerInfoStruct retrieveServerInfo() {
        throw new NotImplementedException();
    }

    private static List<String> jsonList(String gms) {
        return new Gson().fromJson(gms, new TypeToken<List<String>>() {}.getType());
    }

    public static class ServerInfoStruct {
        public short count;
        public long time;
        public long uptime;
        public List<String> gms;
        public short record;
        public int total;

        public ServerInfoStruct(short count, long time, long uptime, List<String> gms, short record) {
            this.count = count;
            this.time = time;
            this.uptime = uptime;
            this.gms = gms;
            this.record = record;
        }
    }

}
