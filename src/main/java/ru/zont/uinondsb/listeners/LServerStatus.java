package ru.zont.uinondsb.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.tools.Configs;
import ru.zont.dsbot.core.tools.Messages;
import ru.zont.uinondsb.tools.Commons;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.zont.dsbot.core.tools.Strings.STR;

public class LServerStatus extends ListenerAdapter {
    private List<ServerStatusEntry> entryList;
    private List<Thread> threadList;
    private Map<Class<? extends ServerStatusEntry>, Message> messages;

    private static final ServerStatusMap titles = new ServerStatusMap() {{
        put(STR.getString("status.main.title"), StatusMain.class);
        put(STR.getString("status.statistics.title"), StatusStatistics.class);
        put(STR.getString("comm.gms.get.title"), StatusGMs.class);
    }};

    private ArrayList<ServerStatusEntry> buildEntryList() {
        return new ArrayList<ServerStatusEntry>() {{
            add(new StatusGMs());
            add(new StatusStatistics());
            add(new StatusMain());
        }};
    }

    public Message getMessage(Class<? extends ServerStatusEntry> key) {
        return messages.get(key);
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        TextChannel channel = null;
        String channelStatusID = Commons.getChannelStatusID();
        for (Guild guild: event.getJDA().getGuilds()) {
            channel = guild.getTextChannelById(channelStatusID);
            if (channel != null) break;
        }
        if (channel == null)
            throw new RuntimeException("Cannot find server state channel with id " + channelStatusID);

        entryList = buildEntryList();
        threadList = new ArrayList<>();
        messages = new HashMap<>();

        prepare(channel);
        setup(channel);
    }

    private void prepare(TextChannel channel) {
        boolean allPresent = true;
        List<Message> history = channel.getHistory().retrievePast(50).complete();
        for (Message message: history) {
            Class<? extends ServerStatusEntry> klass = null;
            List<MessageEmbed> embeds = message.getEmbeds();
            if (embeds.size() == 1) {
                String title = embeds.get(0).getTitle();
                if (titles.containsKey(title)) {
                    klass = titles.get(title);
                    messages.put(klass, message);
                }
            }

            if (klass == null) {
                allPresent = false;
                break;
            }
        }
        // Если хоть одно сообщение отсутствует - удаляем все, что бы не нарушать их порядок.
        if (!allPresent) {
            for (Message message: history) message.delete().queue();
            messages.clear();
        }
    }

    @SuppressWarnings("BusyWait")
    private void setup(TextChannel channel) {
        for (ServerStatusEntry entry: entryList) {
            Message message;
            if (!messages.containsKey(entry.getClass())) {
                message = channel.sendMessage(entry.getInitialMsg()).complete();
                messages.put(entry.getClass(), message);
            } else message = messages.get(entry.getClass());

            Thread thread = new Thread(null, () -> {
                while (!Thread.interrupted()) {
                    try {
                        entry.update(message);
                    } catch (Exception e) {
                        message.editMessage(Messages.error(
                                STR.getString("err.update_fail"),
                                String.format("Class: %s, Exception: %s: %s",
                                        entry.getClass().getSimpleName(),
                                        e.getClass().getSimpleName(),
                                        e.getLocalizedMessage()))).queue();
                    }
                    try { Thread.sleep(entry.getPeriod()); }
                    catch (InterruptedException ignored) { break; }
                }
            }, String.format("%s ServerStatus Worker", entry.getClass().getSimpleName()));
            thread.start();
            threadList.add(thread);
        }
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        for (Thread thread: threadList) thread.interrupt();
        for (Map.Entry<Class<? extends ServerStatusEntry>, Message> e: messages.entrySet())
            e.getValue().delete().complete();
    }

    private static class ServerStatusMap extends HashMap<String, Class<? extends ServerStatusEntry>> { }
}
