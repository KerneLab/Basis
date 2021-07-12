package org.kernelab.basis;

import java.util.Iterator;

public interface CloseableIterator<E> extends Iterator<E>, Closeable
{
}
