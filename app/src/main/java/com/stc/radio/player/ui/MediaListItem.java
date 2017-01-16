package com.stc.radio.player.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.like.LikeButton;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.stc.radio.player.GridItemImageView;
import com.stc.radio.player.R;
import com.stc.radio.player.utils.MediaIDHelper;
import com.stc.radio.player.utils.PabloPicasso;

import java.util.List;


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
	private boolean favorite;
	private CharSequence name;
	private Uri artUrl;
	public  static ColorStateList sColorStatePlaying;
	public  static ColorStateList sColorStateNotPlaying;
	private boolean mIsDraggable = true;
	private Integer state;
	private MediaBrowserCompat.MediaItem mediaItem;
	Activity activity;

	/*protected MediaListItem(Parcel in) {
		mIsDraggable = in.readByte() != 0;
		key = in.readString();
		name = in.readString();
		artUrl = Uri.parse(in.readString());
		state=in.readInt();
	}

	public MediaListItem(String key, String artUrl, String name) {
		this.key = key;
		this.name = name;
		this.artUrl = artUrl;
	}*/
	/*public MediaListItem(MediaDescriptionCompat description, int state) {
		this.key = description.getMediaId();
		this.name = description.getDescription();
		this.artUrl = description.getIconUri();
		this.state=state;
	}*/
	public MediaListItem(MediaBrowserCompat.MediaItem mediaItem, int state, Activity activity) {
	this.mediaItem=mediaItem;
		this.state=state;
		this.activity=activity;
		}
	public MediaListItem(MediaBrowserCompat.MediaItem mediaItem, int state, Activity activity, boolean favorite) {
	this.mediaItem=mediaItem;
		this.state=state;
		this.activity=activity;
		this.favorite=favorite;
		}

/*	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(key);
		dest.writeString(name.toString());
		dest.writeString(artUrl.toString());
		dest.writeInt(state);

	}


	public static final Creator<MediaListItem> CREATOR = new Creator<MediaListItem>() {
		@Override
		public MediaListItem createFromParcel(Parcel in) {

			return new MediaListItem(in);
		}

		@Override
		public MediaListItem[] newArray(int size) {
			return new MediaListItem[size];
		}
	};
*/

	@Override
	public boolean isSelected() {
		return super.isSelected();
	}

	@Override
	public MediaListItem withSetSelected(boolean selected) {
		return super.withSetSelected(selected);
	}

/*
	@Override
	public boolean isDraggable() {
		return false;
	}

	@Override
	public MediaListItem withIsDraggable(boolean draggable) {
		this.mIsDraggable = draggable;
		return this;
	}*/

	public Uri getArtUrl() {
		return mediaItem.getDescription().getIconUri();
	}

	public String getKey() {
		return mediaItem.getDescription().getMediaId();
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
			MediaControllerCompat controller = ((FragmentActivity) activity)
					.getSupportMediaController();
			if (controller != null && controller.getMetadata() != null) {
				String currentPlaying = controller.getMetadata().getDescription().getMediaId();
				String musicId = MediaIDHelper.extractMusicIDFromMediaID(
						key);
				viewHolder.favButton.setLiked(favorite);

				if (currentPlaying != null && currentPlaying.equals(musicId)) {
					PlaybackStateCompat pbState = controller.getPlaybackState();
					if (pbState == null ||
							pbState.getState() == PlaybackStateCompat.STATE_ERROR) {
						state = STATE_NONE;
					} else if (pbState.getState() == PlaybackStateCompat.STATE_PLAYING) {
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
		viewHolder.icon.setImageResource( R.drawable.default_art);
		PabloPicasso.with(activity)
				.load(artUrl)
				.error(R.drawable.default_art)
				.tag(key)
				.into(viewHolder);
		if (cachedState == null || !cachedState.equals(state)) {
			switch (state) {
				case STATE_PLAYABLE:
					Drawable pauseDrawable = ContextCompat.getDrawable(ctx,
							R.drawable.ic_play_arrow_black_36dp);
					DrawableCompat.setTintList(pauseDrawable, sColorStateNotPlaying);
					viewHolder.playbackIndicator.setImageDrawable(pauseDrawable);
					viewHolder.playbackIndicator.setVisibility(View.VISIBLE);
					break;
				case STATE_PLAYING:
					AnimationDrawable animation = (AnimationDrawable)
							ContextCompat.getDrawable(ctx, R.drawable.ic_equalizer_white_36dp);
					DrawableCompat.setTintList(animation, sColorStatePlaying);
					viewHolder.playbackIndicator.setImageDrawable(animation);
					viewHolder.playbackIndicator.setVisibility(View.VISIBLE);
					animation.start();
					break;
				case STATE_PAUSED:
					Drawable playDrawable = ContextCompat.getDrawable(ctx,
							R.drawable.ic_equalizer1_white_36dp);
					DrawableCompat.setTintList(playDrawable, sColorStatePlaying);
					viewHolder.playbackIndicator.setImageDrawable(playDrawable);
					viewHolder.playbackIndicator.setVisibility(View.VISIBLE);
					break;
				default:
					viewHolder.playbackIndicator.setVisibility(View.GONE);
			}
		}

		if(favorite) {
			viewHolder.favButton.setLiked(true);
		}else viewHolder.favButton.setLiked(false);


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

	public MediaBrowserCompat.MediaItem getMediaItem() {

		return mediaItem;
	}

/*
	@Override
	public int describeContents() {
		return 1933;
	}

	public MediaListItem(Parcel in) {
		String mediaId=in.readString();
		String musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId);
		if(musicId!=null) {
			From from = new Select().from(DBMediaItem.class).where("MediaId = ?",musicId);
			if(from.exists()){
				DBMediaItem dbMediaItem=from.executeSingle();
			}
		}
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mediaItem.getMediaId());
	}

	public static final Creator<MediaListItem> CREATOR = new Creator<MediaListItem>() {
		@Override
		public MediaListItem createFromParcel(Parcel in) {

			return new MediaListItem(in);
		}

		@Override
		public MediaListItem[] newArray(int size) {
			return new MediaListItem[size];
		}
	};*/


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

		LikeButton favButton;


		public ViewHolder(View view) {
			super(view);
			this.view = view;
			name=(TextView)view.findViewById(R.id.title);
			favButton=(LikeButton) view.findViewById(R.id.fav_button);
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
