package com.stc.radio.player.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.stc.radio.player.R;
import com.stc.radio.player.ui.customviews.GridItemImageView;
import com.stc.radio.player.utils.MediaIDHelper;
import com.stc.radio.player.utils.PabloPicasso;

import java.util.List;

;


public class MediaListItem
		extends AbstractItem<MediaListItem, MediaListItem.ViewHolder>
		/*  implements Parcelable , IDraggable<MediaListItem, MediaListItem>*/
{

	public static final ViewHolderFactory<? extends MediaListItem.ViewHolder> FACTORY = new MediaListItem.ItemFactory();
	public static final int STATE_INVALID = -1;
	public static final int STATE_NONE = 0;
	public static final int STATE_PLAYABLE = 1;
	public static final int STATE_PAUSED = 2;
	public static final int STATE_PLAYING = 3;
	public  static ColorStateList sColorStatePlaying;
	public  static ColorStateList sColorStateNotPlaying;
	private Integer state;
	private MediaBrowser.MediaItem mediaItem;
	Activity activity;

	public MediaListItem(MediaBrowser.MediaItem mediaItem, int state, Activity activity) {
		this.mediaItem=mediaItem;
		this.state=state;
		this.activity=activity;
	}

	@Override
	public boolean isSelected() {
		return super.isSelected();
	}

	@Override
	public MediaListItem withSetSelected(boolean selected) {
		return super.withSetSelected(selected);
	}

	public String getName() {
		return mediaItem.getDescription().getTitle().toString();
	}

	@Override
	public void bindView(ViewHolder viewHolder, List payloads) {
		super.bindView(viewHolder, payloads);
		String key = mediaItem.getDescription().getMediaId();
		CharSequence name="";
		if(mediaItem.getDescription().getTitle()!=null){
			String  nameStr = mediaItem.getDescription().getTitle().toString();
			name = nameStr.replace(" - ", "\n");
		}

		Uri artUrl = mediaItem.getDescription().getIconUri();
		Context ctx = activity;

		state = STATE_NONE;
		if (mediaItem!=null && mediaItem.isPlayable()) {
			state = STATE_PLAYABLE;
			MediaController controller = ((FragmentActivity) activity)
					.getMediaController();
			if (controller != null && controller.getMetadata() != null) {
				String currentPlaying = controller.getMetadata().getDescription().getMediaId();
				String musicId = MediaIDHelper.extractMusicIDFromMediaID(key);
				if (currentPlaying != null && currentPlaying.equals(musicId)) {
					PlaybackState pbState = controller.getPlaybackState();
					if (pbState == null ||
							pbState.getState() == PlaybackState.STATE_ERROR) {
						state = STATE_NONE;
					} else if (pbState.getState() == PlaybackState.STATE_PLAYING) {
						state = STATE_PLAYING;
					} else {
						state = STATE_PAUSED;
					}
				}
			}
		}



		if (sColorStateNotPlaying == null || sColorStatePlaying == null) {
			initializeColorStateLists(ctx);
		}
		Integer cachedState = STATE_INVALID;
		if(viewHolder.itemView!=null) cachedState = (Integer) viewHolder.itemView.getTag(R.id.tag_mediaitem_state_cache);

		if(name!=null && viewHolder.name!=null)viewHolder.name.setText(name);
		viewHolder.icon.setImageResource( R.drawable.ic_queue_music_black_24dp);
		PabloPicasso.with(activity)
				.load(artUrl)
				.error(R.drawable.ic_queue_music_black_24dp)
				.tag(key)
				.into(viewHolder);
		if (cachedState == null || !cachedState.equals(state)) {
			switch (state) {
				case STATE_PLAYABLE:
					Drawable pauseDrawable = ctx.getDrawable(
							R.drawable.ic_play_arrow_black_36dp);
					pauseDrawable.setTintList(sColorStateNotPlaying);
					viewHolder.playbackIndicator.setImageDrawable(pauseDrawable);
					viewHolder.playbackIndicator.setVisibility(View.VISIBLE);
					break;
				case STATE_PLAYING:
					AnimationDrawable animation = (AnimationDrawable)
							ctx.getDrawable(R.drawable.ic_equalizer_white_36dp);
					animation.setTintList(sColorStatePlaying);
					viewHolder.playbackIndicator.setImageDrawable(animation);
					viewHolder.playbackIndicator.setVisibility(View.VISIBLE);
					animation.start();
					break;
				case STATE_PAUSED:
					Drawable playDrawable = ctx.getDrawable(
							R.drawable.ic_equalizer1_white_36dp);
					playDrawable.setTintList( sColorStatePlaying);
					viewHolder.playbackIndicator.setImageDrawable(playDrawable);
					viewHolder.playbackIndicator.setVisibility(View.VISIBLE);
					break;
				default:
					viewHolder.playbackIndicator.setVisibility(View.GONE);
			}
		}
		viewHolder.itemView.setTag(R.id.tag_mediaitem_state_cache, state);
	}

	static private void initializeColorStateLists(Context ctx) {
		sColorStateNotPlaying = ColorStateList.valueOf(ctx.getResources().getColor(
				R.color.media_item_icon_not_playing));
		sColorStatePlaying = ColorStateList.valueOf(ctx.getResources().getColor(
				R.color.media_item_icon_playing));
	}
	@Override
	public int getType() {
		return R.id.mediaitem_id;
	}


	@Override
	public int getLayoutRes() {
		return R.layout.my_media_list_item;
	}

	public MediaBrowser.MediaItem getMediaItem() {

		return mediaItem;
	}

	protected static class ItemFactory implements ViewHolderFactory<ViewHolder> {
		public ViewHolder create(View v) {
			return new ViewHolder(v);
		}
	}

	@Override
	public ViewHolderFactory<? extends ViewHolder> getFactory() {
		return FACTORY;
	}
	protected static class ViewHolder extends RecyclerView.ViewHolder implements Target

	{


		protected View view;
		TextView name;
		ImageView playbackIndicator;

		GridItemImageView icon;



		public ViewHolder(View view) {
			super(view);
			this.view = view;
			name=(TextView)view.findViewById(R.id.title);
			icon=(GridItemImageView) view.findViewById(R.id.icon_list_item);
			playbackIndicator=(ImageView) view.findViewById(R.id.play_eq);
		}


		@Override
		public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
			if(icon!=null){
				icon.setImageBitmap(bitmap);
			}
		}

		@Override
		public void onBitmapFailed(Drawable errorDrawable) {

		}

		@Override
		public void onPrepareLoad(Drawable placeHolderDrawable) {

		}
	}



}
