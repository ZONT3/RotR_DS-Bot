package ru.zont.uinondsb.command.exec;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.tools.Messages;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class ExecHandler {
    public static final int OUT_MAX_LEN = 2000;
    public static final String TYPE_STDERR = "stderr";
    public static final String TYPE_STDOUT = "stdout";
    public static final int COLOR_END_SUCC = 0x00DA00;
    public static final int COLOR_END_FAIL = 0x8F0000;
    public static final int COLOR_OUT = 0x311b92;
    public static final int COLOR_STDERR = 0xBB0000;

    private final MessageChannel workingChannel;
    private final long started = System.currentTimeMillis();

    private final long pid;
    private final String name;
    private final OutList stdoutMessages = new OutList();
    private final OutList stderrMessages = new OutList();

    private final Parameters params;
    private final SubprocessListener sl;
    private Message statusMessage;

    static class Parameters {
        boolean verbose = true;
        boolean single_window = false;
        Consumer<Void> onVeryFinish = null;
    }

    public ExecHandler(@NotNull SubprocessListener sl, long pid, @NotNull MessageChannel workingChannel, Parameters params) {
        this.workingChannel = workingChannel;
        this.params = params;
        this.sl = sl;
        this.pid = pid;

        name = this.sl.getProcName();
        connectSL(this.sl);
        sl.start();
        if (params.verbose)
            printStart();
    }

    private void printStart() {
        statusMessage = workingChannel.sendMessage(new EmbedBuilder()
                .setTitle(String.format("Process [%d] %s started", pid, name))
                .setColor(0xE0E0E0)
                .setTimestamp(Instant.now())
                .build()).complete();
        statusMessage.addReaction("\u23F3").queue();
    }

    private void printEnd(int code) {
        long millis = System.currentTimeMillis() - started;
        long sec = millis / 1000;
        workingChannel.sendMessage(new EmbedBuilder()
                .setColor(code == 0 ? COLOR_END_SUCC : COLOR_END_FAIL)
                .setTitle(String.format("Process [%d] finished", pid))
                .setDescription(String.format(
                        "Name: %s\n" +
                                "Duration: %d.%03ds\n" +
                                "Exit code: `%d`",
                        name, sec, millis % 1000, code
                )).build()).queue();
    }

    private void connectSL(SubprocessListener sl) {
        sl.setOnStdout(this::appendStdout);
        sl.setOnStderr(this::appendStderr);
        sl.setOnError(this::onError);
        sl.setOnFinish(this::onFinish);
    }

    private void onError(Exception e) {
        if (checkWChPrint()) return;
        Messages.printError(workingChannel, "Error in SubprocessListener", Messages.describeException(e));
    }

    private synchronized void onFinish(int code) {
        Exec.removeProcess(pid);
        if (params.onVeryFinish != null)
            params.onVeryFinish.accept(null);

        if (statusMessage != null) {
            statusMessage.removeReaction("\u23F3").queue();
            Messages.addOK(statusMessage);
        }

        if (checkWChPrint()) return;
        if (code != 0 || params.verbose) {
            printEnd(code);
        }

        for (OutListEntry e: stdoutMessages) {
            List<MessageEmbed> embeds = e.m.getEmbeds();
            if (embeds.size() < 1) continue;
            e.edit(e.s, new EmbedBuilder(embeds.get(0))
                    .setColor(code == 0 ? COLOR_END_SUCC : COLOR_END_FAIL)
                    .build());
        }
    }

    private void appendStdout(String lines) {
        appendOut(lines, stdoutMessages, TYPE_STDOUT);
    }

    private void appendStderr(String lines) {
        appendOut(lines, stderrMessages, TYPE_STDERR);
    }

    private synchronized void appendOut(String lines, OutList outList, String type) {
        if (checkWChPrint()) return;

        try {
            if (params.single_window && !type.equalsIgnoreCase(TYPE_STDERR)) {
                String[] split = lines.split("(?<=\\n)");
                outList.setContent(split[split.length - 1]);
            } else outList.appendContent(lines);

            List<String> chunks = splitByChunks(outList.getContent());
            int i;
            for (i = 0; i < chunks.size(); i++) {
                String s = chunks.get(i);
                if (i < outList.size()) {
                    if (i < outList.size() - 1 && !params.single_window)
                        continue;
                    OutListEntry entry = outList.get(i);
                    if (!entry.s.equals(s))
                        entry.edit(s, basicOutputMsg(i, type).build());
                } else sendOutput(outList, type, s);
            }
            if (i < outList.size()) {
                List<OutListEntry> excess = outList.subList(i, outList.size());
                for (OutListEntry entry: excess)
                    entry.m.delete().complete();
                excess.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Messages.printError(workingChannel, "Error in ExecHandler", Messages.describeException(e));
        }
    }

    private List<String> splitByChunks(String lines) {
        ArrayList<String> res = new ArrayList<>();
        StringBuilder nextLines = new StringBuilder();
        for (String line: lines.split("(?<=\\n)")) {
            if (nextLines.length() + line.length() <= OUT_MAX_LEN) {
                nextLines.append(line);
            } else {
                List<String> list = splitString(nextLines);
                if (list.size() <= 1) {
                    if (!nextLines.toString().isEmpty())
                        res.add(nextLines.toString());
                    nextLines = new StringBuilder(line);
                } else {
                    res.addAll(list.subList(0, list.size() - 1));
                    nextLines = new StringBuilder(list.get(list.size() - 1)).append(line);
                }
            }
        }
        if (!nextLines.toString().isEmpty())
            res.addAll(splitString(nextLines));
        return res;
    }

    @NotNull
    private List<String> splitString(CharSequence nextLines) {
        String text = nextLines.toString();
        List<String> ret = new ArrayList<>((text.length() + OUT_MAX_LEN - 1) / OUT_MAX_LEN);
        for (int start = 0; start < text.length(); start += OUT_MAX_LEN)
            ret.add(text.substring(start, Math.min(text.length(), start + OUT_MAX_LEN)));
        return ret;
    }

    private void sendOutput(OutList outList, String type, String lines) {
        outList.add(
                workingChannel.sendMessage(basicOutputMsg(outList.size(), type)
                        .setDescription("```\n" + lines + "```")
                        .build()
                ).complete(), lines);
    }

    private EmbedBuilder basicOutputMsg(int i, String type) {
        int endColor = -1;
        try {
            endColor = (sl.getExitStatus() == 0 ? COLOR_END_SUCC : COLOR_END_FAIL);
        } catch (Exception ignored) { }
        return new EmbedBuilder().setTitle(nextTitle(i, type))
                .setColor(type.equalsIgnoreCase(TYPE_STDOUT) ? (endColor < 0 ? COLOR_OUT : endColor) : COLOR_STDERR);
    }

    private String nextTitle(int i, String type) {
        return String.format("[%d] %s %s #%d", pid, name, type, i+1);
    }

    private boolean checkWChPrint() {
        if (workingChannel == null) {
            new NullPointerException("Working channel").printStackTrace();
            return true;
        }
        return false;
    }

    public long getPid() {
        return pid;
    }

    public void terminate() {
        sl.terminate();
    }

    private static class OutList extends ArrayList<OutListEntry> {
        private String content = "";

        public void add(Message m, String s) {
            add(new OutListEntry(m, s));
        }

        public String getContent() {
            return content;
        }

        public void appendContent(CharSequence a) {
            content += a.toString();
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    private static class OutListEntry {
        private final Message m;
        private String s;

        public OutListEntry(Message m, String s) {
            this.m = m;
            this.s = s;
        }

        public void edit(String ns, MessageEmbed embed) {
            m.editMessage(
                    new EmbedBuilder(embed)
                            .setDescription("```\n" + ns + "```")
                            .build()
            ).complete();
            s = ns;
        }
    }
}
