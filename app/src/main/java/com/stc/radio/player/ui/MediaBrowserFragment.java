package com.stc.radio.player.ui;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.fastadapter_extensions.drag.SimpleDragCallback;
import com.stc.radio.player.AutoFitGridRecyclerView;
import com.stc.radio.player.R;
import com.stc.radio.player.utils.LogHelper;
import com.stc.radio.player.utils.MediaIDHelper;
import com.stc.radio.player.utils.NetworkHelper;
import com.turingtechnologies.materialscrollbar.DragScrollBar;

import java.util.List;

import timber.log.Timber;


public class MediaBrowserFragment extends Fragment{
	private static final String TAG = LogHelper.makeLogTag(MediaBrowserFragment.class);

	private static final String ARG_MEDIA_ID = "media_id";
	private String mMediaId;
	private static final String LIST_KEY = "com.stc.radio.player.LIST_KEY";
	private MediaFragmentListener mListener;
	private FastItemAdapter<MediaListItem> fastItemAdapter;
	private RecyclerView recyclerView;
	private SimpleDragCallback touchCallback;
	private ItemTouchHelper touchHelper;
	private View mErrorView;
	private TextView mErrorMessage;
	private ProgressBar progressBar;
	private DragScrollBar scrollBar;



	List<MediaListItem> list;
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public MediaBrowserFragment() {

	}

	public void onScrollToItem(String query) {
		for(MediaListItem listItem :    fastItemAdapter.getAdapterItems()){
			String title=listItem.getMediaItem().getDescription().getTitle().toString();
			String name=title.substring(title.indexOf(" - ")+3).trim();
			String simpleQuery=query.trim();
			Timber.w("name=%s", name);
			Timber.w("title=%s", title);
			if (simpleQuery.equalsIgnoreCase(name)){
				recyclerView.smoothScrollToPosition(
						fastItemAdapter.getAdapterPosition(listItem)
				);
				break;
			}
			if (title.equalsIgnoreCase(simpleQuery)){
				recyclerView.smoothScrollToPosition(
						fastItemAdapter.getAdapterPosition(listItem)
				);
				break;
			}
		}
	}

	private final BroadcastReceiver mConnectivityChangeReceiver = new BroadcastReceiver() {
		private boolean oldOnline = false;
		@Override
		public void onReceive(Context context, Intent intent) {
			// We don't care about network changes while this fragment is not associated
			// with a media ID (for example, while it is being initialized)
			if (mMediaId != null) {
				boolean isOnline = com.stc.radio.player.utils.NetworkHelper.isOnline(context);
				if (isOnline != oldOnline) {
					oldOnline = isOnline;
					checkForUserVisibleErrors(false);
					if (isOnline) {
						fastItemAdapter.notifyDataSetChanged();
					}else Toast.makeText(getActivity(), "No connection", Toast.LENGTH_SHORT).show();
				}
			}
		}
	};

	// Receive callbacks from the MediaController. Here we update our state such as which queue
	// is being shown, the current title and description and the PlaybackState.
	private final MediaControllerCompat.Callback mMediaControllerCallback =
			new MediaControllerCompat.Callback() {
				@Override
				public void onMetadataChanged(MediaMetadataCompat metadata) {
					super.onMetadataChanged(metadata);
					if (metadata == null) {
						return;
					}
					LogHelper.d(TAG, "Received metadata change to media ",
							metadata.getDescription().getMediaId());
					fastItemAdapter.notifyDataSetChanged();
					onScrollToItem((String) metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE));
				}

				@Override
				public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
					super.onPlaybackStateChanged(state);
					LogHelper.d(TAG, "Received state change: ", state);
					checkForUserVisibleErrors(false);
					fastItemAdapter.notifyDataSetChanged();
				}
			};

	private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
			new MediaBrowserCompat.SubscriptionCallback() {
				@Override
				public void onChildrenLoaded(@NonNull String parentId,
				                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
					try {

						progressBar.setVisibility(View.GONE);

						//Log.w(TAG, "fragment onChildrenLoaded, parentId=" + parentId +
						//		"  count=" + children.size());
						Timber.w("child hierarchy : %s", children.get(0).getMediaId());
						checkForUserVisibleErrors(children.isEmpty());
						fastItemAdapter.clear();



						for (MediaBrowserCompat.MediaItem item : children) {
							int itemState = MediaListItem.STATE_NONE;
							boolean isFavorite=false;
							//Timber.w("LISTFRAGMENT NEW ITEM: id=%s title=%s desc=%s", item.getMediaId(),item.getDescription().getTitle()
							//,item.toString());
							String musicId = MediaIDHelper.extractMusicIDFromMediaID(
									item.getDescription().getMediaId());
							//isFavorite= RatingHelper.isFavorite(musicId);
							if(isFavorite)Timber.w("%s isFav %b", musicId, isFavorite);
							if (item.isPlayable()) {
								itemState = MediaListItem.STATE_PLAYABLE;
								MediaControllerCompat controller = ((FragmentActivity) getActivity())
										.getSupportMediaController();
								if (controller != null && controller.getMetadata() != null) {
									String currentPlaying = controller.getMetadata().getDescription().getMediaId();

									if (currentPlaying != null && currentPlaying.equals(musicId)) {
										PlaybackStateCompat pbState = controller.getPlaybackState();
										if (pbState == null ||
												pbState.getState() == PlaybackStateCompat.STATE_ERROR) {
											itemState = MediaListItem.STATE_NONE;
										} else if (pbState.getState() == PlaybackStateCompat.STATE_PLAYING) {
											itemState = MediaListItem.STATE_PLAYING;
										} else {
											itemState = MediaListItem.STATE_PAUSED;
										}

									}
								}
							}

							fastItemAdapter.add(new MediaListItem(
									item,
									itemState,
									getActivity(),
									isFavorite
							));
						}
						fastItemAdapter.notifyDataSetChanged();
						progressBar.setVisibility(View.GONE);

					} catch (Throwable t) {
						LogHelper.e(TAG, "Error on childrenloaded", t);
					}
				}

				@Override
				public void onError(@NonNull String id) {
					LogHelper.e(TAG, "browse fragment subscription onError, id=" + id);
					Toast.makeText(getActivity(), R.string.error_loading_media, Toast.LENGTH_LONG).show();
					checkForUserVisibleErrors(true);
				}
			};


	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof MediaFragmentListener) {
			mListener = (MediaFragmentListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnListFragmentInteractionListener");
		}
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		setRetainInstance(true);

		View view = inflater.inflate(R.layout.fragment_list_uamp, container, false);
		mErrorView = view.findViewById(R.id.playback_error);
		mErrorMessage = (TextView) mErrorView.findViewById(R.id.error_message);
		Context context = view.getContext();
		progressBar=(ProgressBar)view.findViewById(R.id.progress_bar);
		scrollBar=(DragScrollBar)view.findViewById(R.id.dragScrollBar);
			recyclerView = (AutoFitGridRecyclerView)view.findViewById(R.id.recyclerViewAutoFit);
			//scrollBar.setIndicator(new AlphabetIndicator(getActivity()),true);
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.setLayoutManager(new GridLayoutManager(context, 3));
			//final FastScrollIndicatorAdapter<MediaListItem> fastScrollIndicatorAdapter = new FastScrollIndicatorAdapter<>();

			fastItemAdapter = new FastItemAdapter<>();
			fastItemAdapter.withSelectable(false);
			fastItemAdapter.withMultiSelect(false);
			fastItemAdapter.select(true);
			fastItemAdapter.withOnClickListener(listItemOnClickListener);
			recyclerView.setAdapter(fastItemAdapter);
			//DragScrollBar materialScrollBar = new DragScrollBar(context, recyclerView, true);
			//materialScrollBar.addIndicator(new AlphabetIndicator(context), true);

			//fastItemAdapter.notifyAdapterDataSetChanged();
			//EventBus.getDefault().post(fastItemAdapter);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		// fetch browsing information to fill the listview:
		MediaBrowserCompat mediaBrowser = mListener.getMediaBrowser();



		LogHelper.d(TAG, "fragment.onStart, mediaId=", mMediaId,
				"  onConnected=" + mediaBrowser.isConnected());

		if (mediaBrowser.isConnected()) {
			onConnected();
			progressBar.setVisibility(View.GONE);
		}else progressBar.setVisibility(View.VISIBLE);

		// Registers BroadcastReceiver to track network connection changes.
		this.getActivity().registerReceiver(mConnectivityChangeReceiver,
				new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	@Override
	public void onStop() {
		super.onStop();
		MediaBrowserCompat mediaBrowser = mListener.getMediaBrowser();
		if (mediaBrowser != null && mediaBrowser.isConnected() && mMediaId != null) {
			mediaBrowser.unsubscribe(mMediaId);
		}
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if (controller != null) {
			controller.unregisterCallback(mMediaControllerCallback);
		}
		this.getActivity().unregisterReceiver(mConnectivityChangeReceiver);
	}
	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}
	public String getMediaId() {
		Bundle args = getArguments();
		if (args != null) {
			return args.getString(ARG_MEDIA_ID);
		}
		return null;
	}



	public void setMediaId(String mediaId) {
		Bundle args = new Bundle(1);
		args.putString(MediaBrowserFragment.ARG_MEDIA_ID, mediaId);
		setArguments(args);
	}

	// Called when the MediaBrowser is connected. This method is either called by the
	// fragment.onStart() or explicitly by the activity in the case where the connection
	// completes after the onStart()
	public void onConnected() {
		if (isDetached()) {
			return;
		}
		mMediaId = getMediaId();
		mListener.getMediaBrowser();
		if (mMediaId == null) {
			mMediaId = mListener.getMediaBrowser().getRoot();
		}
		updateTitle();
		mListener.getMediaBrowser().unsubscribe(mMediaId);

		mListener.getMediaBrowser().subscribe(mMediaId, mSubscriptionCallback);

		// Add MediaController callback so we can redraw the list when metadata changes:
		MediaControllerCompat controller = ((FragmentActivity) getActivity())
				.getSupportMediaController();
		if (controller != null) {
			controller.registerCallback(mMediaControllerCallback);
		}
	}

	private void checkForUserVisibleErrors(boolean forceError) {
		boolean showError = forceError;
		// If offline, message is about the lack of connectivity:
		if (!NetworkHelper.isOnline(getActivity())) {
			mErrorMessage.setText(R.string.error_no_connection);
			showError = true;
		} else {
			// otherwise, if state is ERROR and metadata!=null, use playback state error message:
			MediaControllerCompat controller = ((FragmentActivity) getActivity())
					.getSupportMediaController();
			if (controller != null
					&& controller.getMetadata() != null
					&& controller.getPlaybackState() != null
					&& controller.getPlaybackState().getState() == PlaybackStateCompat.STATE_ERROR
					&& controller.getPlaybackState().getErrorMessage() != null) {
				mErrorMessage.setText(controller.getPlaybackState().getErrorMessage());
				showError = true;
			} else if (forceError) {
				// Finally, if the caller requested to show error, show a generic message:
				mErrorMessage.setText(R.string.error_loading_media);
				showError = true;
			}
		}
		mErrorView.setVisibility(showError ? View.VISIBLE : View.GONE);
		LogHelper.d(TAG, "checkForUserVisibleErrors. forceError=", forceError,
				" showError=", showError,
				" isOnline=", NetworkHelper.isOnline(getActivity()));
	}

	private void updateTitle() {
		if (MediaIDHelper.MEDIA_ID_ROOT.equals(mMediaId)) {
			mListener.setToolbarTitle(null);
			return;
		}

		MediaBrowserCompat mediaBrowser = mListener.getMediaBrowser();
		mediaBrowser.getItem(mMediaId, new MediaBrowserCompat.ItemCallback() {
			@Override
			public void onItemLoaded(MediaBrowserCompat.MediaItem item) {
				mListener.setToolbarTitle(
						item.getDescription().getTitle());
			}
		});
	}

	public interface MediaFragmentListener extends MediaBrowserProvider {
		void onMediaItemSelected(MediaBrowserCompat.MediaItem item);
		void setToolbarTitle(CharSequence title);
		void isItemFavorite(String musicId);
	}

	public FastItemAdapter.OnClickListener<MediaListItem> listItemOnClickListener=new FastAdapter.OnClickListener<MediaListItem>() {
		@Override
		public boolean onClick(View v, IAdapter<MediaListItem> adapter, MediaListItem item, int position) {
			Timber.v("item %s",item.getName());
			checkForUserVisibleErrors(false);
			MediaBrowserCompat.MediaItem mediaItem = fastItemAdapter.getItem(position).getMediaItem();
			mListener.onMediaItemSelected(mediaItem);
			return true;
		}
	};

/*

@Override
public void itemsFiltered() {
	Timber.v("filtered items count: %d", fastItemAdapter.getItemCount() );
	Toast.makeText(getActivity(), "filtered items count: " + fastItemAdapter.getItemCount(), Toast.LENGTH_SHORT).show();
}
	@Override
	public boolean itemTouchOnMove(int oldPosition, int newPosition) {
		if(newPosition!=oldPosition) {

			Collections.swap(fastItemAdapter.getAdapterItems(), oldPosition, newPosition); // change position
			fastItemAdapter.notifyAdapterItemMoved(oldPosition, newPosition);
		}
		return true;
	}
*/

/*


	public void filter(String s) {
		if(fastItemAdapter!=null){
			fastItemAdapter.filter(s);
		}
	}*/


	/*
	public void restoreState(int position){
		if(position>-1 && fastItemAdapter!=null){
			MediaListItem item = getSelectedItem();
			if(item !=null && fastItemAdapter.getAdapterPosition(item)!=position)
				fastItemAdapter.deselect();
			fastItemAdapter.select(position, false);
			if(recyclerView!=null) recyclerView.smoothScrollToPosition(position);
		}
	}
	public void selectNextItem() {
		MediaListItem item=null;
		int pos=-1;
		List list=  fastItemAdapter.getAdapterItems();
		assertNotNull(list);
		assertFalse(list.isEmpty());
		if(!DbHelper.isShuffle()) {
			MediaListItem oldSelection = getSelectedItem();
			Iterator iterator = list.iterator();
			while (iterator.hasNext()) {
				item = (MediaListItem) iterator.next();
				if (item.getStation().getName().equals(oldSelection.getStation().getName())) {
					if (iterator.hasNext()) item = (MediaListItem) iterator.next();
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
		MediaListItem item=null;
		List list=  fastItemAdapter.getAdapterItems();
		assertNotNull(list);
		assertFalse(list.isEmpty());
		MediaListItem oldSelection = getSelectedItem();

		Iterator iterator = list.iterator();
		item = (MediaListItem) iterator.next();
		while (iterator.hasNext()) {
			MediaListItem tempitem = (MediaListItem) iterator.next();
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
}
