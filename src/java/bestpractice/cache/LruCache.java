package java.bestpractice.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by wuyongbo on 2015/8/5.
 *
 * �������ʹ�õ�cache��ƣ���Ҫ����������⣺
 * 1����ѯ����ʹ�õ���
 * 2�������ʹ�õ��������
 * �����������ݽṹ���Ը����������¶�������ʹ�õķ��������β�����ʹ�õķ�������ͷ��
 * ������������ô�����������ҵ����˳������ǳ��ò��ҷ�����ʱ�临�Ӷ�O(n)��
 * ��һ��˼����ʹ��HashMap���Խ��ʱ�临�Ӷȵ����⣻���⣬�ƶ�����Ԫ��Ҳ��Ҫ�������ܵĵط���
 *
 * Java��LinkedHashMap���Խ��Ԫ���ƶ������⣻���ṩ����������Ԫ�ص�˳��һ�ǰ��ղ���˳�����У����ǰ��շ���˳�����С�
 * ʹ�õڶ������з�ʽ������ʵ����get���Զ������ʹ�õ�Ԫ�طŵ���ǰ�˵�����
 * ��Ҫע����ǣ������ݽṹ�Ƿ��̰߳�ȫ�ġ�
 *
 * �и�С���ɣ�ֻ��HashMap��ʵ�����Ϲ��ܣ���key��ǰ�߼�һ���ֶ�����"#1#"����ʾ������һ��
 */
public class LruCache<K, V> {

    private final LinkedHashMap<K, V> map;
    private int maxSize;
    private int size;

    private int hitCount;       //���д���
    private int missCount;      //δ���д���
    private int createCount;    //��������
    private int putCount;       //��Ӵ���
    private int evictionCount;  //ɾ������

    public LruCache(int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>(0, 0.75f, true);
    }

    //���Ԫ��
    public final V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        V previous;
        synchronized (this) {
            putCount++;
            size += safeSizeOf(key, value);
            previous = map.put(key, value);
            if (previous != null) {
                size -= safeSizeOf(key, value);
            }
        }

        //Ϊɶ����������ͬkey��ǰvalue�� ������Ϊ����̳���Ҫ
        if (previous != null) {
            entryRemoved(false, key, previous, value);
        }

        trimToSize(maxSize);
        return null;
    }

    //ɾ��Ԫ��
    public final V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V previous;
        synchronized (this) {
            previous = map.remove(key);
            if (previous != null) {
                size -= safeSizeOf(key, previous);
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, null);
        }

        return previous;
    }

    /**
     * Clear the cache, calling {@link #entryRemoved} on each removed entry.
     */
    public final void evictAll() {
        trimToSize(-1); // -1 will evict 0-sized elements
    }

    //����Ԫ��
    public final V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }

       /*
         * Attempt to create a value. This may take a long time, and the map
         * may be different when create() returns. If a conflicting value was
         * added to the map while create() was working, we leave that value in
         * the map and release the created value.
         */

        V createdValue = create(key);
        if (createdValue == null) {
            return null;
        }

        synchronized (this) {
            createCount++;
            mapValue = map.put(key, createdValue);

            if (mapValue != null) {
                // There was a conflict so undo that last put
                map.put(key, mapValue);
            } else {
                size += safeSizeOf(key, createdValue);
            }
        }

        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue);
            return mapValue;
        } else {
            trimToSize(maxSize);
            return createdValue;
        }
    }

    //�޶�size
    public void trimToSize(int maxSize) {
        if (maxSize <= 0 ) {
            throw new IllegalArgumentException("maxSiz <= 0");
        }

        while (true) {
            K key;
            V value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(".sizeOf is reporting inconsistent results!");
                }

                if (size <= maxSize) {
                    break;
                }

                Map.Entry<K, V> toEvict = map.entrySet().iterator().next();
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= sizeOf(key, value);
                evictionCount++;
            }

            entryRemoved(true, key, value, null);
        }
    }

    /**
     * Called for entries that have been evicted or removed. This method is
     * invoked when a value is evicted to make space, removed by a call to
     * {@link #remove}, or replaced by a call to {@link #put}. The default
     * implementation does nothing.
     *
     * <p>The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * @param evicted true if the entry is being removed to make space, false
     *     if the removal was caused by a {@link #put} or {@link #remove}.
     * @param newValue the new value for {@code key}, if it exists. If non-null,
     *     this removal was caused by a {@link #put}. Otherwise it was caused by
     *     an eviction or a {@link #remove}.
     */
    protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue) {}

    private int safeSizeOf(K key, V value) {
        int result = sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value);
        }
        return result;
    }

    protected int sizeOf(K key, V value) {
        return 1;
    }
    /**
     * Called after a cache miss to compute a value for the corresponding key.
     * Returns the computed value or null if no value can be computed. The
     * default implementation returns null.
     *
     * <p>The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * <p>If a value for {@code key} exists in the cache when this method
     * returns, the created value will be released with {@link #entryRemoved}
     * and discarded. This can occur when multiple threads request the same key
     * at the same time (causing multiple values to be created), or when one
     * thread calls {@link #put} while another is creating a value for the same
     * key.
     */
    protected V create(K key) {
        return null;
    }

    //���Բ�ѯ
    public synchronized final int maxSize() {
        return maxSize;
    }

    public synchronized final int size() {
        return this.map.size();
    }

    public synchronized final int missCount() {
        return missCount;
    }

    public synchronized final int hitCount() {
        return hitCount;
    }

    public synchronized final int createCount() {
        return createCount;
    }

    public synchronized final int putCount() {
        return putCount;
    }

    public synchronized final int evictionCount() {
        return evictionCount;
    }

    /**
     * Returns a copy of the current contents of the cache, ordered from least
     * recently accessed to most recently accessed.
     */
    public synchronized final Map<K, V> snapshot() {
        return new LinkedHashMap<K, V>(map);
    }
}
