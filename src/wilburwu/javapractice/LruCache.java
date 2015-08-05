package wilburwu.javapractice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by wuyongbo on 2015/8/5.
 * 最近最少使用的cache设计，主要解决几个问题：
 * 1，查询最晚使用的项
 * 2，给最近使用的项做标记
 * 链表这种数据结构可以辅助完成这件事儿，最晚使用的放在链表结尾，最近使用的放在链表头。
 * 但是问题是怎么能在链表中找到该项，顺序遍历是常用查找方法，时间复杂度O(n)。
 * 进一步思考，使用HashMap可以解决时间复杂度的问题；另外，移动链表元素也是要提升性能的地方；
 *
 * Java的LinkedHashMap可以解决元素移动的问题；它提供了两种排列元素的顺序，一是按照插入顺序排列，二是按照访问顺序排列。
 * 使用第二种排列方式，可以实现在get后自动将最近使用的元素放到最前端的需求。
 * 需要注意的是：该数据结构是非线程安全的。
 *
 * 有个小技巧，只用HashMap来实现以上功能：在key的前边加一个字段例如"#1#"，表示访问了一次
 */
public class LruCache<K, V> {

    private final LinkedHashMap<K, V> map;
    private int maxSize;
    private int size;

    private int hitCount;       //命中次数
    private int missCount;      //未命中次数
    private int createCount;    //创建次数
    private int putCount;       //添加次数
    private int evictionCount;  //删除次数

    public LruCache(int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>(0, 0.75f, true);
    }

    //添加元素
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

        //为啥单独处理相同key的前value？ 可能是为子类继承需要
        if (previous != null) {
            entryRemoved(false, key, previous, value);
        }

        trimToSize(maxSize);
        return null;
    }

    //删除元素
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

    //查找元素
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

    //限定size
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

    //属性查询
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
