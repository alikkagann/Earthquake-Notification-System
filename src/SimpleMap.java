import java.util.ArrayList;
import java.util.List;

public class SimpleMap<K, V> implements Map<K, V> {
    private List<Entry<K, V>> entries;

    public SimpleMap() {
        entries = new ArrayList<>();
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public V get(K key) {
        for (int i = 0; i < entries.size(); i++) {
            Entry<K, V> entry = entries.get(i);
            if (entry.getKey().equals(key))
                return entry.getValue();
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        for (int i = 0; i < entries.size(); i++) {
            Entry<K, V> entry = entries.get(i);
            if (entry.getKey().equals(key)) {
                V oldValue = entry.getValue();
                entries.set(i, new MapEntry<>(key, value));
                return oldValue;
            }
        }
        entries.add(entries.size(), new MapEntry<>(key, value));
        return null;
    }

    @Override
    public V remove(K key) {
        for (int i = 0; i < entries.size(); i++) {
            Entry<K, V> entry = entries.get(i);
            if (entry.getKey().equals(key)) {
                V value = entry.getValue();
                entries.remove(i);
                return value;
            }
        }
        return null;
    }

    @Override
    public Iterable<K> keySet() {
        List<K> keys = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            keys.add(keys.size(), entries.get(i).getKey());
        }
        return keys;
    }

    @Override
    public Iterable<V> values() {
        List<V> values = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            values.add(values.size(), entries.get(i).getValue());
        }
        return values;
    }

    @Override
    public Iterable<Entry<K, V>> entrySet() {
        return entries;
    }

    private static class MapEntry<K, V> implements Entry<K, V> {
        private final K key;
        private final V value;

        public MapEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() { return key; }

        @Override
        public V getValue() { return value; }
    }
}
