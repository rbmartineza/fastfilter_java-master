package org.fastfilter;

/**
 * An approximate membership filter.
 */
public interface Filter {

    /**
     * Whether the set may contain the key.
     *
     * @param key the key
     * @return true if the set might contain the key, and false if not
     */
    boolean mayContain(long key);

    /**
     * Get the number of bits in thhe filter.
     *
     * @return the number of bits
     */
    long getBitCount();

    /**
     * Whether the add operation (after construction) is supported.
     *
     * @return true if yes
     */
    default boolean supportsAdd() {
        return false;
    }

    /**
     * Add a key.
     *
     * @param key the key
     */
    default void add(long key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Add a key in aux xor.
     *
     * @param key the key
     */
    default void aux(long [] key) {
        throw new UnsupportedOperationException();
    }

   /**
     * To calculate the frequency of a key.
     *
     * @param key the key
     */
    default int frequency(long key) {
        throw new UnsupportedOperationException();
    }

     /**
     * To resset the frequency counter.
     *
     * @param key the key
     */
    default void resetCms(long[] key) {
        throw new UnsupportedOperationException();
    }

    /**
     * To return the number of memory access.
     *
     * @return number of memory access
     */
    default int getMemAccess() {
        throw new UnsupportedOperationException();
    }

    /**
     * To return the number of memory access.
     *
     * @return number of memory access
     */
    default void resetMemAccess() {
        throw new UnsupportedOperationException();
    }

    /**
     * Whether the remove operation is supported.
     *
     * @return true if yes
     */
    default boolean supportsRemove() {
        return false;
    }

    /**
     * Remove a key.
     *
     * @param key the key
     */
    default void remove(long key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the number of set bits. This should be 0 for an empty filter.
     *
     * @return the number of bits
     */
    default long cardinality() {
        return -1;
    }

}
