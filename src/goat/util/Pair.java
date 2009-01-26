package goat.util;

public class Pair<T> {
	private T first;
	private T second;
	
	public Pair(T first, T second) {
		this.first = first;
		this.second = second;
	}
	
	public boolean equals(Pair<T> pair) {
		return first.equals(pair.getFirst()) && second.equals(pair.getSecond());
	}

	public T getFirst() {
		return first;
	}

	public void setFirst(T first) {
		this.first = first;
	}

	public T getSecond() {
		return second;
	}

	public void setSecond(T second) {
		this.second = second;
	}
}
