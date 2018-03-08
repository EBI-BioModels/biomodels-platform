package net.biomodels.jummp.models

class JummpEntry<K extends Serializable, V extends Serializable> implements Map.Entry<K,V>, Serializable {

    private K key;
    private V value;

    JummpEntry(K key, V value) {
        this.key = key
        this.value = value
    }

    JummpEntry() {
    }

    void setKey(K key) {
        this.key = key
    }

    K getKey() {
        return key;
    }

    V getValue() {
        return value;
    }

    V setValue(V value) {
        this.value = value;
        return null;
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        JummpEntry jEntry = (JummpEntry) o

        if (key != jEntry.key) return false
        if (value != jEntry.value) return false

        return true
    }

    int hashCode() {
        int result
        result = (key != null ? key.hashCode() : 0)
        result = 31 * result + (value != null ? value.hashCode() : 0)
        return result
    }
}
