
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Pools;

import java.util.Iterator;

/** Manages selected objects. Optionally fires a {@link ChangeEvent} on an actor. Selection changes can be vetoed via
 * {@link ChangeEvent#cancel()}.
 * @author Nathan Sweet */
public class Selection<T> implements Disableable, Iterable<T> {
	static boolean isMac = System.getProperty("os.name").contains("Mac");

	private Actor actor;
	final ObjectSet<T> selected = new ObjectSet();
	private final ObjectSet<T> old = new ObjectSet();
	boolean isDisabled;
	private boolean toggle;
	boolean multiple;
	private boolean required;
	private boolean programmaticChangeEvents = true;
	T lastSelected;

	/** @param actor An actor to fire {@link ChangeEvent} on when the selection changes, or null. */
	public void setActor (Actor actor) {
		this.actor = actor;
	}

	/** Selects or deselects the specified item based on how the selection is configured, whether ctrl is currently pressed, etc.
	 * This is typically invoked by user interaction. */
	public void choose (T item) {
		if (isDisabled) return;
		snapshot();
		if ((toggle || (!required && selected.size == 1) || isCtrlPressed()) && selected.contains(item)) {
			if (required && selected.size == 1) return;
			selected.remove(item);
			lastSelected = null;
		} else {
			boolean modified = false;
			if (!multiple || (!toggle && !isCtrlPressed())) {
				modified = selected.size > 0;
				selected.clear();
			}
			if (!selected.add(item) && !modified) return;
			lastSelected = item;
		}
		if (fireChangeEvent()) revert();
		cleanup();
	}

	public boolean hasItems () {
		return selected.size > 0;
	}

	public boolean isEmpty () {
		return selected.size == 0;
	}

	public int size () {
		return selected.size;
	}

	public ObjectSet<T> items () {
		return selected;
	}

	/** Returns the first selected item, or null. */
	public T first () {
		return selected.size == 0 ? null : selected.first();
	}

	void snapshot () {
		old.clear();
		old.addAll(selected);
	}

	void revert () {
		selected.clear();
		selected.addAll(old);
	}

	void cleanup () {
		old.clear(32);
	}

	/** Sets the selection to only the item if found, else clears the selection. */
	public void set (T item) {
		if (selected.size == 1 && selected.first() == item) return;
		snapshot();
		selected.clear();
		selected.add(item);
		if (programmaticChangeEvents && fireChangeEvent())
			revert();
		else
			lastSelected = item;
		cleanup();
	}

	public void setAll (Array<T> items) {
		boolean added = false;
		snapshot();
		selected.clear();
		for (int i = 0, n = items.size; i < n; i++) {
			T item = items.get(i);
			if (selected.add(item)) added = true;
		}
		if (added && programmaticChangeEvents && fireChangeEvent())
			revert();
		else
			lastSelected = items.peek();
		cleanup();
	}

	/** Adds the item to the selection. */
	public void add (T item) {
		if (!selected.add(item)) return;
		if (programmaticChangeEvents && fireChangeEvent())
			selected.remove(item);
		else
			lastSelected = item;
	}

	public void addAll (Array<T> items) {
		boolean added = false;
		snapshot();
		for (int i = 0, n = items.size; i < n; i++) {
			T item = items.get(i);
			if (selected.add(item)) added = true;
		}
		if (added && programmaticChangeEvents && fireChangeEvent())
			revert();
		else
			lastSelected = items.peek();
		cleanup();
	}

	public void remove (T item) {
		if (!selected.remove(item)) return;
		if (programmaticChangeEvents && fireChangeEvent())
			selected.add(item);
		else
			lastSelected = null;
	}

	public void removeAll (Array<T> items) {
		boolean removed = false;
		snapshot();
		for (int i = 0, n = items.size; i < n; i++) {
			T item = items.get(i);
			if (selected.remove(item)) removed = true;
		}
		if (removed && programmaticChangeEvents && fireChangeEvent())
			revert();
		else
			lastSelected = null;
		cleanup();
	}

	public void clear () {
		if (selected.size == 0) return;
		snapshot();
		selected.clear();
		if (programmaticChangeEvents && fireChangeEvent())
			revert();
		else
			lastSelected = null;
		cleanup();
	}

	/** Called when the selection changes.
	 * @return true if the change should be undone. */
	public boolean fireChangeEvent () {
		ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
		try {
			return actor.fire(changeEvent);
		} finally {
			Pools.free(changeEvent);
		}
	}

	public boolean contains (T item) {
		return selected.contains(item);
	}

	/** Makes a best effort to return the last item selected, else returns an arbitrary item or null if the selection is empty. */
	public T getLastSelected () {
		return lastSelected != null ? lastSelected : selected.first();
	}

	public Iterator<T> iterator () {
		return selected.iterator();
	}

	public Array<T> toArray () {
		return selected.iterator().toArray();
	}

	/** If true, prevents {@link #choose(Object)} from changing the selection. Default is false. */
	public void setDisabled (boolean isDisabled) {
		this.isDisabled = isDisabled;
	}

	public boolean isDisabled () {
		return isDisabled;
	}

	public boolean getToggle () {
		return toggle;
	}

	/** If true, prevents {@link #choose(Object)} from clearing the selection. Default is false. */
	public void setToggle (boolean toggle) {
		this.toggle = toggle;
	}

	public boolean getMultiple () {
		return multiple;
	}

	/** If true, prevents {@link #choose(Object)} from selecting multiple. Default is false. */
	public void setMultiple (boolean multiple) {
		this.multiple = multiple;
	}

	public boolean getRequired () {
		return required;
	}

	/** If true, prevents {@link #choose(Object)} from selecting none. Default is false. */
	public void setRequired (boolean required) {
		this.required = required;
	}

	/** If false, only {@link #choose(Object)} will fire a change event. Default is true. */
	public void setProgrammaticChangeEvents (boolean programmaticChangeEvents) {
		this.programmaticChangeEvents = programmaticChangeEvents;
	}

	/** Returns true if ctrl is currently pressed, except on Mac OS X where it returns true if command is currently pressed. */
	static public boolean isCtrlPressed () {
		if (isMac)
			return Gdx.input.isKeyPressed(Keys.SYM);
		else
			return Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
	}
}
