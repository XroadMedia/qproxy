package net.e175.klaus.config;

import java.util.NoSuchElementException;

/**
 * Value implements {@link ConfigValue} and holds values read from config,
 * even nonexistent ones (similar to a Null Object).
 * 
 * This class is immutable and thus thread-safe.
 */
public final class Value implements ConfigValue {

	private final boolean exists;
	private final String internalValue;
	private final Location fromLocation;

	Value(final String internalValue, final Location fromLocation) {
		this.exists = internalValue != null;
		this.internalValue = internalValue;
		this.fromLocation = fromLocation != null ? fromLocation : Location.getNonexistentLocation();
	}

	/** {@inheritDoc} */
	public boolean exists() {
		return exists;
	}

	/** {@inheritDoc} */
	public String asString() {
		if (exists) {
			return internalValue;
		} else {
			throw new NoSuchElementException("value does not exist");
		}
	}

	/** {@inheritDoc} */
	public String asString(final String orDefault) {
		return exists ? asString() : orDefault;
	}

	/** {@inheritDoc} */
	public double asDouble() {
		return Double.parseDouble(asString());
	}

	/** {@inheritDoc} */
	public double asDouble(final double orDefault) {
		return exists ? asDouble() : orDefault;
	}

	/** {@inheritDoc} */
	public long asLong() {
		return Long.parseLong(asString());
	}

	/** {@inheritDoc} */
	public long asLong(final long orDefault) {
		return exists ? asLong() : orDefault;
	}
	
	/** {@inheritDoc} */
	public boolean isTrue() {
		return "true".equalsIgnoreCase(asString()) || "yes".equalsIgnoreCase(asString());
	}	
	
	/** {@inheritDoc} */	
	public boolean isTrue(boolean orDefault) {
		return exists ? isTrue() : orDefault;
	}	

	/** {@inheritDoc} */
	public Location loadedFrom() {
		return fromLocation;
	}

	@Override
	public String toString() {
		return "Value{" + "exists=" + exists + ", internalValue=" + internalValue + ", fromLocation=" + fromLocation + '}';
	}



}
