package ru.zont.uinondsb.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.sql.SQLException;

public abstract class ServerStatusEntry {
    abstract MessageEmbed getInitialMsg();

    abstract void update(Message entryMessage);

    long getPeriod() { return 20000; }

    public static class NoResponseException extends SQLException {
        public NoResponseException() {
            super("No response from DB");
        }
    }
}
