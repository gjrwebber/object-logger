/**
 * TimestampedObjectSet.java (c) Copyright 2013 Graham Webber
 */
package org.gw.objectlogger;

import org.gw.commons.utils.DateUtil;

import java.util.*;

/**
 * Encapsulates a Set of {@link TimestampedObject} data. THe backing data
 * structure is a {@link java.util.TreeMap} with the key being the <code>logDate</code> of
 * the {@link TimestampedObject} rounded down to the minute and the value being
 * a {@link java.util.List} of {@link TimestampedObject}s that were logged at that exact
 * logDate.
 * <p>
 * Note: This is not a thread safe class.
 * 
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public class TimestampedObjectSet<T> {

	/**
	 * A {@link java.util.TreeMap} holding the {@link TimestampedObject}s using the log
	 * {@link java.util.Date} of the {@link TimestampedObject} rounded down to the nearest
	 * minute as the key.
	 */
	private TreeMap<Date, List<TimestampedObject<T>>> map = new TreeMap<Date, List<TimestampedObject<T>>>();

	/**
	 * Instance variable holding the size of the set.
	 */
	private int size;

	private final Comparable<T> any = new Comparable<T>() {
		@Override
		public int compareTo(T o) {
			return 0;
		}
	};

	/**
	 * Returns the internal data structure.
	 * 
	 * @return Returns the internal data structure.
	 */
	public TreeMap<Date, List<TimestampedObject<T>>> asMap() {
		return map;
	}

	/**
	 * Returns the T at or closest before the given {@link java.util.Date}.
	 * 
	 * @param date
	 *            The latest {@link java.util.Date} to find a T
	 * @return The T at or closest before the given {@link java.util.Date}.
	 */
	public T getForDate(Date date) {
		return getForDate(date, any);
	}

	/**
	 * Returns the T that matched the given {@link Comparable} at or closest
	 * before the given {@link java.util.Date}.
	 * 
	 * @param date
	 *            The latest {@link java.util.Date} to find the {@link Comparable} T
	 * @param comparable
	 *            The {@link Comparable} to find the T
	 * @return The T that matches the {@link Comparable} at or closest before
	 *         the given {@link java.util.Date}.
	 */
	public T getForDate(Date date, Comparable<T> comparable) {

		for (Date logDate : map.descendingKeySet()) {
			if (logDate.after(date)) {
				continue;
			}
			List<TimestampedObject<T>> logged = map.get(logDate);
			for (int i = logged.size() - 1; i >= 0; i--) {
				TimestampedObject<T> serialisable = logged.get(i);
				T obj = serialisable.getObj();
				if (comparable.compareTo(obj) == 0) {
					return obj;
				}
			}
		}
		return null;
	}

	/**
	 * Returns a {@link java.util.TreeMap} containing only the unique
	 * {@link TimestampedObject}s given by the {@link java.util.Comparator}.
	 * 
	 * @param comparator
	 * @return
	 */
	public TreeMap<Date, Set<TimestampedObject<T>>> asUniqueMap(
			Comparator<TimestampedObject<T>> comparator) {
		TreeMap<Date, Set<TimestampedObject<T>>> uniqueMap = new TreeMap<Date, Set<TimestampedObject<T>>>();
		for (Date date : map.keySet()) {
			uniqueMap.put(date, getUniqueForDate(date, comparator));
		}
		return uniqueMap;
	}

	/**
	 * Returns the first {@link TimestampedObject} after the given {@link java.util.Date}
	 * that matches the given <code>example</code> using the given
	 * {@link java.util.Comparator}.
	 * 
	 * @param date
	 *            The {@link java.util.Date} from which to start the search
	 * @param example
	 *            The example &lt;T&gt; to match against
	 * @param comparator
	 *            The {@link java.util.Comparator} to use to match. If the
	 *            {@link java.util.Comparator}s compare method returns 0, it is a match.
	 * @return Returns the first {@link TimestampedObject} after the given
	 *         {@link java.util.Date} that matches the given <code>example</code> using
	 *         the given {@link java.util.Comparator}.
	 */
	public TimestampedObject<T> getExampleAfter(Date date, T example,
			Comparator<T> comparator) {

		Date roundedDate = DateUtil.roundUpToMinute(date);
		for (Date logDate : map.keySet()) {
			if (logDate.before(roundedDate)) {
				continue;
			}
			List<TimestampedObject<T>> logged = map.get(logDate);
			for (TimestampedObject<T> serialised : logged) {
				if (comparator.compare(example, serialised.getObj()) == 0) {
					return serialised;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the first {@link TimestampedObject} after the given {@link java.util.Date}
	 * that matches the given <code>example</code> using the given
	 * {@link java.util.Comparator}.
	 * 
	 * @param from
	 *            The {@link java.util.Date} from which to start the search
	 * @param to
	 *            The {@link java.util.Date} to end the search
	 * @param example
	 *            The example &lt;T&gt; to match against
	 * @param comparator
	 *            The {@link java.util.Comparator} to use to match. If the
	 *            {@link java.util.Comparator}s compare method returns 0, it is a match.
	 * @return Returns the first {@link TimestampedObject} after the given
	 *         {@link java.util.Date} that matches the given <code>example</code> using
	 *         the given {@link java.util.Comparator}.
	 */
	public Set<TimestampedObject<T>> getExamples(Date from, Date to, T example,
			Comparator<T> comparator) {

		Set<TimestampedObject<T>> result = new HashSet<TimestampedObject<T>>();
		Date roundedFromDate = DateUtil.roundDownToMinute(from);
		Date roundedToDate = DateUtil.roundUpToMinute(to);
		for (Date logDate : map.keySet()) {
			if (logDate.after(roundedToDate)) {
				System.out.println("Log Date "+logDate+" after rounded to date: "+roundedToDate);
				break;
			}
			if (logDate.before(roundedFromDate)) {
				System.out.println("Log Date "+logDate+" before rounded from date: "+roundedFromDate);
				continue;
			}
			List<TimestampedObject<T>> logged = map.get(logDate);
			for (TimestampedObject<T> serialised : logged) {
				if (comparator.compare(example, serialised.getObj()) == 0) {
					result.add(serialised);
				}
			}
		}
		return result;
	}

	/**
	 * Returns a unique {@link java.util.Set} of &lt;T&gt;s using the given
	 * {@link java.util.Comparator} that were logged between the given {@link java.util.Date}s.
	 * 
	 * @param from
	 *            The earliest Date the unique &lt;T&gt; was logged.
	 * @param from
	 *            The latest Date the unique &lt;T&gt; was logged.
	 * @param comparator
	 *            A {@link java.util.Comparator} which determines the uniqueness of the
	 *            &lt;T&gt;. ie. The <code>compareTo</code> method needs to
	 *            return 0 for equal &lt;T&gt;s.
	 * @return a unique {@link java.util.Set} of &lt;T&gt;s using the given
	 *         {@link java.util.Comparator} that were logged between the given
	 *         {@link java.util.Date}s .
	 */
	public Set<TimestampedObject<T>> getUniqueBetweenDates(Date from, Date to,
			Comparator<TimestampedObject<T>> comparator) {
		TreeSet<TimestampedObject<T>> set = new TreeSet<TimestampedObject<T>>(
				comparator);
		for (Date logDate : map.descendingKeySet()) {
			if (logDate.after(to)) {
				continue;
			}
			if (logDate.before(from)) {
				break;
			}
			List<TimestampedObject<T>> logged = map.get(logDate);
			for (TimestampedObject<T> serialised : logged) {
				if (!set.contains(serialised)) {
					set.add(serialised);
				}
			}
		}
		return set;
	}

	/**
	 * Returns a unique {@link java.util.Set} of &lt;T&gt;s using the given
	 * {@link java.util.Comparator} that were logged before the given {@link java.util.Date}.
	 * 
	 * @param date
	 *            The latest Date the unique &lt;T&gt; was logged.
	 * @param comparator
	 *            A {@link java.util.Comparator} which determines the uniqueness of the
	 *            &lt;T&gt;. ie. The <code>compareTo</code> method needs to
	 *            return 0 for equal &lt;T&gt;s.
	 * @return a unique {@link java.util.Set} of &lt;T&gt;s using the given
	 *         {@link java.util.Comparator} that were logged before the given {@link java.util.Date}
	 *         .
	 */
	public Set<TimestampedObject<T>> getUniqueForDate(Date date,
			Comparator<TimestampedObject<T>> comparator) {
		TreeSet<TimestampedObject<T>> set = new TreeSet<TimestampedObject<T>>(
				comparator);
		for (Date logDate : map.descendingKeySet()) {
			if (logDate.after(date)) {
				continue;
			}
			List<TimestampedObject<T>> logged = map.get(logDate);
			for (TimestampedObject<T> serialised : logged) {
				if (!set.contains(serialised)) {
					set.add(serialised);
				}
			}
		}
		return set;
	}

	/**
	 * @return an ordered {@link java.util.List} of {@link TimestampedObject}s. Oldest to
	 *         newest.
	 */
	public List<TimestampedObject<T>> asTimestampedList() {
		List<TimestampedObject<T>> list = new ArrayList<TimestampedObject<T>>();
		for (Date date : map.keySet()) {
			List<TimestampedObject<T>> forDate = map.get(date);
			for (TimestampedObject<T> logged : forDate) {
				list.add(logged);
			}
		}
		return Collections.unmodifiableList(list);
	}

	/**
	 * 
	 * @return an ordered {@link java.util.List} of &lt;T&gt;s. Oldest to newest.
	 */
	public List<T> asList() {
		List<T> list = new ArrayList<T>();
		for (Date date : map.keySet()) {
			List<TimestampedObject<T>> forDate = map.get(date);
			for (TimestampedObject<T> logged : forDate) {
				list.add(logged.getObj());
			}
		}
		return Collections.unmodifiableList(list);
	}

	/**
	 * Adds the given {@link TimestampedObject} to the
	 * {@link TimestampedObjectSet}.
	 * 
	 * @param obj
	 *            The {@link TimestampedObject} to add.
	 */
	public void add(TimestampedObject<T> obj) {
		if (obj == null || obj.getObj() == null) {
			return;
		}
		Date logDate = obj.getLogTime();
		Date roundedDate = DateUtil.roundDownToMinute(logDate);
		List<TimestampedObject<T>> set = map.get(roundedDate);
		if (set == null) {
			set = new ArrayList<TimestampedObject<T>>();
			map.put(roundedDate, set);
		}
		set.add(obj);
		size++;
	}

	/**
	 * Adds all given {@link TimestampedObject}s to the
	 * {@link TimestampedObjectSet}.
	 * 
	 * @param c
	 *            A {@link java.util.Collection} of {@link TimestampedObject}s to add
	 */
	public void addAll(Collection<? extends TimestampedObject<T>> c) {
		for (TimestampedObject<T> serialisable : c) {
			add(serialisable);
		}
	}

	/**
	 * Add the given {@link TimestampedObjectSet} to this
	 * {@link TimestampedObjectSet}.
	 * 
	 * @param set
	 *            The {@link TimestampedObjectSet} to add to this
	 *            {@link TimestampedObjectSet}.
	 */
	public void addAll(TimestampedObjectSet<T> set) {
		for (TimestampedObject<T> serialisable : set.asTimestampedList()) {
			add(serialisable);
		}
	}

	/**
	 * Clear this {@link TimestampedObjectSet}.
	 */
	public void clear() {
		map.clear();
		size = 0;
	}

	/**
	 * Returns true if the {@link TimestampedObjectSet} is empty, false
	 * otherwise.
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Returns the size of this {@link TimestampedObjectSet}.
	 * 
	 * @return
	 */
	public int size() {
		return size;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TimestampedObjectSet [map=");
		builder.append(map);
		builder.append(", size=");
		builder.append(size);
		builder.append("]");
		return builder.toString();
	}

}
