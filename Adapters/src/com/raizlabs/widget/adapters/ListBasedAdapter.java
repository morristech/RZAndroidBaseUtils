package com.raizlabs.widget.adapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;

import com.raizlabs.threading.ThreadingUtils;
import com.raizlabs.util.observable.lists.ListObserver;
import com.raizlabs.util.observable.lists.ListObserverListener;
import com.raizlabs.util.observable.lists.ObservableList;
import com.raizlabs.util.observable.lists.SimpleListObserver;
import com.raizlabs.util.observable.lists.SimpleListObserverListener;
import com.raizlabs.widget.adapters.viewholderstrategy.ViewHolderStrategyUtils;

/**
 * Common base class implementation of a {@link Adapter} that is backed
 * by a {@link List}. This class also implements {@link List} to support the
 * same functionality, while still functioning as an {@link Adapter}.
 *
 * @param <Item> The type of item that views will represent.
 * @param <Holder> The type of the {@link ViewHolder} that will be used to hold
 * views.
 */
public abstract  class ListBasedAdapter<Item, Holder extends ViewHolder> extends BaseAdapter implements ObservableList<Item> {
	
	private SimpleListObserver<Item> listObserver;
	
	@Override
	public ListObserver<Item> getListObserver() {
		return listObserver;
	}
	
	/**
	 * {@link ListObserverListener} which listens to underlying list changes and calls the appropriate methods.
	 */
	private ListObserverListener<Item> observableListener = new ListObserverListener<Item>() {

		@Override
		public void onItemRangeChanged(ListObserver<Item> observer, int startPosition, int itemCount) {
			listObserver.notifyItemRangeChanged(startPosition, itemCount);
		}

		@Override
		public void onItemRangeInserted(ListObserver<Item> observer, int startPosition, int itemCount) {
			listObserver.notifyItemRangeInserted(startPosition, itemCount);
		}

		@Override
		public void onItemRangeRemoved(ListObserver<Item> observer, int startPosition, int itemCount) {
			listObserver.notifyItemRangeRemoved(startPosition, itemCount);
		}

		@Override
		public void onGenericChange(ListObserver<Item> observer) {
			listObserver.notifyGenericChange();
		}
	};
	
	private List<Item> mList;
	/**
	 * @return The {@link List} of items in this adapter.
	 */
	protected List<Item> getItemsList() { return mList; }
	protected void unbindList() {
		if (mList instanceof ObservableList<?>) {
			((ObservableList<Item>) mList).getListObserver().removeListener(observableListener);
		}
	}
	
	/**
	 * Sets the {@link List} of items in this adapter.
	 * @param list The {@link List} of items to use.
	 */
	protected void setItemsList(List<Item> list) {
		unbindList();
		if (list == null) list = new LinkedList<Item>();
		mList = list;
		notifyDataSetChangedOnUIThread();
	}
	
	/**
	 * Sets the {@link ObservableList} of items in this adapter, and subscribes
	 * to updates.
	 * @param list The {@link ObservableList} of items to use.
	 */
	protected void setItemsList(ObservableList<Item> list) {
		if (list != null) list.getListObserver().addListener(observableListener);
		setItemsList((List<Item>) list);
	}
	
	/**
	 * Constructs an empty {@link ListBasedAdapter}.
	 */
	protected ListBasedAdapter() {
		this(null);
	}
	
	/**
	 * Constructs a {@link ListBasedAdapter} which contains the given list.
	 * @param list The list of items to use.
	 */
	protected ListBasedAdapter(List<Item> list) {
		listObserver = new SimpleListObserver<Item>();
		// Listen to any changes and make sure our super data set changed
		// is called, but not the one which will trigger another internal update.
		listObserver.addListener(new SimpleListObserverListener<Item>() {
			@Override
			public void onGenericChange(ListObserver<Item> observer) {
				superNotifyDataSetChanged();
			}
		});
		setItemsList(list);
	}
	
	/**
	 * Purges any resources from this adapter. Note that this may make the
	 * adapter unusable.
	 */
	public void cleanup() {
		unbindList();
	}
	
	/**
	 * Loads the given {@link List} into this adapter. This will use the same
	 * reference, so any changes to the source list will be reflected by the
	 * adapter whenever the data is repopulated. See
	 * {@link #notifyDataSetChangedOnUIThread()}. 
	 * @param list The {@link List} to load.
	 */
	public void loadItemList(List<Item> list) {
		setItemsList(list);
	}
	
	/**
	 * Loads the given {@link List} into this adapter and subscribes to updates.
	 * This will use the same reference, so any changes to the source list will
	 * be reflected by the adapter whenever the data is repopulated. See
	 * {@link #notifyDataSetChangedOnUIThread()}. 
	 * @param list The {@link ObservableList} to load.
	 */
	public void loadItemList(ObservableList<Item> list) {
		setItemsList(list);
	}
	
	/**
	 * Loads the given items as the contents of this adapter.
	 * @param items The {@link Collection} of items to load.
	 */
	public void loadItems(Collection<Item> items) {
		List<Item> data = new ArrayList<Item>(items.size());
		for (Item item : items) {
			data.add(item);
		}
		setItemsList(data);
	}
	
	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		listObserver.notifyGenericChange();
	}
	
	public void superNotifyDataSetChanged() {
		super.notifyDataSetChanged();
	}
	
	private final Runnable dataSetChangedRunnable = new Runnable() {
		@Override
		public void run() {
			notifyDataSetChanged();
		}
	};
	/**
	 * Calls {@link #notifyDataSetChangedOnUIThread()} on the UI thread.
	 */
	public void notifyDataSetChangedOnUIThread() {
		ThreadingUtils.runOnUIThread(dataSetChangedRunnable);
	}
	
	@Override
	public long getItemId(int position) {
		return 0;
	}
	
	/**
	 * Inflates the given layout resource using the context of the given parent.
	 * Does not add the resources to the parent.
	 * @param parent A parent view where the view will eventually be added. 
	 * @param layoutResId The layout resource ID to inflate.
	 * @return The inflated view.
	 */
	protected View inflateView(ViewGroup parent, int layoutResId) {
		return LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
	}

	/**
	 * Call to populate the given view holder with the data at the given
	 * position in the list.
	 * @param viewHolder The view holder to populate.
	 * @param position The position of the data in the list.
	 */
	public void onBindViewHolder(Holder viewHolder, int position) {
		onBindViewHolder(viewHolder, get(position), position);
	}
	
	/**
	 * Called to populate the given view holder with the data from the given
	 * item. By default, this is called from
	 * {@link #onBindViewHolder(ViewHolder, int)} with the appropriate item.
	 * @param viewHolder The view holder to populate.
	 * @param item The item whose data to populate into the view holder.
	 * @param position The position of the item in the list.
	 */
	protected abstract void onBindViewHolder(Holder viewHolder, Item item, int position);
	
	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		return mList.get(position);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder viewHolder = null;
		if (convertView != null) {
			viewHolder = getViewHolder(convertView);
		}
		
		if (viewHolder == null) {
			int viewType = getItemViewType(position);
			viewHolder = onCreateViewHolder(parent, viewType);
			setViewHolder(viewHolder.itemView, viewHolder);
		}
		
		onBindViewHolder(viewHolder, position);
		
		return viewHolder.itemView;
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		Holder viewHolder = null;
		if (convertView != null) {
			viewHolder = getViewHolder(convertView);
		}
		
		if (viewHolder == null) {
			int viewType = getItemViewType(position);
			viewHolder = onCreateDropDownViewHolder(parent, viewType);
			setViewHolder(viewHolder.itemView, viewHolder);
		}
		
		onBindDropDownViewHolder(viewHolder, position);
		
		return viewHolder.itemView;
	}
	
	@SuppressWarnings("unchecked")
	protected Holder getViewHolder(View view) {
		try {
			return (Holder) ViewHolderStrategyUtils.getViewHolder(view);
		} catch (ClassCastException ex) {
			// Don't care. Just don't crash. We'll just ignore convertView.
		}
		
		return null;
	}
	
	protected void setViewHolder(View view, Holder holder) {
		ViewHolderStrategyUtils.setViewHolder(view, holder);
	}
	
	public abstract Holder onCreateViewHolder(ViewGroup parent, int itemType);
	
	public Holder onCreateDropDownViewHolder(ViewGroup parent, int itemType) {
		return onCreateViewHolder(parent, itemType);
	}
	
	public void onBindDropDownViewHolder(Holder viewHolder, int position) {
		onBindViewHolder(viewHolder, position);
	}

	@Override
	public void add(int location, Item object) {
		mList.add(location, object);
		listObserver.notifyItemRangeInserted(location, 1);
	}

	@Override
	public boolean add(Item object) {
		int location = mList.size();
		final boolean result = mList.add(object);
		listObserver.notifyItemRangeInserted(location, 1);
		return result;
	}

	@Override
	public boolean addAll(int location, Collection<? extends Item> collection) {
		if (mList.addAll(location, collection)) {
			listObserver.notifyItemRangeInserted(location, collection.size());
			return true;
		}
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends Item> collection) {
		int location = mList.size();
		if (mList.addAll(collection)) {
			listObserver.notifyItemRangeInserted(location, collection.size());
			return true;
		}
		return false;
	}

	@Override
	public void clear() {
		mList.clear();
		listObserver.notifyGenericChange();
	}

	@Override
	public boolean contains(Object object) {
		return mList.contains(object);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		return mList.containsAll(collection);
	}

	@Override
	public Item get(int location) {
		return mList.get(location);
	}

	@Override
	public int indexOf(Object object) {
		return mList.indexOf(object);
	}

	@Override
	public Iterator<Item> iterator() {
		return mList.iterator();
	}

	@Override
	public int lastIndexOf(Object object) {
		return mList.lastIndexOf(object);
	}

	@Override
	public ListIterator<Item> listIterator() {
		return mList.listIterator();
	}

	@Override
	public ListIterator<Item> listIterator(int location) {
		return mList.listIterator();
	}

	@Override
	public Item remove(int location) {
		Item result = mList.remove(location);
		listObserver.notifyItemRangeRemoved(location, 1);
		return result;
	}

	@Override
	public boolean remove(Object object) {
		int location = mList.indexOf(object);
		if (location >= 0) {
			mList.remove(location);
			listObserver.notifyItemRangeRemoved(location, 1);
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		boolean result = mList.removeAll(collection);
		if (result) {
			listObserver.notifyGenericChange();
		}
		return result;
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		boolean result = mList.retainAll(collection);
		if (result) {
			listObserver.notifyGenericChange();
		}
		return result;
	}

	@Override
	public Item set(int location, Item object) {
		Item result = mList.set(location, object);
		if (!result.equals(object)) {
			listObserver.notifyItemRangeChanged(location, 1);
		}
		return result;
	}

	@Override
	public int size() {
		return mList.size();
	}

	@Override
	public List<Item> subList(int start, int end) {
		return mList.subList(start, end);
	}

	@Override
	public Object[] toArray() {
		return mList.toArray();
	}

	@Override
	public <S> S[] toArray(S[] array) {
		return mList.toArray(array);
	}
}