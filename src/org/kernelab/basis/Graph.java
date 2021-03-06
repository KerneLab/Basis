package org.kernelab.basis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.kernelab.basis.Canal.Action;

public class Graph<N, E>
{
	public static class CyclicPathDetectException extends RuntimeException
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1902178279943980566L;

		public CyclicPathDetectException(String msg)
		{
			super(msg);
		}
	}

	public static class DefaultLinkAttrSetFactory<E> implements LinkAttrSetFactory<E>
	{
		@Override
		public Collection<E> newSet()
		{
			return new HashSet<E>();
		}
	}

	public static class Edge<N, E>
	{
		public final N	source;

		public final N	target;

		public final E	attr;

		public Edge(N source, N target, E attr)
		{
			this.source = source;
			this.target = target;
			this.attr = attr;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			if (obj == null)
			{
				return false;
			}
			if (!(obj instanceof Edge))
			{
				return false;
			}
			Edge<?, ?> other = (Edge<?, ?>) obj;
			if (source == null)
			{
				if (other.source != null)
				{
					return false;
				}
			}
			else if (!source.equals(other.source))
			{
				return false;
			}
			if (target == null)
			{
				if (other.target != null)
				{
					return false;
				}
			}
			else if (!target.equals(other.target))
			{
				return false;
			}
			if (attr == null)
			{
				if (other.attr != null)
				{
					return false;
				}
			}
			else if (!attr.equals(other.attr))
			{
				return false;
			}
			return true;
		}

		public int getDegree()
		{
			return 1;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			result = prime * result + ((attr == null) ? 0 : attr.hashCode());
			return result;
		}

		public Edge<N, E> reverse()
		{
			return new Edge<N, E>(target, source, attr);
		}

		@Override
		public String toString()
		{
			return this.source + " -> " + this.target + " @ " + this.attr;
		}

		public Trace<N> toTrace()
		{
			return new Trace<N>(this.source, this.target);
		}
	}

	public static class Link<N, E> extends Edge<N, Collection<E>>
	{
		public Link(N source, N target, Collection<E> attr)
		{
			super(source, target, attr);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			if (obj == null)
			{
				return false;
			}
			if (!(obj instanceof Link))
			{
				return false;
			}
			Link<?, ?> other = (Link<?, ?>) obj;
			if (source == null)
			{
				if (other.source != null)
				{
					return false;
				}
			}
			else if (!source.equals(other.source))
			{
				return false;
			}
			if (target == null)
			{
				if (other.target != null)
				{
					return false;
				}
			}
			else if (!target.equals(other.target))
			{
				return false;
			}
			return true;
		}

		@Override
		public int getDegree()
		{
			return this.attr.size();
		}

		@Override
		public Link<N, E> reverse()
		{
			return new Link<N, E>(target, source, attr);
		}

		public Iterable<Edge<N, E>> toEdges()
		{
			return Canal.of(this.attr).map(new Mapper<E, Edge<N, E>>()
			{
				@Override
				public Edge<N, E> map(E attr)
				{
					return new Edge<N, E>(source, target, attr);
				}
			});
		}

		@Override
		public String toString()
		{
			StringBuilder buf = new StringBuilder();
			if (this.attr != null)
			{
				boolean first = true;
				for (E attr : this.attr)
				{
					if (first)
					{
						first = false;
					}
					else
					{
						buf.append(' ');
					}
					buf.append(attr == null ? "null" : attr.toString());
				}
			}
			return this.source + " -> " + this.target + " @ " + buf.toString();
		}
	}

	public static interface LinkAttrSetFactory<E>
	{
		public Collection<E> newSet();
	}

	public static class Trace<N> extends Link<N, Object>
	{
		public Trace(N source, N target)
		{
			super(source, target, null);
		}

		@Override
		public int getDegree()
		{
			return 1;
		}

		@Override
		public Trace<N> reverse()
		{
			return new Trace<N>(target, source);
		}

		@Override
		public Iterable<Edge<N, Object>> toEdges()
		{
			return Canal.some(this).map(new Mapper<Trace<N>, Edge<N, Object>>()
			{
				@Override
				public Edge<N, Object> map(Trace<N> el)
				{
					return new Edge<N, Object>(el.source, el.target, null);
				}
			});
		}

		@Override
		public String toString()
		{
			return this.source + " -> " + this.target;
		}

		@Override
		public Trace<N> toTrace()
		{
			return this;
		}
	}

	public static void main(String[] args)
	{
		Graph<Object, Object> net = new Graph<Object, Object>();

		Tools.debug("====1====");
		net.add(1, 2);
		assert net.hasCycle() == false;

		Tools.debug("====2====");
		net.clear() //
				.add(1, 2) //
				.add(2, 1) //
		;
		assert net.hasCycle() == true;

		Tools.debug("====3====");
		net.clear() //
				.add(1, 2) //
				.add(2, 3) //
				.add(3, 1) //
		;
		assert net.hasCycle() == true;

		Tools.debug("====4====");
		net.clear() //
				.add(1, 2) //
				.add(1, 3) //
				.add(2, 4) //
				.add(3, 4) //
		;
		assert net.hasCycle() == false;

		Tools.debug("====5====");
		net.clear() //
				.add(1, 2) //
				.add(2, 3) //
				.add(3, 1) //
				.add(3, 4) //
				.add(1, 4) //
		;
		assert net.hasCycle() == true;

		Tools.debug("====6====");
		net.clear() //
				.add(1, 2) //
				.add(2, 3) //
				.add(1, 3) //
				.add(3, 4) //
				.add(4, 5) //
				.add(5, 6) //
				.add(6, 4) //
		;
		assert net.hasCycle() == true;

		Tools.debug("====7====");
		net.clear() //
				.add(1, 3) //
				.add(1, 4) //
				.add(2, 4) //
				.add(3, 6) //
				.add(4, 8) //
				.add(4, 5) //
				.add(6, 7) //
				.add(6, 8) //
				.add(7, 9) //
				.add(8, 9) //
		;

		for (Object n : net.getNodes())
		{
			if (net.getInDegree(n) == 0)
			{
				Tools.debug(n);
			}
		}

		Tools.debug(Canal.of(net.topSort()).toString(","));

		Tools.debug(Canal.of(net.reverse().topSort()).toString(","));

		Tools.debug(net.stratify());
	}

	private LinkAttrSetFactory<E>		linkAttrSetFactory;

	private Map<Trace<N>, Link<N, E>>	map;

	private Set<Link<N, E>>				links;

	private Set<N>						nodes;

	private Map<N, Set<Link<N, E>>>		inLinks;

	private Map<N, Set<Link<N, E>>>		outLinks;

	public Graph()
	{
		this(new DefaultLinkAttrSetFactory<E>());
	}

	public Graph(LinkAttrSetFactory<E> factory)
	{
		this.setLinkAttrSetFactory(factory);
		this.init();
	}

	protected Graph<N, E> add(Link<N, E> link)
	{
		this.getNodes().add(link.source);
		this.getNodes().add(link.target);
		this.getMap().put(link.toTrace(), link);
		this.getLinks().add(link);
		this.getInLinks(link.target).add(link);
		this.getOutLinks(link.source).add(link);
		return this;
	}

	public Graph<N, E> add(N source, N target)
	{
		return add(source, target, null);
	}

	public Graph<N, E> add(N source, N target, E attr)
	{
		if (source == null || target == null)
		{
			throw new NullPointerException("source=" + source + " " + target + "=" + target);
		}

		Trace<N> trace = new Trace<N>(source, target);

		Link<N, E> link = this.getMap().get(trace);

		if (link == null)
		{
			link = new Link<N, E>(source, target, this.newLinkAttrSet());
			this.add(link);
		}

		if (link.attr != null)
		{
			link.attr.add(attr);
		}

		return this;
	}

	public Graph<N, E> clear()
	{
		this.getNodes().clear();
		this.getMap().clear();
		this.getLinks().clear();
		this.getInLinks().clear();
		this.getOutLinks().clear();
		return this;
	}

	protected Link<N, E> findCycle()
	{
		return this.findCycle(new HashSet<N>(this.getNodes()));
	}

	protected Link<N, E> findCycle(Set<N> nodes)
	{
		Set<Link<N, E>> visited = new HashSet<Link<N, E>>();
		Set<N> removes = new HashSet<N>();
		Link<N, E> find = null;

		while (!nodes.isEmpty())
		{
			removes.clear();

			N visit = null;

			for (N node : nodes)
			{
				removes.add(node);

				if (!this.getOutLinks(node).isEmpty())
				{
					visit = node;
					break;
				}
			}

			nodes.removeAll(removes);

			if (visit != null)
			{
				if ((find = findCycle(nodes, visit, visited)) != null)
				{
					return find;
				}
			}
		}
		return null;
	}

	protected Link<N, E> findCycle(Set<N> nodes, N node, Set<Link<N, E>> visited)
	{
		Link<N, E> find = null;

		for (Link<N, E> link : this.getOutLinks(node))
		{
			if (visited.contains(link))
			{
				return link;
			}

			nodes.remove(link.target);

			if (!this.getOutLinks(link.target).isEmpty())
			{
				visited.add(link);

				if ((find = findCycle(nodes, link.target, visited)) != null)
				{
					return find;
				}

				visited.remove(link);
			}
		}

		return null;
	}

	public int getDegree(N node)
	{
		return this.getInDegree(node) + this.getOutDegree(node);
	}

	public Canal<?, Edge<N, E>> getEdges()
	{
		return mapLinksToEdges(this.getLinks());
	}

	public int getInDegree(N node)
	{
		return Canal.of(this.getInLinks(node)).fold(0, new Reducer<Link<N, E>, Integer>()
		{
			@Override
			public Integer reduce(Integer res, Link<N, E> el)
			{
				return res + el.getDegree();
			}
		});
	}

	public Canal<?, Edge<N, E>> getInEdges(N node)
	{
		return mapLinksToEdges(this.getInLinks(node));
	}

	protected Map<N, Set<Link<N, E>>> getInLinks()
	{
		return inLinks;
	}

	public Set<Link<N, E>> getInLinks(N node)
	{
		return this.getLinkSet(this.getInLinks(), node);
	}

	public Canal<?, N> getInNeighbors(N node)
	{
		return Canal.of(this.getInLinks(node)).map(new Mapper<Link<N, E>, N>()
		{
			@Override
			public N map(Link<N, E> el)
			{
				return el.source;
			}
		});
	}

	protected LinkAttrSetFactory<E> getLinkAttrSetFactory()
	{
		return linkAttrSetFactory;
	}

	public Collection<Link<N, E>> getLinks()
	{
		return links;
	}

	protected Set<Link<N, E>> getLinkSet(Map<N, Set<Link<N, E>>> map, N node)
	{
		if (!map.containsKey(node))
		{
			map.put(node, new HashSet<Link<N, E>>());
		}
		return map.get(node);
	}

	protected Map<Trace<N>, Link<N, E>> getMap()
	{
		return map;
	}

	public Canal<?, N> getNeighbors(N node)
	{
		return this.getInNeighbors(node).union(this.getOutNeighbors(node)).distinct();
	}

	public Set<N> getNodes()
	{
		return nodes;
	}

	public int getOutDegree(N node)
	{
		return Canal.of(this.getOutLinks(node)).fold(0, new Reducer<Link<N, E>, Integer>()
		{
			@Override
			public Integer reduce(Integer res, Link<N, E> el)
			{
				return res + el.getDegree();
			}
		});
	}

	public Canal<?, Edge<N, E>> getOutEdges(N node)
	{
		return mapLinksToEdges(this.getOutLinks(node));
	}

	protected Map<N, Set<Link<N, E>>> getOutLinks()
	{
		return outLinks;
	}

	public Set<Link<N, E>> getOutLinks(N node)
	{
		return this.getLinkSet(this.getOutLinks(), node);
	}

	public Canal<?, N> getOutNeighbors(N node)
	{
		return Canal.of(this.getOutLinks(node)).map(new Mapper<Link<N, E>, N>()
		{
			@Override
			public N map(Link<N, E> el)
			{
				return el.target;
			}
		});
	}

	public boolean hasCycle()
	{
		return this.findCycle() != null;
	}

	protected void init()
	{
		this.setMap(new HashMap<Trace<N>, Link<N, E>>());
		this.setLinks(new HashSet<Link<N, E>>());
		this.setNodes(new HashSet<N>());
		this.setInLinks(new HashMap<N, Set<Link<N, E>>>());
		this.setOutLinks(new HashMap<N, Set<Link<N, E>>>());
	}

	protected Canal<?, Edge<N, E>> mapLinksToEdges(Collection<Link<N, E>> links)
	{
		return Canal.of(links).flatMap(new Mapper<Link<N, E>, Iterable<Edge<N, E>>>()
		{
			@Override
			public Iterable<Edge<N, E>> map(Link<N, E> key)
			{
				return key.toEdges();
			}
		});
	}

	protected Collection<E> newLinkAttrSet()
	{
		return this.getLinkAttrSetFactory().newSet();
	}

	public boolean removeNode(N node)
	{
		if (!this.getNodes().remove(node))
		{
			return false;
		}

		Set<Link<N, E>> inLinks = this.getInLinks().remove(node);

		if (inLinks != null && !inLinks.isEmpty())
		{
			this.getLinks().removeAll(inLinks);

			for (Link<N, E> l : inLinks)
			{
				this.getMap().remove(l.toTrace());
			}

			for (Link<N, E> link : inLinks)
			{
				this.getOutLinks(link.source).removeAll(inLinks);
			}

			inLinks.clear();
		}

		Set<Link<N, E>> outLinks = this.getOutLinks().remove(node);

		if (outLinks != null && !outLinks.isEmpty())
		{
			this.getLinks().removeAll(outLinks);

			for (Link<N, E> l : outLinks)
			{
				this.getMap().remove(l.toTrace());
			}

			for (Link<N, E> link : outLinks)
			{
				this.getInLinks(link.target).removeAll(outLinks);
			}

			outLinks.clear();
		}

		return true;
	}

	public void removeNodes(Collection<N> nodes)
	{
		for (N node : nodes)
		{
			this.removeNode(node);
		}
	}

	public Graph<N, E> reverse()
	{
		Graph<N, E> reverse = new Graph<N, E>();

		for (Link<N, E> link : this.getLinks())
		{
			reverse.add(link.reverse());
		}

		return reverse;
	}

	protected void setInLinks(Map<N, Set<Link<N, E>>> inLinks)
	{
		this.inLinks = inLinks;
	}

	protected void setLinkAttrSetFactory(LinkAttrSetFactory<E> linkAttrSetFactory)
	{
		this.linkAttrSetFactory = linkAttrSetFactory;
	}

	protected void setLinks(Set<Link<N, E>> links)
	{
		this.links = links;
	}

	protected void setMap(Map<Trace<N>, Link<N, E>> map)
	{
		this.map = map;
	}

	protected void setNodes(Set<N> nodes)
	{
		this.nodes = nodes;
	}

	protected void setOutLinks(Map<N, Set<Link<N, E>>> outLinks)
	{
		this.outLinks = outLinks;
	}

	/**
	 * Return a TreeMap which divides nodes into each set according to its
	 * topology level (zero-based).
	 * 
	 * @return
	 */
	public TreeMap<Integer, Set<N>> stratify()
	{
		Map<Integer, Set<N>> res = new HashMap<Integer, Set<N>>();

		int level = 0;
		Set<N> nodes = null;
		for (Entry<N, Integer> entry : this.topLevel().entrySet())
		{
			level = entry.getValue();
			nodes = res.get(level);
			if (nodes == null)
			{
				nodes = new HashSet<N>();
				res.put(level, nodes);
			}
			nodes.add(entry.getKey());
		}

		return new TreeMap<Integer, Set<N>>(res);
	}

	/**
	 * Return a Map which indicates the topology level (zero-based) of each
	 * node.
	 * 
	 * @return
	 */
	public Map<N, Integer> topLevel()
	{
		if (this.hasCycle())
		{
			throw new CyclicPathDetectException("");
		}

		Map<N, Variable<Integer>> map = new HashMap<N, Variable<Integer>>();

		for (N node : this.getNodes())
		{
			if (this.getInDegree(node) == 0)
			{
				this.topLevel(node, 0, map);
			}
		}

		return Canal.of(map).mapValues(new Mapper<Variable<Integer>, Integer>()
		{
			@Override
			public Integer map(Variable<Integer> el)
			{
				return el.value;
			}
		}).collectAsMap(new HashMap<N, Integer>());
	}

	protected void topLevel(N node, final int level, final Map<N, Variable<Integer>> map)
	{
		Variable<Integer> lv = map.get(node);
		if (lv == null)
		{
			map.put(node, new Variable<Integer>(level));
		}
		else if (level > lv.value)
		{
			lv.value = level;
		}

		this.getOutNeighbors(node).foreach(new Action<N>()
		{
			@Override
			public void action(N el)
			{
				topLevel(el, level + 1, map);
			}
		});
	}

	/**
	 * Return topology sort result. The nodes on source side will be treated as
	 * dependencies.
	 * 
	 * @return
	 */
	public List<N> topSort()
	{
		Graph<N, Object> trace = this.toTrace();

		LinkedList<N> top = new LinkedList<N>();

		Set<N> heads = new HashSet<N>();
		Set<N> nexts = null;

		while (!trace.getNodes().isEmpty())
		{
			heads.clear();

			for (N node : nexts != null ? nexts : trace.getNodes())
			{
				if (trace.getInDegree(node) == 0)
				{
					heads.add(node);
				}
			}

			if (heads.isEmpty())
			{
				throw new CyclicPathDetectException("");
			}

			top.addAll(heads);

			if (nexts != null)
			{
				nexts.clear();
			}
			else
			{
				nexts = new HashSet<N>();
			}

			for (N node : heads)
			{
				this.getOutNeighbors(node).collect(nexts);
			}

			trace.removeNodes(heads);
		}

		return top;
	}

	/**
	 * Return a new Graph with the same nodes as this Graph but the edges are
	 * all Trace objects.
	 * 
	 * @return
	 */
	public Graph<N, Object> toTrace()
	{
		Graph<N, Object> trace = new Graph<N, Object>();

		for (Trace<N> t : this.getMap().keySet())
		{
			trace.add(t);
		}

		return trace;
	}
}
