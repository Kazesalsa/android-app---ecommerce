package com.example.btday4.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.btday4.data.repository.OrderRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.ActivityOrderHistoryBinding;
import com.example.btday4.util.Constants;
import com.example.btday4.util.UiUtils;

/**
 * Displays the authenticated user's order history loaded from Room SQLite.
 */
public class OrderHistoryActivity extends AppCompatActivity {

    private ActivityOrderHistoryBinding binding;
    private OrderAdapter adapter;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.orderHistoryRoot);

        userSession = new UserSession(this);
        binding.toolbarOrders.setNavigationOnClickListener(v -> finish());

        adapter = new OrderAdapter(orderWithItems -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra(Constants.EXTRA_ORDER_ID, orderWithItems.order.getId());
            intent.putExtra(Constants.EXTRA_IS_NEW_ORDER, false);
            startActivity(intent);
        });
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);

        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        OrderRepository.getOrdersForUser(this, userSession.getUsername(), orders -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            adapter.submitList(orders);
            binding.llOrdersEmpty.setVisibility(orders == null || orders.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
