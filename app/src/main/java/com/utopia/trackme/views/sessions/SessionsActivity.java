package com.utopia.trackme.views.sessions;

import static com.utopia.trackme.utils.MyConstants.EXTRA_SESSION;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.utopia.trackme.R;
import com.utopia.trackme.databinding.ActivitySessionsBinding;
import com.utopia.trackme.views.sessiondetails.SessionDetailsActivity;

public class SessionsActivity extends AppCompatActivity {

  SessionsViewModel mViewModel;
  ActivitySessionsBinding mBinding;
  private SessionsAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mViewModel = ViewModelProviders.of(this).get(SessionsViewModel.class);
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_sessions);

    setSupportActionBar(mBinding.toolbar);
    mBinding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

    mBinding.recyclerView.setHasFixedSize(true);
    mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

    mAdapter = new SessionsAdapter((view, item, position) -> {
      startActivity(new Intent(this, SessionDetailsActivity.class).putExtra(EXTRA_SESSION, item));
      overridePendingTransition(0, 0);
    });
    mAdapter.setLoading(false);
    mBinding.recyclerView.setAdapter(mAdapter);

    mViewModel.getSessions();
    mViewModel.getObservableSessions().observe(this, list -> {
      mAdapter.setData(list);
      mAdapter.notifyDataSetChanged();
      mBinding.layoutEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    });
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    finish();
    overridePendingTransition(0, 0);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_session, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_clear) {
      mViewModel.deleteAllSession();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}