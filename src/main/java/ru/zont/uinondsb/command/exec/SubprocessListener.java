package ru.zont.uinondsb.command.exec;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class SubprocessListener extends Thread {
    public static final int LISTEN_DELAY = 500;

    private final String name;
    private final String execLine;
    private Process process = null;

    private Consumer<String> onStdout = null;
    private Consumer<String> onStderr = null;
    private Consumer<Integer> onFinish = null;
    private Consumer<Exception> onError = null;

    private Charset charset = StandardCharsets.UTF_8;

    public static class Builder {

        private Charset charset;
        public Builder setCharset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public SubprocessListener build(@NotNull CharSequence name, @NotNull CharSequence execLine) {
            SubprocessListener l = new SubprocessListener(name, execLine);
            if (charset != null) l.charset = charset;
            return l;
        }

    }
    public SubprocessListener(@NotNull CharSequence name, @NotNull CharSequence execLine) {
        super("SpL: " + name);
        this.name = name.toString();
        this.execLine = execLine.toString();
    }

    @Override
    public void run() {
        Consumer<Exception> eCallback = e -> {
            e.printStackTrace();
            if (onError != null)
                onError.accept(e);
        };

        try {
            process = Runtime.getRuntime().exec(execLine);
            new StreamListener(process, process.getInputStream(), line -> {
                if (onStdout != null)
                    onStdout.accept(line);
            }, eCallback);
            new StreamListener(process, process.getErrorStream(), line -> {
                if (onStderr != null)
                    onStderr.accept(line);
            }, eCallback);
            process.waitFor();
            if (onFinish != null)
                onFinish.accept(getExitStatus());
        } catch (Exception e) {
            eCallback.accept(e);
        }
    }

    public void terminate() {
        process.destroy();
    }

    public int getExitStatus() {
        return process.exitValue();
    }

    public String getProcName() { return name; }

    public void setOnStdout(Consumer<String> onStdout) {
        this.onStdout = onStdout;
    }

    public void setOnStderr(Consumer<String> onStderr) {
        this.onStderr = onStderr;
    }

    public void setOnFinish(Consumer<Integer> onFinish) {
        this.onFinish = onFinish;
    }

    public void setOnError(Consumer<Exception> onError) {
        this.onError = onError;
    }

    private class StreamListener extends Thread {
        private final Process process;
        private final InputStream stream;
        private final Consumer<String> callback;
        private final Consumer<Exception> eCallback;

        private StreamListener(Process process, InputStream stream, Consumer<String> listener, Consumer<Exception> eCallback) {
            super("SpL.StL: " + name);
            this.process = process;
            this.stream = stream;
            this.callback = listener;
            this.eCallback = eCallback;
            start();
        }

        @Override
        public void run() {
            BufferedReader scanner = new BufferedReader(new InputStreamReader(stream, charset));
            StringBuilder buffer = new StringBuilder();
            try {
                long lstListen = 0;
                while (process.isAlive() || scanner.ready() || !buffer.toString().isEmpty()) {
                    if (scanner.ready()) {
                        buffer.append(Character.toChars(scanner.read()));
                    } else if (!buffer.toString().isEmpty()) {
                        long l = System.currentTimeMillis() - lstListen;
                        if (l < LISTEN_DELAY)
                            sleep(LISTEN_DELAY - l);
                        lstListen = System.currentTimeMillis();

                        callback.accept(buffer.toString());
                        buffer = new StringBuilder();
                    }
                }
            } catch (Exception e) {
                eCallback.accept(e);
            }
        }
    }


    public static void main(String[] args) throws IOException {
//            SubprocessListener test = new SubprocessListener("Test", "python -X utf8 -u \"D:\\Users\\ZONT_\\Documents\\Test.py\"");
        SubprocessListener test = new SubprocessListener("Test", "java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp \"D:\\Users\\ZONT_\\Documents\" Test2");
        PrintWriter writer = new PrintWriter(new FileOutputStream(new File("test.txt")), true);
        test.setOnStdout(param -> {
            System.out.println("STDOUT: " + param);
            writer.println(param);
        });


        test.setOnStderr(param -> System.err.println("STDERR: " + param));
        test.setOnFinish(param -> System.out.println("EXIT CODE: " + param));
        test.setOnError(param -> System.out.println("ERROR: " + param.getClass().getSimpleName() + ": " + param.getLocalizedMessage()));
        test.start();
    }

}
