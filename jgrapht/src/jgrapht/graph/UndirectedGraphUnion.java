/*
 * (C) Copyright 2009-2017, by Ilya Razenshteyn and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
package jgrapht.graph;

import java.util.*;

import jgrapht.*;
import jgrapht.util.*;

/**
 * An undirected version of the read-only union of two graphs.
 * 
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 */
public class UndirectedGraphUnion<V, E>
    extends GraphUnion<V, E, UndirectedGraph<V, E>>
    implements UndirectedGraph<V, E>
{
    private static final long serialVersionUID = -740199233080172450L;

    /**
     * Construct a new undirected graph union.
     * 
     * @param g1 the first graph
     * @param g2 the second graph
     * @param operator the weight combiner (policy for edge weight calculation)
     */
    public UndirectedGraphUnion(
        UndirectedGraph<V, E> g1, UndirectedGraph<V, E> g2, WeightCombiner operator)
    {
        super(g1, g2, operator);
    }

    /**
     * Construct a new undirected graph union. The union will use the {@link WeightCombiner#SUM}
     * weight combiner.
     * 
     * @param g1 the first graph
     * @param g2 the second graph
     */
    public UndirectedGraphUnion(UndirectedGraph<V, E> g1, UndirectedGraph<V, E> g2)
    {
        super(g1, g2);
    }

    @Override
    public int degreeOf(V vertex)
    {
        Set<E> res = edgesOf(vertex);
        return res.size();
    }
}

// End UndirectedGraphUnion.java
