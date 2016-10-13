package com.stc.radio.player;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.IItemAdapter;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter_extensions.drag.ItemTouchCallback;
import com.mikepenz.fastadapter_extensions.drag.SimpleDragCallback;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.Station;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

import static junit.framework.Assert.assertNotNull;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class ListFragment extends Fragment implements ItemAdapter.ItemFilterListener, ItemTouchCallback
{

	private OnListFragmentInteractionListener mListener;
	private FastItemAdapter<StationListItem> fastItemAdapter;
	private RecyclerView recyclerView;
	private SimpleDragCallback touchCallback;
	private ItemTouchHelper touchHelper;
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ListFragment() {

	}

	public static ListFragment newInstance() {
		return new ListFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	/*@Subscribe()
	public void onPrevNext(int or) {
		if(or>0){
			Timber.d("onNext");
			selectNextItem();
		}else {
			Timber.d("onPrev");
			selectPrevItem();
		}
	}*/
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		setRetainInstance(true);

		View view = inflater.inflate(R.layout.fragment_recycler, container, false);

		// Set the adapter
		if (view instanceof RecyclerView) {
			Context context = view.getContext();
			recyclerView = (RecyclerView) view;
				recyclerView.setLayoutManager(new LinearLayoutManager(context));
			recyclerView.setLayoutManager(new LinearLayoutManager(context));
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			//final FastScrollIndicatorAdapter<StationListItem> fastScrollIndicatorAdapter = new FastScrollIndicatorAdapter<>();

			fastItemAdapter=initFastAdapter();
			recyclerView.setAdapter(fastItemAdapter);
			//DragScrollBar materialScrollBar = new DragScrollBar(context, recyclerView, true);
			//materialScrollBar.addIndicator(new AlphabetIndicator(context), true);
			touchCallback = new SimpleDragCallback(this);
			touchHelper = new ItemTouchHelper(touchCallback);
			touchHelper.attachToRecyclerView(recyclerView);
			fastItemAdapter.notifyAdapterDataSetChanged();
			//EventBus.getDefault().post(fastItemAdapter);
		}
		return view;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);


	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);


	}
	public FastItemAdapter<StationListItem> getAdapter() {
		if(fastItemAdapter==null) fastItemAdapter=initFastAdapter();
		assertNotNull(fastItemAdapter);
		return fastItemAdapter;
	}

	private FastItemAdapter<StationListItem> initFastAdapter(){

		fastItemAdapter = new FastItemAdapter<>();
		fastItemAdapter.withSelectable(true);
		fastItemAdapter.withMultiSelect(false);
		fastItemAdapter.withOnClickListener(listItemOnClickListener);
		fastItemAdapter.withFilterPredicate(new IItemAdapter.Predicate() {
			@Override
			public boolean filter(IItem item, CharSequence constraint) {
				return !((StationListItem)item).station.getName().toLowerCase()
						.contains(constraint.toString().toLowerCase());
			}
		});
		fastItemAdapter.getItemAdapter().withItemFilterListener(this);
		return fastItemAdapter;
		/*if(getActionBar()!=null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setHomeButtonEnabled(false);
		}*/
	}
	public void replaceAdapter(List <Station> list){
		Timber.w("newList %d", list.size());
		fastItemAdapter.clear();
		fastItemAdapter.notifyAdapterDataSetChanged();
		DbHelper.resetActiveStations();
		for(Station s: list) {
			s.setActive(true);
			StationListItem stationListItem = new StationListItem()
					.withIdentifier(s.getId())
					.withStation(s);
			fastItemAdapter.add(stationListItem);
			s.setPosition(fastItemAdapter.getAdapterPosition(stationListItem));
			s.save();
		}
		fastItemAdapter.notifyAdapterDataSetChanged();
	}
	@Override
	public void itemsFiltered() {
		Timber.v("filtered items count: %d", fastItemAdapter.getItemCount() );
		Toast.makeText(getActivity(), "filtered items count: " + fastItemAdapter.getItemCount(), Toast.LENGTH_SHORT).show();
	}
	@Override
	public boolean itemTouchOnMove(int oldPosition, int newPosition) {
		StationListItem item = (StationListItem) fastItemAdapter.getAdapterItem(oldPosition);
		item.getStation().setPosition(newPosition);
		item.getStation().save();
		item =(StationListItem) fastItemAdapter.getAdapterItem(newPosition);
		item.getStation().setPosition(oldPosition);
		item.getStation().save();
		Collections.swap(fastItemAdapter.getAdapterItems(), oldPosition, newPosition); // change position
		fastItemAdapter.notifyAdapterItemMoved(oldPosition, newPosition);
		return true;
	}


	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnListFragmentInteractionListener) {
			mListener = (OnListFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnListFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	public void filter(String s) {
		if(fastItemAdapter!=null){
			fastItemAdapter.filter(s);
		}
	}


	public StationListItem getSelectedItem() {
		if(fastItemAdapter==null){
			Timber.d("item=null");
			return null;
		}
		Set list = fastItemAdapter.getSelectedItems();
		//if(list!=null)Timber.w("selected listsize=%d list:%s",list.size(), list.toString());
		StationListItem item=null;
		if(list!=null && !list.isEmpty()) item=(StationListItem)list.iterator().next();
		if(item!=null)Timber.d("item=%s", item.station.getName());
		else Timber.d("item=null");
		return  item;
	}
	public FastItemAdapter.OnClickListener<StationListItem> listItemOnClickListener=new FastAdapter.OnClickListener<StationListItem>() {
		@Override
		public boolean onClick(View v, IAdapter<StationListItem> adapter, StationListItem item, int position) {
			Timber.v("item %s",item.station.getName());
			adapter.getFastAdapter().deselect();
			adapter.getFastAdapter().select(position,false);
			//fastItemAdapter.set(position, item.withSetSelected(true));
			//fastItemAdapter.notifyAdapterItemChanged(position);
			mListener.onListFragmentInteraction(item);
			return true;
		}
	};/*
	public void restoreState(int position){
		if(position>-1 && fastItemAdapter!=null){
			StationListItem item = getSelectedItem();
			if(item !=null && fastItemAdapter.getAdapterPosition(item)!=position)
				fastItemAdapter.deselect();
			fastItemAdapter.select(position, false);
			if(recyclerView!=null) recyclerView.smoothScrollToPosition(position);
		}
	}
	public void selectNextItem() {
		StationListItem item=null;
		int pos=-1;
		List list=  fastItemAdapter.getAdapterItems();
		assertNotNull(list);
		assertFalse(list.isEmpty());
		if(!DbHelper.isShuffle()) {
			StationListItem oldSelection = getSelectedItem();
			Iterator iterator = list.iterator();
			while (iterator.hasNext()) {
				item = (StationListItem) iterator.next();
				if (item.getStation().getName().equals(oldSelection.getStation().getName())) {
					if (iterator.hasNext()) item = (StationListItem) iterator.next();
					break;
				}
			}
			assertNotNull(item);
			pos=fastItemAdapter.getAdapterPosition(item);
		}else pos=new Random().nextInt(fastItemAdapter.getAdapterItemCount()-1);
		assertTrue(pos>=0);
		Timber.d("newpos %d", pos);
		fastItemAdapter.deselect();
		fastItemAdapter.select(pos, true);
		if(getSelectedItem()!=null) DbHelper.setUrl(getSelectedItem().getStation().url);
		if(recyclerView!=null) recyclerView.smoothScrollToPosition(pos);
	}

	public void selectPrevItem() {
		StationListItem item=null;
		List list=  fastItemAdapter.getAdapterItems();
		assertNotNull(list);
		assertFalse(list.isEmpty());
		StationListItem oldSelection = getSelectedItem();

		Iterator iterator = list.iterator();
		item = (StationListItem) iterator.next();
		while (iterator.hasNext()) {
			StationListItem tempitem = (StationListItem) iterator.next();
			if (tempitem.getStation().getName().equals(oldSelection.getStation().getName())) {
				break;
			}
			item=tempitem;
		}
		assertNotNull(item);
		int pos=fastItemAdapter.getAdapterPosition(item);
		assertTrue(pos>=0);
		Timber.d("newpos %d", pos);
		fastItemAdapter.deselect();
		fastItemAdapter.select(pos, true);
		if(getSelectedItem()!=null) DbHelper.setUrl(getSelectedItem().getStation().url);
		if(recyclerView!=null) recyclerView.smoothScrollToPosition(pos);

		*//*if(oldSelection!=null) {
			int oldpos=fastItemAdapter.getAdapterPosition(oldSelection);
			if(oldpos>0) {
				fastItemAdapter.set(oldpos, oldSelection);
				fastItemAdapter.notifyAdapterItemChanged(oldpos);
			}
		}*//*
	}*/


	public void updateSelection(Station station) {

		if(station!=null && fastItemAdapter!=null){
			Timber.w("station %s,pos=%d,", station.getName(),station.getPosition());

			StationListItem item = getSelectedItem();
			if(item !=null && item.station.getPosition()!=station.getPosition()) {
				fastItemAdapter.deselect();
				fastItemAdapter.select(station.getPosition(), false);
			}

			if(recyclerView!=null && station.getPosition()>=0) recyclerView.smoothScrollToPosition(station.getPosition());
		}
	}

	public interface OnListFragmentInteractionListener {
		// TODO: Update argument type and name
		void onListFragmentInteraction(StationListItem item);
	}
}
