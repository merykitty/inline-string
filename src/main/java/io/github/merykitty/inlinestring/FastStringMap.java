package io.github.merykitty.inlinestring;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FastStringMap<V> implements Map<InlineString.ref, V>{
    private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;
    private static final int LOAD_FACTOR_SHIFT = 1;

    private Node<V>[] table;
    private int size;

    @SuppressWarnings("unchecked")
    public FastStringMap(int initialCapacity) {
        table = (Node<V>[]) new Node[initialCapacity];
    }

    public FastStringMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKeyInline(InlineString key) {
        return getNode(hash(key), key).hasValue();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof InlineString k) {
            return containsKeyInline(k);
        } else {
            return false;
        }
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    public V getInline(InlineString key) {
        var node = getNode(hash(key), key);
        if (node.hasValue()) {
            return node.node().value();
        } else {
            return null;
        }
    }

    @Override
    public V get(Object key) {
        if (key instanceof InlineString k) {
            return getInline(k);
        } else {
            return null;
        }
    }

    public V putInline(InlineString key, V value) {
        if (table.length >> LOAD_FACTOR_SHIFT < (size + 1)) {
            grow(table.length << 1);
        }
        int h = hash(key);
        var node = putNode(h, key);
        V result;
        if (node.hasValue()) {
            result = node.node().value();
        } else {
            result = null;
            size++;
        }
        table[node.index()] = new Node<>(h, key, Objects.requireNonNull(value));
        return result;
    }

    @Override
    public V put(InlineString.ref key, V value) {
        return putInline(key, value);
    }

    public V removeInline(InlineString key) {
        var node = getNode(hash(key), key);
        if (node.hasValue()) {
            var temp = node.node();
            table[node.index()] = temp.delete();
            size--;
            return temp.value();
        } else {
            return null;
        }
    }

    @Override
    public V remove(Object key) {
        if (key instanceof InlineString k) {
            return removeInline(k);
        } else {
            return null;
        }
    }

    @Override
    public void putAll(Map<? extends InlineString.ref, ? extends V> m) {
        int estimatedSize = size + m.size();
        int nCapacity = computeCapacity(estimatedSize << LOAD_FACTOR_SHIFT);
        if (nCapacity > table.length) {
            grow(nCapacity);
        }
        if (m instanceof FastStringMap) {
            // Fast path, not need to recompute hash
            @SuppressWarnings("unchecked")
            var im = (FastStringMap<? extends V>) m;
            for (int i = 0; i < im.table.length; i++) {
                var temp = im.table[i];
                if (temp.inserted() && !temp.deleted()) {
                    var node = putNode(temp.hash(), temp.key());
                    table[node.index()] = new Node<>(temp.hash(), temp.key(), temp.value());
                    if (!node.hasValue()) {
                        size++;
                    }
                }
            }
        } else {
            for (var entry : m.entrySet()) {
                var key = entry.getKey();
                int h = hash(key);
                var node = putNode(h, key);
                table[node.index()] = new Node<>(h, key, Objects.requireNonNull(entry.getValue()));
                if (!node.hasValue()) {
                    size++;
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void clear() {
        size = 0;
        table = (Node<V>[]) new Node[DEFAULT_INITIAL_CAPACITY];
    }

    @Override
    public Set<InlineString.ref> keySet() {
        return null;
    }

    @Override
    public Collection<V> values() {
        return null;
    }

    @Override
    public Set<Entry<InlineString.ref, V>> entrySet() {
        return null;
    }

    public V getOrDefaultInline(InlineString key, V defaultValue) {
        int h = hash(key);
        var node = getNode(h, key);
        if (node.hasValue()) {
            return node.node().value();
        } else {
            return Objects.requireNonNull(defaultValue);
        }
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        if (key instanceof InlineString k) {
            return getOrDefaultInline(k, defaultValue);
        } else {
            return Objects.requireNonNull(defaultValue);
        }
    }

    @Override
    public void forEach(BiConsumer<? super InlineString.ref, ? super V> action) {
        for (int i = 0, j = 0; j < size && i < table.length; i++) {
            var temp = table[i];
            if (temp.inserted() && !temp.deleted()) {
                action.accept(temp.key(), temp.value());
                j++;
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super InlineString.ref, ? super V, ? extends V> function) {
        for (int i = 0, j = 0; j < size && i < table.length; i++) {
            var temp = table[i];
            if (temp.inserted() && !temp.deleted()) {
                var nValue = function.apply(temp.key(), temp.value());
                table[i] = new Node<>(temp.hash(), temp.key(), nValue);
                j++;
            }
        }
    }

    @Override
    public V putIfAbsent(InlineString.ref key, V value) {
        return Map.super.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return Map.super.remove(key, value);
    }

    @Override
    public boolean replace(InlineString.ref key, V oldValue, V newValue) {
        return Map.super.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(InlineString.ref key, V value) {
        return Map.super.replace(key, value);
    }

    @Override
    public V computeIfAbsent(InlineString.ref key, Function<? super InlineString.ref, ? extends V> mappingFunction) {
        return Map.super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(InlineString.ref key, BiFunction<? super InlineString.ref, ? super V, ? extends V> remappingFunction) {
        return Map.super.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(InlineString.ref key, BiFunction<? super InlineString.ref, ? super V, ? extends V> remappingFunction) {
        return Map.super.compute(key, remappingFunction);
    }

    @Override
    public V merge(InlineString.ref key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return Map.super.merge(key, value, remappingFunction);
    }

    @__primitive__
    private record OptionalNode<V>(boolean hasValue, int index, Node<V> node) {}

    @__primitive__
    private record Node<V>(int hash, InlineString key, V value, boolean inserted, boolean deleted)
            implements Map.Entry<InlineString.ref, V> {
        Node(int hash, InlineString key, V value) {
            this(hash, key, value, true, false);
        }

        Node<V> delete() {
            return new Node<>(this.hash, this.key, this.value, this.inserted, true);
        }

        @Override
        public InlineString getKey() {
            return key();
        }

        @Override
        public V getValue() {
            return value();
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }

    private static int hash(InlineString key) {
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    private OptionalNode<V> getNode(int h, InlineString key) {
        int i = h & (table.length - 1);
        while (true) {
            var temp = table[i];
            if (!temp.inserted()) {
                return OptionalNode.default;
            } else if (!temp.deleted() && h == temp.hash() && key.equals(temp.key())) {
                return new OptionalNode<>(true, i, temp);
            } else {
                i++;
                if (i == table.length) {
                    i = 0;
                }
            }
        }
    }

    private OptionalNode<V> putNode(int h, InlineString key) {
        int i = h & (table.length - 1);
        int firstDeleted = -1;
        while (true) {
            var temp = table[i];
            if (!temp.inserted()) {
                return new OptionalNode<>(false, firstDeleted == -1 ? i : firstDeleted, Node.default);
            } else if (h == temp.hash() && key.equals(temp.key())) {
                return new OptionalNode<>(true, i, temp);
            } else {
                if (temp.deleted() && firstDeleted == -1) {
                    firstDeleted = i;
                }
                i++;
                if (i == table.length) {
                    i = 0;
                }
            }
        }
    }

    // Put node to a new table, the table is guaranteed to not have any deleted
    // node or node that equals to the inserted one, use for resize method
    private int putNodeEx(int h, InlineString key) {
        int i = h & (table.length - 1);
        while (true) {
            var temp = table[i];
            if (!temp.inserted()) {
                return i;
            } else {
                i++;
                if (i == table.length) {
                    i = 0;
                }
            }
        }
    }

    // Get the lowest power of 2 larger than the input
    private static int computeCapacity(int requestedCapacity) {
        requestedCapacity--;
        for (int i = 0; i < 32; i++) {
            if (requestedCapacity >>> i == 0) {
                return 1 << i;
            }
        }
        // can't reach here, due to an int contains only 32 bit
        throw new AssertionError();
    }

    // Trusted method, nCapacity is always a power of 2
    @SuppressWarnings("unchecked")
    private void grow(int nCapacity) {
        if (nCapacity < 0) {
            throw new OutOfMemoryError("Too many elements");
        }
        var oldTable = table;
        table = (Node<V>[])new Node[nCapacity];
        for (int i = 0, j = 0; j < size && i < oldTable.length; i++) {
            var temp = oldTable[i];
            if (temp.inserted() && !temp.deleted()) {
                int index = putNodeEx(temp.hash(), temp.key());
                table[index] = temp;
                j++;
            }
        }
    }
}
