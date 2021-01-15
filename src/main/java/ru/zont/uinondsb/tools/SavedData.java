package ru.zont.uinondsb.tools;

import java.io.*;

@SuppressWarnings("unchecked")
public class SavedData<T extends Serializable> implements Serializable {
    private static final long DEFAULT_CACHE_LIFETIME = 10000;
    private static final File DATA_DIR = new File("data");
    private static final String EXT = ".bin";

    private final Class<T> klass;
    private T data;
    private long stamp = 0;
    private final File file;

    static {
        if (!DATA_DIR.exists() && !DATA_DIR.mkdirs())
            new IOException("Cannot create data dirs").printStackTrace();
    }

    public SavedData(Class<T> klass, String key) {
        this.klass = klass;
        file = new File(DATA_DIR, key + EXT);
    }

    public T restore() {
        return restore(DEFAULT_CACHE_LIFETIME);
    }

    public T restore(T defaultValue) {
        return restore(DEFAULT_CACHE_LIFETIME, defaultValue);
    }

    public T restore(long cacheLifetime, T defaultValue) {
        try {
            return restore(cacheLifetime);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof FileNotFoundException)
                return defaultValue;
            else throw e;
        }
    }

    public T restore(long cacheLifetime) {
        if (System.currentTimeMillis() - stamp < cacheLifetime && data != null) return data;

        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            final Object o = ois.readObject();
            if (!klass.isInstance(o)) throw new RuntimeException("Found object with same key but different class");

            data = (T) o;
            stamp = System.currentTimeMillis();
            return data;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(T newData) {
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            if (!file.delete()) throw new IOException("Cannot delete file");
            data = newData;
            oos.writeObject(data);
            oos.flush();
            stamp = System.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
