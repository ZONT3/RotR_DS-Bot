package ru.zont.uinondsb.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.dsbot.core.tools.Messages;
import ru.zont.dsbot.core.tools.Strings;
import ru.zont.uinondsb.tools.TStatus;

public class StatusStatistics extends ServerStatusEntry {
    @Override
    MessageEmbed getInitialMsg() {
        return TStatus.Msg.serverStatisticInitial();
    }

    @Override
    void update(Message entryMessage) {
        final TStatus.ServerInfoStruct info = TStatus.retrieveInfo();
        entryMessage.editMessage(TStatus.Msg.serverStatistic(info.record, info.total)).queue();
    }
}
