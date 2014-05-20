package org.f1x.util;

import org.junit.Assert;
import org.junit.Test;

public class Test_ByteArrayReference {

    @Test
    public void testSetMethod() {
        byte[] array = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        assertSetMethodThrows(null, -1, 0);
        assertSetMethodThrows(null, 0, -1);
        assertSetMethodThrows(array, -1, 0);
        assertSetMethodThrows(array, 0, -1);
        assertSetMethodThrows(array, 3, -1);
        assertSetMethodThrows(array, 11, 0);
        assertSetMethodThrows(array, 10, 1);

        assertSetMethodDoesNotThrow(null, 0, 0);
        assertSetMethodDoesNotThrow(array, 0, 0);
        assertSetMethodDoesNotThrow(array, 10, 0);
        assertSetMethodDoesNotThrow(array, 9, 1);
        assertSetMethodDoesNotThrow(array, 0, 10);
    }

    @Test
    public void testCharAtMethod() {
        byte[] array = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        assertCharAtMethod(null, 0, 0);
        assertCharAtMethod(array, 0, 0);
        assertCharAtMethod(array, 0, 9);
        assertCharAtMethod(array, 3, 7);
        assertCharAtMethod(array, 4, 3);
    }

    @Test
    public void testSubSequenceMethod() {
        byte[] array = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        assertSubSequenceMethod(null, 0, 0);
        assertSubSequenceMethod(array, 0, 0);
        assertSubSequenceMethod(array, 0, 9);
        assertSubSequenceMethod(array, 3, 7);
        assertSubSequenceMethod(array, 4, 3);
    }

    private void assertSetMethodThrows(byte[] array, int offset, int length) {
        try {
            new ByteArrayReference(array, offset, length);
            Assert.fail();
        } catch (IndexOutOfBoundsException e) {
        }
    }

    private void assertSetMethodDoesNotThrow(byte[] array, int offset, int length) {
        new ByteArrayReference(array, offset, length);
    }

    private void assertSubSequenceMethod(byte[] array, int offset, int length) {
        CharSequence sequence = new ByteArrayReference(array, offset, length);

        assertSubSequenceMethodThrows(-1, 0, sequence);
        assertSubSequenceMethodThrows(0, -1, sequence);
        assertSubSequenceMethodThrows(0, length + 1, sequence);
        assertSubSequenceMethodThrows(1, 0, sequence);

        String expected = array == null ? "" : new String(array, offset, length);
        Assert.assertTrue(expected.contentEquals(sequence));
    }

    private void assertSubSequenceMethodThrows(int start, int end, CharSequence sequence) {
        try {
            sequence.subSequence(start, end);
            Assert.fail();
        } catch (IndexOutOfBoundsException e) {
        }
    }

    private void assertCharAtMethod(byte[] array, int offset, int length) {
        CharSequence sequence = new ByteArrayReference(array, offset, length);

        assertCharAtMethodThrows(-2, sequence);
        assertCharAtMethodThrows(-1, sequence);
        assertCharAtMethodThrows(length, sequence);
        assertCharAtMethodThrows(length + 1, sequence);

        for (int index = 0; index < length; index++)
            Assert.assertEquals((char) array[offset + index], sequence.charAt(index));
    }

    private void assertCharAtMethodThrows(int index, CharSequence sequence) {
        try {
            sequence.charAt(index);
            Assert.fail();
        } catch (IndexOutOfBoundsException e) {
        }
    }

}
