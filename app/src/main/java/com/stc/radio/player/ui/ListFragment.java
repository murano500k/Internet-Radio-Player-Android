package com.stc.radio.player.ui;

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

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.IItemAdapter;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter_extensions.drag.ItemTouchCallback;
import com.mikepenz.fastadapter_extensions.drag.SimpleDragCallback;
import com.stc.radio.player.R;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.NowPlaying;
import com.stc.radio.player.db.Station;

import org.greenrobot.eventbus.Subscribe;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import timber.log.Timber;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class ListFragment extends Fragment implements ItemAdapter.ItemFilterListener, ItemTouchCallback
{

	private static final String ARG_PLAYLIST_ID = "com.stc.radio.player.ui.ARG_PLAYLIST_ID";
	private long playlistId;
	private OnListFragmentInteractionListener mListener;
	private FastItemAdapter fastItemAdapter;
	private RecyclerView recyclerView;
	private SimpleDragCallback touchCallback;
	private ItemTouchHelper touchHelper;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ListFragment() {
	}

	public static ListFragment newInstance(long activePlaylistId) {
		ListFragment fragment = new ListFragment();
		Bundle args = new Bundle();
		args.putLong(ARG_PLAYLIST_ID, activePlaylistId);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			playlistId = getArguments().getLong(ARG_PLAYLIST_ID);
		} else {
			
		}
	}
	@Subscribe()
	public void onPrevNext(int or) {
		if(or>0){
			Timber.d("onNext");
			selectNextItem();
		}else {
			Timber.d("onPrev");
			selectPrevItem();
		}
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_recycler, container, false);

		// Set the adapter
		if (view instanceof RecyclerView) {
			Context context = view.getContext();
			recyclerView = (RecyclerView) view;
				recyclerView.setLayoutManager(new LinearLayoutManager(context));
			recyclerView.setLayoutManager(new LinearLayoutManager(context));
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			//final FastScrollIndicatorAdapter<StationListItem> fastScrollIndicatorAdapter = new FastScrollIndicatorAdapter<>();
			fastItemAdapter=initFastAdapter(savedInstanceState, playlistId);

			recyclerView.setAdapter(fastItemAdapter);
			//DragScrollBar materialScrollBar = new DragScrollBar(context, recyclerView, true);
			//materialScrollBar.addIndicator(new AlphabetIndicator(context), true);
			touchCallback = new SimpleDragCallback(this);
			touchHelper = new ItemTouchHelper(touchCallback);
			touchHelper.attachToRecyclerView(recyclerView);
		}
		return view;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null) {
			//Restore the fragment's state here
		}
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		NowPlaying nowPlaying= DbHelper.getNowPlaying();
		nowPlaying
				.withUrl(getSelectedItem().station.url).save();

	}
	public void updateActivePlaylist() {

	}

	private FastItemAdapter<StationListItem> initFastAdapter(Bundle savedInstanceState, long activePlaylistId){

		fastItemAdapter = new FastItemAdapter<>();
		fastItemAdapter.withSelectable(true);
		fastItemAdapter.withMultiSelect(false);
		fastItemAdapter.withOnClickListener(listItemOnClickListener);
		fastItemAdapter.withFilterPredicate(new IItemAdapter.Predicate() {
			@Override
			public boolean filter(IItem item, CharSequence constraint) {
				return !((StationListItem)item).station.name.toLowerCase()
						.contains(constraint.toString().toLowerCase());
			}
		});
		fastItemAdapter.getItemAdapter().withItemFilterListener(this);
		Timber.w("PlaylistId = %d", activePlaylistId);
		List<Station> list = new Select()
				.from(Station.class).where("PlaylistId = ?",activePlaylistId).execute();
		assertNotNull(list);
		assertTrue(list.size()>0);
		ActiveAndroid.beginTransaction();
		try {
			DbHelper.resetActiveStations();
		for(Station s: list) {
			s.active=true;
			StationListItem stationListItem = new StationListItem()
					.withIdentifier(s.getId())
					.withStation(s);
			fastItemAdapter.add(stationListItem);
			s.position=fastItemAdapter.getAdapterPosition(stationListItem);
			s.save();
		}
		if(savedInstanceState!=null) fastItemAdapter.withSavedInstanceState(savedInstanceState);

		}catch (Exception e){
			Timber.e(e);
			throw new RuntimeException(e.getCause() );
		}
		finally {
			ActiveAndroid.endTransaction();
		}
		return fastItemAdapter;
		/*if(getActionBar()!=null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setHomeButtonEnabled(false);
		}*/
	}
	@Override
	public void itemsFiltered() {
		Timber.v("filtered items count: %d", fastItemAdapter.getItemCount() );
		Toast.makeText(getActivity(), "filtered items count: " + fastItemAdapter.getItemCount(), Toast.LENGTH_SHORT).show();
	}
	@Override
	public boolean itemTouchOnMove(int oldPosition, int newPosition) {
		StationListItem item = (StationListItem) fastItemAdapter.getAdapterItem(oldPosition);
		item.getStation().position=newPosition;
		item =(StationListItem) fastItemAdapter.getAdapterItem(newPosition);
		item.getStation().position=oldPosition;
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
		if(list!=null)Timber.w("selected listsize=%d list:%s",list.size(), list.toString());
		StationListItem item=null;
		if(list!=null && !list.isEmpty()) item=(StationListItem)list.iterator().next();
		if(item!=null)Timber.d("item=%s", item.station.name);
		else Timber.d("item=null");
		return  item;
	}
	public FastItemAdapter.OnClickListener<StationListItem> listItemOnClickListener=new FastAdapter.OnClickListener<StationListItem>() {
		@Override
		public boolean onClick(View v, IAdapter<StationListItem> adapter, StationListItem item, int position) {
			Timber.v("item %s",item.station.name);
			adapter.getFastAdapter().deselect();
			adapter.getFastAdapter().select(position,false);

			//fastItemAdapter.set(position, item.withSetSelected(true));
			//fastItemAdapter.notifyAdapterItemChanged(position);
			mListener.onListFragmentInteraction(item);
			return true;
		}
	};
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
				if (item.getStation().name.equals(oldSelection.getStation().name)) {
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
			if (tempitem.getStation().name.equals(oldSelection.getStation().name)) {
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

		/*if(oldSelection!=null) {
			int oldpos=fastItemAdapter.getAdapterPosition(oldSelection);
			if(oldpos>0) {
				fastItemAdapter.set(oldpos, oldSelection);
				fastItemAdapter.notifyAdapterItemChanged(oldpos);
			}
		}*/
	}

	public interface OnListFragmentInteractionListener {
		// TODO: Update argument type and name
		void onListFragmentInteraction(StationListItem item);
	}
}
