package com.utopia.trackme.views.sessions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.utopia.trackme.R;
import com.utopia.trackme.data.remote.pojo.SessionResponse;
import com.utopia.trackme.databinding.LayoutSessionItemBinding;
import com.utopia.trackme.utils.SystemUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SessionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private List<SessionResponse> mData = new ArrayList<>();
  private boolean mIsLoading = false;
  private int mPage = 1;
  private final OnItemClickListener listener;

  public void setLoading(boolean loading) {
    mIsLoading = loading;
  }

  public boolean isLoading() {
    return mIsLoading;
  }

  public int getPage() {
    return mPage;
  }

  public void setPage(int mPage) {
    this.mPage = mPage;
  }

  public void setData(List<SessionResponse> data) {
    this.mData = data;
  }

  public List<SessionResponse> getData() {
    return mData;
  }

  public SessionsAdapter(OnItemClickListener listener) {
    this.listener = listener;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutSessionItemBinding viewBinding = DataBindingUtil
        .inflate(LayoutInflater.from(parent.getContext()), R.layout.layout_session_item, parent,
            false);
    return new MyViewHolder(viewBinding);
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    ((MyViewHolder) holder).bind(mData.get(position), position, this.listener);
  }

  @Override
  public int getItemCount() {
    return mData.size();
  }

  static class MyViewHolder extends RecyclerView.ViewHolder {

    LayoutSessionItemBinding mBinding;

    MyViewHolder(LayoutSessionItemBinding binding) {
      super(binding.getRoot());
      this.mBinding = binding;
    }

    void bind(SessionResponse session, int position, OnItemClickListener listener) {
      mBinding.duration.setText(SystemUtils.convertTime(Long.parseLong(session.getDuration())));
      mBinding.startTime.setText(SystemUtils.getTimeAgo(session.getStartTime()));
      mBinding.distance
          .setText(new DecimalFormat("#.### miles").format(Double.valueOf(session.getDistance())));
      mBinding.averageSpeed.setText(
          new DecimalFormat("#.# m/s").format(Double.parseDouble(session.getAverageSpeed())));

      mBinding.layoutMain
          .setOnClickListener(v -> listener.onItemClick(mBinding.layoutMain, session, position));
    }
  }

  public interface OnItemClickListener {

    void onItemClick(View view, SessionResponse item, int position);
  }

}
