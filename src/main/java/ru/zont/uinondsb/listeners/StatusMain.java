package ru.zont.uinondsb.listeners;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.dsbot.core.tools.Configs;
import ru.zont.dsbot.core.tools.Strings;
import ru.zont.uinondsb.tools.Commons;
import ru.zont.uinondsb.tools.TStatus;

public class StatusMain extends ServerStatusEntry {

    @Override
    MessageEmbed getInitialMsg() {
        return TStatus.Msg.serverInactive();
    }

    @Override
    void update(Message entryMessage) {
        final TStatus.ServerInfoStruct inf = TStatus.retrieveInfo();
        String d;
        if (inf.time > 45000) d = ".d";
        else d = "";

        entryMessage.getJDA().getPresence().setActivity(Activity.watching(String.format(
                Strings.STR.getString("status.main.status" + d), inf.count, Configs.getPrefix())));
        entryMessage.editMessage(TStatus.Msg.status(inf, Commons.getRoleGmID())).complete();
    }

}
