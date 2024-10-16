package lv.ailab.lvtb.universalizer.utils;

import java.util.Objects;

/**
 * Ordered tuple.
 */
public class Tuple<E, F>
{
	public E first;
	public F second;
	
	public Tuple (E e, F f)
	{
		first = e;
		second = f;
	}

	static public <E,F> Tuple<E,F> of(E first, F second){
		return new Tuple<>(first, second);
	}
	
	// This is needed for putting Tuples in hash structures (hashmaps, hashsets).
	@Override
	public boolean equals (Object o)
	{
		if (o == null) return false;
		if (this.getClass() != o.getClass()) return false;
		Tuple<?, ?> obj = (Tuple<?, ?>) o;
		return Objects.equals(this.first, obj.first)
				&& Objects.equals(this.second, obj.second);
	}
	
	// This is needed for putting Tuples in hash structures (hashmaps, hashsets).
	@Override
	public int hashCode()
	{
		return 2719 *(first == null ? 1 : first.hashCode())
				+ (second == null ? 1 : second.hashCode());
	}
}