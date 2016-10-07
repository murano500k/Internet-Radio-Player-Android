package com.stc.radio.player.ui;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.fastadapter.IDraggable;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.stc.radio.player.R;
import com.stc.radio.player.db.DbHelper;
import com.stc.radio.player.db.Station;
import com.stc.radio.player.utils.PabloPicasso;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static junit.framework.Assert.assertNotNull;

public class StationListItem
		extends AbstractItem<StationListItem, StationListItem.ViewHolder> implements IDraggable<StationListItem, StationListItem> {

	public static final ViewHolderFactory<? extends StationListItem.ViewHolder> FACTORY = new StationListItem.ItemFactory();

	public Station station=null;
	private boolean mExpanded = false;
	private boolean mIsDraggable = true;
	private boolean mFavorite = false;
	private int mIconRes;


	public boolean isAnimated() {
		return mAnimated;
	}

	public boolean isFavorite() {
		return mFavorite;
	}

	private boolean mAnimated = false;



	public StationListItem withFavorite(boolean favorite) {
		this.mFavorite = favorite;

		return this;
	}

	public StationListItem withIconRes(@DrawableRes int iconRes) {
		this.mIconRes=iconRes;
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
		return super.getIdentifier();
	}

	public StationListItem withStation(Station station) {
		this.station=station;
		return this;
	}
	public Station getStation() {
		return station;
	}

	@Override
	public StationListItem withTag(Object object) {
		return super.withTag(object);
	}

	@Override
	public Object getTag() {
		return super.getTag();
	}


	@Override
	public void bindView(ViewHolder viewHolder, List payloads) {
		super.bindView(viewHolder, payloads);
		Context ctx = viewHolder.itemView.getContext();
		/*viewHolder.extrasLayout.setVisibility(isExpanded() ? View.VISIBLE : View.GONE);
		if(isExpanded()){
			viewHolder.fav.setImageResource(isFavorite() ? R.drawable.ic_fav_enabled : R.drawable.ic_fav_disabled);
			viewHolder.icon.setIcon(new IconicsDrawable(ctx, MaterialDesignIconic.Icon.gmi_playstation).color(Color.LTGRAY));
			viewHolder.icon.setImageResource(mIconRes>0 ? mIconRes : R.drawable.ic_checked);
			if(!viewHolder.anim.isAnimating()) viewHolder.anim.animateBars();
			viewHolder.anim.setVisibility(isAnimated() ? View.VISIBLE : View.GONE);
		}
		//get the context*/
		assertNotNull(this.station);
		String artUrl = DbHelper.getArtUrl(station.url);
		//Timber.w("station %s artUrl=%s", station.name, artUrl);
		PabloPicasso.with(ctx).load(Uri.parse(artUrl))
				.placeholder(R.drawable.ic_default_art)
				.error(R.drawable.ic_default_art)
				//.resizeDimen(R.dimen.list_item_art_size, R.dimen.list_item_art_size)
				.fit()
				.tag(ctx)
				.into(viewHolder.icon);

		viewHolder.name.setText(station.name);
		viewHolder.name.setBackgroundColor(ctx.getResources().getColor(isSelected() ? R.color.colorAccent : R.color.cardview_dark_background));
		viewHolder.name.setTextColor(ctx.getResources().getColor(isSelected() ? R.color.colorPrimary : R.color.md_white_1000));
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
	protected static class ViewHolder extends RecyclerView.ViewHolder {
		protected View view;
		@BindView(R.id.name)
		TextView name;

		@BindView(R.id.icon)
		ImageView icon;




		public ViewHolder(View view) {
			super(view);
			ButterKnife.bind(this, view);
			this.view = view;
		}
	}
}
