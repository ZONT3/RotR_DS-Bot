package ru.zont.uinondsb.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.uinondsb.tools.TGameMasters;

import java.util.ArrayList;

public class StatusGMs extends ServerStatusEntry {
    @Override
    MessageEmbed getInitialMsg() {
        return TGameMasters.Msg.statusGMsInitial();
    }

    @Override
    void update(Message entryMessage) {
        ArrayList<TGameMasters.GM> gms = TGameMasters.retrieve();
        final MessageEmbed newContent = TGameMasters.Msg.gmList(gms);
        entryMessage.editMessage(newContent).queue();
    }
}
