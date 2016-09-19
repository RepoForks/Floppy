package io.fabianterhorst.floppy;

import java.util.HashMap;

/**
 * Created by fabianterhorst on 19.09.16.
 */

public class MemoryDisk extends Disk {

    private final HashMap<String, Object> mCache = new HashMap<>();

    public MemoryDisk(String name, String path) {
        super(name, path);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T read(String key, boolean fast) {
        T object = (T) mCache.get(key);
        if (object == null) {
            object = super.read(key, fast);
            mCache.put(key, object);
        }
        return object;
    }

    @Override
    public void write(String key, Object object, boolean fast) {
        super.write(key, object, fast);
        mCache.put(key, object);
    }

    @Override
    public void delete(String key) {
        super.delete(key);
        mCache.remove(key);
    }

    @Override
    public void deleteAll() {
        super.deleteAll();
        mCache.clear();
    }
}
