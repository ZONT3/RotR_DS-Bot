package ru.zont.uinondsb;

import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.tools.Configs;
import ru.zont.uinondsb.command.exec.Cmd;
import ru.zont.uinondsb.command.exec.Do;
import ru.zont.uinondsb.command.exec.Exec;
import ru.zont.uinondsb.command.exec.Term;
import ru.zont.uinondsb.command.*;

import javax.security.auth.login.LoginException;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws LoginException, InterruptedException {
        Configs.setGlobalPropsDefaults(new Properties(){{
            setProperty("command_prefix", "t.");
            setProperty("TA_IDS", "331524458806247426,183600856560566272");
            setProperty("ALLOWED_SERVERS", "795084260083236874,331526118635208716");
        }});
        Configs.writeDefaultGlobalProps();

        handleArguments(args);

        ZDSBot bot = new ZDSBot(args[0], Globals.version,
                "", "");
        registerCommands(bot);
        bot.create().awaitReady();
    }

    private static void registerCommands(ZDSBot bot) {
        // Нет, я не дегенерад, просто Reflections перестали работать на линуксе какого-то хуя. Помогите мне.
        bot.commandAdapters = new CommandAdapter[]{
                new Roles(bot),
                new Cmd(bot),
                new Do(bot),
                new Exec(bot),
                new Term(bot),
                new Clear(bot),
                new Config(bot),
                new GMs(bot),
                new Help(bot),
                new Ping(bot),
                new Say(bot)
        };
    }

    private static void handleArguments(String[] args) throws LoginException, IllegalArgumentException {
        if (args.length < 2) throw new LoginException("Too few arguments");

        Globals.dbConnection = args[1];
    }
}
