package ru.zont.uinondsb.listeners;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.dsbot.core.tools.Configs;
import ru.zont.dsbot.core.tools.Strings;
import ru.zont.uinondsb.command.Config;
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
        entryMessage.getJDA().getPresence().setActivity(Activity.watching(String.format(
                Strings.STR.getString("status.main.status"), inf.count, Configs.getPrefix())));
        entryMessage.editMessage(TStatus.Msg.status(inf, Commons.getRoleGmID())).complete();
    }

}
