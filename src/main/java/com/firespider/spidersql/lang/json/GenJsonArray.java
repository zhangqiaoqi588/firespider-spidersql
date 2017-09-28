package com.firespider.spidersql.lang.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GenJsonArray extends GenJsonElement {
    private final List<GenJsonElement> elements;


    //确保线程安全
    public GenJsonArray() {
        this.elements = new CopyOnWriteArrayList<>();
    }

    @Override
    GenJsonArray deepCopy() {
        GenJsonArray result = new GenJsonArray();
        for (GenJsonElement element : elements) {
            result.add(element.deepCopy());
        }
        return result;
    }

    public <T> void add(T value) {
        if (value instanceof GenJsonElement) {
            elements.add((GenJsonElement) value);
        } else if (value == null) {
            elements.add(GenJsonNull.INSTANCE);
        } else {
            GenJsonPrimitive<T> element = new GenJsonPrimitive<>(value);
            elements.add(element);
        }
    }

    public void addAll(GenJsonArray array) {
        elements.addAll(array.elements);
    }

    public Iterator<GenJsonElement> iterator() {
        return elements.iterator();
    }

    public GenJsonElement get(int i) {
        return elements.get(i);
    }

    public GenJsonElement remove(int index) {
        return elements.remove(index);
    }

    public boolean contains(GenJsonElement element) {
        return elements.contains(element);
    }

    public int size() {
        return elements.size();
    }

    @Override
    public boolean equals(Object o) {
        return (o == this) || (o instanceof GenJsonArray && ((GenJsonArray) o).elements.equals(elements));
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (GenJsonElement element : elements) {
            sb.append(element.toString()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1).append("]");
        return sb.toString();
    }
}
