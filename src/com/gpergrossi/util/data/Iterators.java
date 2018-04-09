package com.gpergrossi.util.data;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class Iterators {

	public static <From, To> Iterator<To> cast(Iterator<From> iterator, Function<? super From, ? extends To> caster) {
		return new IteratorCaster<>(iterator, caster);
	}
	
	private static class IteratorCaster<From, To> implements Iterator<To> {
		private Iterator<From> iterator;
		private Function<? super From, ? extends To> caster;
		
		public IteratorCaster(Iterator<From> iterator, Function<? super From, ? extends To> caster) {
			this.iterator = iterator;
			this.caster = caster;
		}
		
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public To next() {
			return caster.apply(iterator.next());
		}
		
		@Override
		public void remove() {
			iterator.remove();
		}
	}
	
	public static <From, To> Iterable<To> cast(Iterable<From> iterable, Function<? super From, ? extends To> caster) {
		return new IterableCaster<>(iterable, caster);
	}
	
	private static class IterableCaster<From, To> implements Iterable<To> {
		private Iterable<From> iterable;
		private Function<? super From, ? extends To> caster;
		
		public IterableCaster(Iterable<From> iterable, Function<? super From, ? extends To> caster) {
			this.iterable = iterable;
			this.caster = caster;
		}

		@Override
		public Iterator<To> iterator() {
			return new IteratorCaster<>(iterable.iterator(), caster);
		}
	}
	
	public static <T> Iterator<T> unwrap(Iterable<? extends Iterable<T>> iterable) {
		return new IteratorIterator<>(iterable.iterator(), t -> t.iterator());
	}
	
	public static <T> Iterator<T> unwrap(Iterator<? extends Iterable<T>> iterator) {
		return new IteratorIterator<>(iterator, t -> t.iterator());
	}
	
	public static <From, To> Iterator<To> unwrap(Iterator<From> iterator, Function<? super From, ? extends Iterator<To>> extractor) {
		return new IteratorIterator<>(iterator, extractor);
	}
	
	private static class IteratorIterator<From, To> implements Iterator<To> {
		final Iterator<From> iterator;
		final Function<? super From, ? extends Iterator<To>> extractor;
		private Iterator<To> currentIterator;
		private Iterator<To> lastIterator;
		
		public IteratorIterator(Iterator<From> iterator, Function<? super From, ? extends Iterator<To>> extractor) {
			this.iterator = iterator;
			this.extractor = extractor;
		}
		
		@Override
		public boolean hasNext() {
			if (currentIterator != null && currentIterator.hasNext()) return true;
			while (iterator.hasNext()) {
				currentIterator = extractor.apply(iterator.next());
				if (currentIterator.hasNext()) return true;
			}
			return false;
		}

		@Override
		public To next() {
			if (!hasNext()) throw new NoSuchElementException();
			lastIterator = currentIterator;
			return currentIterator.next();
		}
		
		@Override
		public void remove() {
			if (lastIterator == null) throw new IllegalStateException();
			lastIterator.remove();
			lastIterator = null;
		};
	}
	
	
}
