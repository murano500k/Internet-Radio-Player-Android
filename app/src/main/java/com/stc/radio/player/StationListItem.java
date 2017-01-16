package com.stc.radio.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.mikepenz.fastadapter.IDraggable;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.utils.PabloPicasso;

import java.util.List;

import static com.activeandroid.Cache.getContext;

public class StationListItem
		extends AbstractItem<StationListItem, StationListItem.ViewHolder> implements IDraggable<StationListItem, StationListItem>,Parcelable {

	public static final ViewHolderFactory<? extends StationListItem.ViewHolder> FACTORY = new StationListItem.ItemFactory();

	public Station station=null;
	private String key;
	private String name;
	private String artUrl;

	private boolean mIsDraggable = true;
	private Bitmap icon;

	protected StationListItem(Parcel in) {
		mIsDraggable = in.readByte() != 0;

		key = in.readString();
		name = in.readString();
		artUrl = in.readString();
	}

	public StationListItem(String key, String artUrl, String name) {
		this.key = key;
		this.name = name;
		this.artUrl = artUrl;
	}
	public StationListItem(Station s) {
		this.key = s.getKey();
		this.name = s.getName();
		this.artUrl = s.getArtUrl();
		station=s;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(key);
		dest.writeString(name);
		dest.writeString(artUrl);

	}

	public static final Creator<StationListItem> CREATOR = new Creator<StationListItem>() {
		@Override
		public StationListItem createFromParcel(Parcel in) {

			return new StationListItem(in);
		}

		@Override
		public StationListItem[] newArray(int size) {
			return new StationListItem[size];
		}
	};

	@Override
	public boolean isSelected() {
		return super.isSelected();
	}

	@Override
	public StationListItem withSetSelected(boolean selected) {
		return super.withSetSelected(selected);
	}


	@Override
	public boolean isDraggable() {
		return mIsDraggable;
	}

	@Override
	public StationListItem withIsDraggable(boolean draggable) {
		this.mIsDraggable = draggable;
		return this;
	}

	@Override
	public StationListItem withIdentifier(long identifier) {
		return super.withIdentifier(identifier);
	}

	@Override
	public long getIdentifier() {
		if(station==null )return -1;
		return station.getId();

	}

	public StationListItem withStation(Station station) {
		this.station=station;
		return this;
	}
	public Station getStation() {
		if(key==null) return null;
		From from = new Select().from(Station.class).where("Key = ?", key);
		if(!from.exists()) return null;
		return from.executeSingle();
	}

	public String getArtUrl() {
		return artUrl;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	@Override
	public void bindView(ViewHolder viewHolder, List payloads) {
		super.bindView(viewHolder, payloads);
		Context ctx = viewHolder.itemView.getContext();
		//assertNotNull(this.station);
		//assertNotNull(this.station.getName());
		if(name!=null && viewHolder.name!=null)viewHolder.name.setText(name);
		//viewHolder.name.setBackgroundColor(ctx.getResources().getColor(isSelected() ? R.color.colorAccent : R.color.cardview_dark_background));
		//viewHolder.name.setTextColor(ctx.getResources().getColor(isSelected() ? R.color.colorPrimary : R.color.md_white_1000));
		viewHolder.favButton.setOnLikeListener(new OnLikeListener() {
			@Override
			public void liked(LikeButton likeButton) {
				if(station==null )station=getStation();
				if(station!=null) {
					station.setFavorite(true);
					station.save();
				}
			}
			@Override
			public void unLiked(LikeButton likeButton) {
				if(station==null )station=getStation();
				if(station!=null) {
					station.setFavorite(false);
					station.save();
				}
			}
		});
		if(station==null )station=getStation();
		if(station!=null) viewHolder.favButton.setLiked(station.isFavorite());
		viewHolder.icon.setImageResource( R.drawable.default_art);
		PabloPicasso.with(getContext())
				.load(artUrl)
				.error(R.drawable.default_art)
				.tag(key)
				.into(viewHolder);
		/*viewHolder.pulsator.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Timber.w("check");
				if(event.getAction()==MotionEvent.ACTION_DOWN) //viewHolder.pulsator.start();
					((PulsatorLayout)v).start();
				return false;
			}
		});*/


	}
	@Override
	public int getType() {
		return R.id.fastadapter_sample_item_id;
	}


	@Override
	public int getLayoutRes() {
		return R.layout.grid_item;
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

		GridItemImageView icon;

		LikeButton favButton;



		/*public ViewHolder(View view) {
			super(view);
			this.view = view;
			name=(TextView)view.findViewById(R.id.textName);
			favButton=(LikeButton) view.findViewById(R.id.fav_button);
			icon=(ImageView) view.findViewById(R.id.icon_list_item);
			pulsator = (PulsatorLayout) view.findViewById(R.id.pulsator);
			view.setOnClickListener(v -> {
				pulsator.start();
			});
		}*/
		public ViewHolder(View view) {
			super(view);
			this.view = view;
			name=(TextView)view.findViewById(R.id.textName);
			favButton=(LikeButton) view.findViewById(R.id.fav_button);
			icon=(GridItemImageView) view.findViewById(R.id.icon_list_item);


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
