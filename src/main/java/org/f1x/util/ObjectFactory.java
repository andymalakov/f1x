package org.f1x.util;

/**
 * Object factory.
 * @param <E> type of elements created by factory method
 */
public interface ObjectFactory<E> {

    E create();

}
