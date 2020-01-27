package io.github.uclarocketproject.daqd;

public interface SockHandler<T> {
    String handle(T arg) throws Exception;
}
