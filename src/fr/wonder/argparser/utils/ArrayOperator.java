package fr.wonder.argparser.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * The ArrayOperator class is meant for quick list-like operations on arrays.
 * <p>
 * This class should only be used when an implementation uses arrays and having
 * operations like add/remove make things more convenient. An array operator
 * instance creates and maintains a list, if the need for the list operations
 * become too frequent having an actual list may be simpler and will have way
 * less overhead than creating an array operator for each operation.
 * <p>
 * Alternatively convenience methods (all static methods of this class) may be
 * used, none will create an array operator.
 * <p>
 * Beware that although all instance methods are guaranteed to return a new
 * array object, the static methods may or may not return the given array,
 * depending on if a change was necessary or not.
 * <p>
 * Example use case: <blockquote>
 * 
 * <pre>
 *   String[] array = { "foo", "bar" };
 * 
 *   ...
 * 
 *   array = new ArrayOperator(array)
 *     .add("foobar")
 *     .toggle("foo", "foo2")
 *     .finish(String[]::new); // .finish(String.class);
 * 
 *   // array is { "bar", "foobar", "foo2" }
 * 
 * </pre>
 * 
 * </blockquote>
 */
public class ArrayOperator<T> {

	private final List<T> array;

	/** see {@link ArrayOperator} */
	public ArrayOperator() {
		this((T[]) null);
	}

	/** see {@link ArrayOperator} */
	@SafeVarargs
	public ArrayOperator(T... array) {
		if (array == null) {
			this.array = new ArrayList<>();
		} else {
			this.array = new ArrayList<>(array.length);
			add(array);
		}
	}
	
	public static <T> ArrayOperator<T> wrap(List<T> list) {
		return new ArrayOperator<>(list);
	}
	
	public static <T> ArrayOperator<T> wrapCopy(Collection<T> nativeUnits) {
		return new ArrayOperator<>(new ArrayList<>(nativeUnits));
	}
	
	private ArrayOperator(List<T> array) {
		this.array = array;
	}

	public int size() {
		return array.size();
	}
	
	public T get(int i) {
		return array.get(i);
	}

	public List<T> getList() {
		return array;
	}
	
	public List<T> getUnmodifiableList() {
		return Collections.unmodifiableList(array);
	}

	public ArrayOperator<T> add(T t) {
		array.add(t);
		return this;
	}

	public ArrayOperator<T> add(@SuppressWarnings("unchecked") T... ts) {
		for (T t : ts)
			array.add(t);
		return this;
	}

	public ArrayOperator<T> add(ArrayOperator<? extends T> ts) {
		array.addAll(ts.array);
		return this;
	}

	public ArrayOperator<T> add(Collection<? extends T> ts) {
		array.addAll(ts);
		return this;
	}

	public static <T> T[] add(T[] array, T t) {
		array = Arrays.copyOf(array, array.length + 1);
		array[array.length - 1] = t;
		return array;
	}
	
	public static <T> T[] add(T[] array, T[] ts) {
		if(array.length == 0) return ts;
		if(ts.length == 0) return array;
		int al = array.length;
		array = Arrays.copyOf(array, array.length + ts.length);
		for(int i = 0; i < ts.length; i++)
			array[al + i] = ts[i];
		return array;
	}

	public ArrayOperator<T> addIfAbsent(T t) {
		if (!array.contains(t))
			array.add(t);
		return this;
	}

	public ArrayOperator<T> addIfAbsent(@SuppressWarnings("unchecked") T... ts) {
		for (T t : ts)
			if (!array.contains(t))
				array.add(t);
		return this;
	}
	
	public ArrayOperator<T> addIfAbsent(Collection<? extends T> ts) {
		for(T t : ts) {
			if(!array.contains(t))
				array.add(t);
		}
		return this;
	}

	public ArrayOperator<T> addIfAbsent(ArrayOperator<? extends T> ts) {
		for (T t : ts.array)
			if (!array.contains(t))
				array.add(t);
		return this;
	}

	public static <T> T[] addIfAbsent(T[] array, T t) {
		for (int i = 0; i < array.length; i++)
			if (Objects.equals(array[i], t))
				return array;
		return add(array, t);
	}

	public ArrayOperator<T> addAt(int i, T t) {
		array.add(i, t);
		return this;
	}

	public ArrayOperator<T> addAt(int i, @SuppressWarnings("unchecked") T... ts) {
		for (int j = 0; j < ts.length; j++)
			array.add(i + j, ts[j]);
		return this;
	}

	public ArrayOperator<T> addAt(int i, ArrayOperator<? extends T> ts) {
		array.addAll(i, ts.array);
		return this;
	}

	public static <T> T[] addAt(int i, T[] array, T t) {
		array = Arrays.copyOf(array, array.length + 1);
		for (int j = array.length - 1; j > i; j--)
			array[j] = array[j - 1];
		array[i] = t;
		return array;
	}

	public ArrayOperator<T> remove(T t) {
		array.remove(t);
		return this;
	}

	public ArrayOperator<T> remove(@SuppressWarnings("unchecked") T... ts) {
		for (T t : ts)
			array.remove(t);
		return this;
	}

	public static <T> T[] remove(T[] array, T t) {
		for (int i = 0; i < array.length; i++) {
			if (Objects.equals(array[i], t))
				return removeAt(i, 1, array);
		}
		return array;
	}

	public ArrayOperator<T> removeAll(ArrayOperator<? extends T> ts) {
		array.removeAll(ts.array);
		return this;
	}
	
	public ArrayOperator<T> removeAll(T t) {
		array.removeIf(x -> Objects.equals(x, t));
		return this;
	}
	
	public ArrayOperator<T> removeAll(@SuppressWarnings("unchecked") T... ts) {
		array.removeIf(x -> contains(ts, x));
		return this;
	}
	
	public static <T> T[] removeAll(T[] array, T t) {
		boolean[] contained = new boolean[array.length];
		int k = 0;
		for(int i = 0; i < array.length; i++) {
			if(Objects.equals(array[i], t)) {
				contained[i] = true;
				k++;
			}
		}
		if(k == array.length)
			return array;
		@SuppressWarnings("unchecked")
		T[] narr = (T[]) Array.newInstance(array.getClass().componentType(), k);
		for(int i = array.length-1; k > 0; i--)
			narr[i] = array[--k];
		return narr;
	}

	public ArrayOperator<T> removeAt(int i) {
		array.remove(i);
		return this;
	}

	public ArrayOperator<T> removeAt(int i, int count) {
		for (int j = 0; j < count; j++)
			array.remove(i);
		return this;
	}
	
	public static <T> T[] removeAt(int i, T[] array) {
		return removeAt(i, 1, array);
	}

	public static <T> T[] removeAt(int i, int count, T[] array) {
		if (count < 0)
			throw new IndexOutOfBoundsException("Cannot remove less than zero elements");
		if (count == 0)
			return array;
		T[] a = Arrays.copyOf(array, array.length - count);
		for(int j = 0; j <= count; j++)
			a[i+j] = array[i+count+j];
		return a;
	}
	
	public ArrayOperator<T> removeIf(Predicate<T> filter) {
		array.removeIf(filter);
		return this;
	}
	
	public static <T> T[] removeIf(T[] array, Predicate<T> filter) {
		boolean[] contained = new boolean[array.length];
		int k = 0;
		for(int i = 0; i < array.length; i++) {
			if(!filter.test(array[i])) {
				contained[i] = true;
				k++;
			}
		}
		if(k == array.length)
			return array;
		@SuppressWarnings("unchecked")
		T[] narr = (T[]) Array.newInstance(array.getClass().componentType(), k);
		for(int i = array.length-1; k > 0; i--) {
			if(contained[i])
				narr[--k] = array[i];
		}
		return narr;
	}
	
	public ArrayOperator<T> retainIf(Predicate<T> filter) {
		array.removeIf(filter.negate());
		return this;
	}
	
	/** Static version of {@link #removeIf(Predicate)} */
	public static <T> T[] filter(T[] array, Predicate<T> filter) {
		return removeIf(array, filter.negate());
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] filter(Object[] array, Class<T> clazz) {
		int count = 0;
		for(Object o : array)
			if(clazz.isInstance(o))
				count++;
		T[] narr = (T[]) Array.newInstance(clazz, count);
		count = 0;
		for(Object o : array)
			if(clazz.isInstance(o))
				narr[count++] = (T) o;
		return narr;
	}
	
	public ArrayOperator<T> toggle(T t) {
		if (array.contains(t))
			array.remove(t);
		else
			array.add(t);
		return this;
	}

	public ArrayOperator<T> toggle(@SuppressWarnings("unchecked") T... ts) {
		for (T t : ts)
			toggle(t);
		return this;
	}

	public ArrayOperator<T> toggle(ArrayOperator<? extends T> ts) {
		for (T t : ts.array)
			toggle(t);
		return this;
	}
	
	public static <T> T[] toggle(T[] array, T t) {
		for(int i = 0; i < array.length; i++) {
			if(Objects.equals(array[i], t))
				return removeAt(i, 1, array);
		}
		return add(array, t);
	}
	
	public ArrayOperator<T> set(T t, boolean set) {
		if(set)
			addIfAbsent(t);
		else
			remove(t);
		return this;
	}
	
	public static <T> T[] set(T[] array, T t, boolean set) {
		if(set)
			return addIfAbsent(array, t);
		else
			return remove(array, t);
	}
	
	public <R> ArrayOperator<R> map(Function<T, R> f) {
		List<R> n = new ArrayList<>(array.size());
		for(T t : array)
			n.add(f.apply(t));
		return new ArrayOperator<>(n);
	}
	
	public static <T, R> R[] map(T[] array, IntFunction<R[]> generator, Function<T, R> f) {
		R[] n = generator.apply(array.length);
		for(int i = 0; i < array.length; i++)
			n[i] = f.apply(array[i]);
		return n;
	}
	
	public static <T, R> R[] map(T[] from, R[] to, Function<T, R> f) {
		if(from.length != to.length)
			throw new IndexOutOfBoundsException("From and To arrays are not of the same size");
		for(int i = 0; i < from.length; i++)
			to[i] = f.apply(from[i]);
		return to;
	}
	
	public static <T> Object[] map(T[] from, Function<T, Object> f) {
		return map(from, new Object[from.length], f);
	}
	
	public ArrayOperator<T> removeDuplicates() {
		for(int i = array.size()-1; i > 0; i--) {
			T t = array.get(i);
			for(int j = i-1; j >= 0; j--) {
				if(Objects.equals(array.get(j), t)) {
					array.remove(j);
					i--;
				}
			}
		}
		return this;
	}
	
	public static <T> T[] removeDuplicates(T[] array) {
		T[] na = Arrays.copyOf(array, array.length);
		int s = 0;
		for(int i = array.length-1; i > 0; i--) {
			T t = array[i];
			boolean duplicate = false;
			for(int j = i-1; j >= 0; j--) {
				if(Objects.equals(array[j], t)) {
					duplicate = true;
					break;
				}
			}
			if(!duplicate)
				na[s++] = t;
		}
		if(s != array.length)
			return Arrays.copyOfRange(na, 0, s);
		return na;
	}

	public Object[] finish() {
		return array.toArray();
	}

	public T[] finish(IntFunction<T[]> generator) {
		return array.toArray(generator);
	}

	@SuppressWarnings("unchecked")
	public T[] finish(Class<T> c) {
		return array.toArray(l -> (T[]) Array.newInstance(c, l));
	}
	
	public T[] finish(T[] array) {
		return this.array.toArray(array);
	}

	public <R> R concat(BiFunction<R, T, R> generator) { // TODO change to accumulate
		return concat(generator, null);
	}
	
	public <R> R concat(BiFunction<R, T, R> generator, R defaultValue) {
		R r = defaultValue;
		for(T t : array)
			r = generator.apply(r, t);
		return r;
	}
	
	public boolean contains(T t) {
		for(T tt : array)
			if(Objects.equals(t, tt))
				return true;
		return false;
	}
	
	public static <T, K> boolean containsMapped(T[] array, K value, Function<T, K> map) {
		for(T t : array) {
			if(Objects.equals(value, map.apply(t)))
				return true;
		}
		return false;
	}
	
	public static <T> int indexOf(T[] array, T t) {
		for(int i = 0; i < array.length; i++)
			if(Objects.equals(array[i], t))
				return i;
		return -1;
	}

	public static int indexOf(int[] array, int x) {
		for(int i = 0; i < array.length; i++)
			if(array[i] == x)
				return i;
		return -1;
	}

	public static int indexOf(float[] array, float x) {
		for(int i = 0; i < array.length; i++)
			if(array[i] == x)
				return i;
		return -1;
	}

	public static int indexOf(double[] array, double x) {
		for(int i = 0; i < array.length; i++)
			if(array[i] == x)
				return i;
		return -1;
	}

	public static int indexOf(char[] array, char x) {
		for(int i = 0; i < array.length; i++)
			if(array[i] == x)
				return i;
		return -1;
	}

	public static int indexOf(short[] array, short x) {
		for(int i = 0; i < array.length; i++)
			if(array[i] == x)
				return i;
		return -1;
	}

	public static int indexOf(long[] array, long x) {
		for(int i = 0; i < array.length; i++)
			if(array[i] == x)
				return i;
		return -1;
	}
	
	public static <T> boolean contains(T[] array, T t) {
		return indexOf(array, t) != -1;
	}
	
	public static boolean contains(int[] array, int x) {
		return indexOf(array, x) != -1;
	}
	
	public static boolean contains(float[] array, float x) {
		return indexOf(array, x) != -1;
	}
	
	public static boolean contains(char[] array, char x) {
		return indexOf(array, x) != -1;
	}
	
	public static boolean contains(short[] array, short x) {
		return indexOf(array, x) != -1;
	}
	
	public static boolean contains(long[] array, long x) {
		return indexOf(array, x) != -1;
	}
	
	public static boolean contains(double[] array, double x) {
		return indexOf(array, x) != -1;
	}

	public static <T, R> R accumulate(T[] array, BiFunction<R, T, R> accumulator, R seed) {
		for(T t : array)
			seed = accumulator.apply(seed, t);
		return seed;
	}

	public static <T, R> R accumulate(T[] array, BiFunction<R, T, R> accumulator) {
		return accumulate(array, accumulator, null);
	}
	
	public static <T> T[] generate(int count, IntFunction<T> generator, IntFunction<T[]> arrayGenerator) {
		return IntStream.range(0, count).mapToObj(generator).toArray(arrayGenerator);
	}
	
}
