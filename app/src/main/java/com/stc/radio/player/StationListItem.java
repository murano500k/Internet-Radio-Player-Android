package com.stc.radio.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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
import static junit.framework.Assert.assertNotNull;

public class StationListItem
		extends AbstractItem<StationListItem, StationListItem.ViewHolder> implements IDraggable<StationListItem, StationListItem> {

	public static final ViewHolderFactory<? extends StationListItem.ViewHolder> FACTORY = new StationListItem.ItemFactory();

	public Station station=null;
	private boolean mIsDraggable = true;
	private Bitmap icon;

	public StationListItem withIcon(Bitmap b) {
		this.icon=b;
		return this;
	}

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
		return station;
	}


	@Override
	public void bindView(ViewHolder viewHolder, List payloads) {
		super.bindView(viewHolder, payloads);
		Context ctx = viewHolder.itemView.getContext();
		assertNotNull(this.station);
		assertNotNull(this.station.getName());
		if(station.getName()!=null && viewHolder.name!=null)viewHolder.name.setText(station.getName());
		viewHolder.name.setBackgroundColor(ctx.getResources().getColor(isSelected() ? R.color.colorAccent : R.color.cardview_dark_background));
		viewHolder.name.setTextColor(ctx.getResources().getColor(isSelected() ? R.color.colorPrimary : R.color.md_white_1000));
		viewHolder.favButton.setOnLikeListener(new OnLikeListener() {
			@Override
			public void liked(LikeButton likeButton) {
				station.setFavorite(true);
				station.save();
			}
			@Override
			public void unLiked(LikeButton likeButton) {
				station.setFavorite(false);
				station.save();
			}
		});
		viewHolder.favButton.setLiked(station.isFavorite());

		PabloPicasso.with(getContext()).load(station.getArtUrl()).error(R.drawable.default_art).tag(station.getKey()).fit().into(viewHolder.icon);

	}
	@Override
	public int getType() {
		return R.id.fastadapter_sample_item_id;
	}


	@Override
	public int getLayoutRes() {
		return R.layout.list_item;
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

		ImageView icon;

		LikeButton favButton;



		public ViewHolder(View view) {
			super(view);
			this.view = view;
			name=(TextView)view.findViewById(R.id.textName);
			favButton=(LikeButton) view.findViewById(R.id.fav_button);
			icon=(ImageView) view.findViewById(R.id.icon_list_item);
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
