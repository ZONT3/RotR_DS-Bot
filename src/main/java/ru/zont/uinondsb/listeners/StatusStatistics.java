package ru.zont.uinondsb.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.dsbot.core.tools.Messages;
import ru.zont.dsbot.core.tools.Strings;

public class StatusStatistics extends ServerStatusEntry {
    @Override
    MessageEmbed getInitialMsg() {
        return Messages.notImplemented(Strings.STR.getString("status.statistics.title"));
//        return TStatus.Msg.serverStatisticInitial();
    }

    @Override
    void update(Message entryMessage) {
//        entryMessage.editMessage().queue(); TODO
    }
}
