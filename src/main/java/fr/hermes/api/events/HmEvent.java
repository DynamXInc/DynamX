package fr.hermes.api.events;

public interface HmEvent<T> {
    void register(T listener);

    boolean unregister(T listener);

    T invoker();
}
