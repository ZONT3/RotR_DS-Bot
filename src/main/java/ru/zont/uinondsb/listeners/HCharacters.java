package ru.zont.uinondsb.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.handler.LStatusHandler;
import ru.zont.dsbot.core.tools.Configs;
import ru.zont.dsbot.core.tools.Strings;
import ru.zont.dsbot.core.tools.Tools;
import ru.zont.uinondsb.Globals;

import java.io.File;
import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static ru.zont.dsbot.core.tools.Strings.STR;

public class HCharacters extends LStatusHandler {
    private static final File stateFile = new File("db", "characters.bin");

    public HCharacters(ZDSBot bot) {
        super(bot);
    }

    @Override
    public void prepare(ReadyEvent event) throws Exception {

    }

    @Override
    public void update() throws Exception {
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
             Statement st = connection.createStatement()) {
            ResultSet resultSet = st.executeQuery("SELECT c_roles, c_id, c_name, c_side FROM characters");
            ArrayList<State> storedState = getStoredState();
            ArrayList<State> newState = new ArrayList<>();

            while (resultSet.next()) {
                JsonElement e = JsonParser.parseString(resultSet.getString(1));
                int id = resultSet.getInt(2);
                String name = resultSet.getString(3);
                String side = resultSet.getString(4);

                if (!e.isJsonArray()) throw new IllegalStateException("Corrupted role list on character " + name);
                JsonArray roles = e.getAsJsonArray();
                HashSet<Integer> rolesSet = new HashSet<>();
                for (JsonElement role: roles)
                    rolesSet.add(role.getAsInt());

                Optional<State> first = storedState.stream().filter(state -> id == state.id).findFirst();
                if (first.isPresent()) {
                    HashSet<Integer> storedRoles = first.get().roles;
                    for (Integer role: rolesSet)
                        if (!storedRoles.contains(role))
                            reportNewRole(name, role, rolesSet);
                } else reportNewCharacter(name, side, rolesSet);

                newState.add(new State(id, rolesSet));
            }

            storeState(newState);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void reportNewCharacter(String name, String side, HashSet<Integer> rolesSet) {
        getChannel().sendMessage(new EmbedBuilder()
                .setTitle(STR.getString("handler.characters.new.title"))
                .appendDescription(String.format(STR.getString("handler.characters.new.nick"), name) + "\n")
                .appendDescription(String.format(STR.getString("handler.characters.new.side"), side) + "\n")
                .appendDescription(String.format(STR.getString("handler.characters.new.roles"), Arrays.toString(rolesSet.toArray())))
                .setColor(0x2020f0)
                .build()).queue();
    }

    private void reportNewRole(String name, Integer role, HashSet<Integer> rolesSet) {
        getChannel().sendMessage(new EmbedBuilder()
                .setTitle(STR.getString("handler.characters.ra.title"))
                .appendDescription(String.format(STR.getString("handler.characters.ra.nick"), name) + "\n")
                .appendDescription(String.format(STR.getString("handler.characters.ra.role"), role) + "\n")
                .appendDescription(String.format(STR.getString("handler.characters.ra.roles"), Arrays.toString(rolesSet.toArray())))
                .setColor(0x2020f0)
                .build()).queue();
    }

    private TextChannel getChannel() {
        return Tools.tryFindTChannel(Configs.getID("channel_charlog"), getJda());
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<State> getStoredState() {
        ArrayList<State> res = (ArrayList<State>) Tools.retrieveObject(stateFile);
        if (res == null) res = new ArrayList<>();
        return res;
    }

    private static void storeState(ArrayList<State> state) {
        Tools.commitObject(stateFile, state);
    }

    @Override
    public long getPeriod() {
        return 30 * 1000;
    }

    private static class State implements Serializable {
        private final int id;
        private final HashSet<Integer> roles;

        public State(int id, HashSet<Integer> roles) {
            this.id = id;
            this.roles = roles;
        }

        public int getId() {
            return id;
        }

        public HashSet<Integer> getRoles() {
            return roles;
        }
    }
}
