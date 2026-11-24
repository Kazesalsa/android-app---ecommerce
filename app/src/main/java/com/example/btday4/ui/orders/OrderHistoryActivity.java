package com.example.btday4.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.btday4.R;
import com.example.btday4.data.repository.OrderRepository;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.ActivityOrderHistoryBinding;
import com.example.btday4.databinding.ItemOrderTabBinding;
import com.example.btday4.model.OrderStatus;
import com.example.btday4.model.OrderWithItems;
import com.example.btday4.util.Constants;
import com.example.btday4.util.UiUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the signed-in user's order history with Shopee-like inline status filter tabs.
 */
public class OrderHistoryActivity extends AppCompatActivity {

    /**
     * Tab order shown to the user; {@code null} means "Tất cả".
     */
    private static final OrderStatus[] TAB_FILTERS = {
            null,
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.PAID,
            OrderStatus.PENDING_CONFIRMATION,
            OrderStatus.CANCELLED
    };

    private ActivityOrderHistoryBinding binding;
    private OrderAdapter adapter;
    private UserSession userSession;
    private final List<ItemOrderTabBinding> tabs = new ArrayList<>();
    private final List<OrderWithItems> allOrders = new ArrayList<>();
    private OrderStatus selectedFilter = null;

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

        buildTabs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void buildTabs() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (OrderStatus filter : TAB_FILTERS) {
            ItemOrderTabBinding tab = ItemOrderTabBinding.inflate(inflater, binding.llOrderTabs, false);
            tab.tvTabLabel.setText(labelFor(filter));
            tab.getRoot().setOnClickListener(v -> selectFilter(filter, tab.getRoot()));
            binding.llOrderTabs.addView(tab.getRoot());
            tabs.add(tab);
        }
        styleTabs();
    }

    private void selectFilter(OrderStatus filter, View tabView) {
        if (selectedFilter == filter) {
            return;
        }
        selectedFilter = filter;
        styleTabs();
        binding.scrollOrderTabs.smoothScrollTo(
                Math.max(0, tabView.getLeft() - (binding.scrollOrderTabs.getWidth() - tabView.getWidth()) / 2),
                0
        );
        applyFilter();
    }

    private void styleTabs() {
        for (int i = 0; i < tabs.size(); i++) {
            ItemOrderTabBinding tab = tabs.get(i);
            UiUtils.styleUnderlineTab(tab.tvTabLabel, tab.viewTabIndicator, TAB_FILTERS[i] == selectedFilter);
        }
    }

    private void loadOrders() {
        OrderRepository.getOrdersForUser(this, userSession.getUsername(), orders -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            allOrders.clear();
            allOrders.addAll(orders);
            applyFilter();
        });
    }

    private void applyFilter() {
        List<OrderWithItems> visible = new ArrayList<>();
        for (OrderWithItems item : allOrders) {
            if (selectedFilter == null || OrderStatus.fromCode(item.order.getStatus()) == selectedFilter) {
                visible.add(item);
            }
        }
        adapter.submitList(visible);

        boolean empty = visible.isEmpty();
        binding.llOrdersEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty && selectedFilter != null) {
            binding.tvOrdersEmptyTitle.setText("Không có đơn hàng nào");
            binding.tvOrdersEmptySubtitle.setText(
                    "Các đơn ở trạng thái \"" + selectedFilter.getLabel() + "\" sẽ hiển thị tại đây."
            );
        } else if (empty) {
            binding.tvOrdersEmptyTitle.setText(R.string.empty_orders);
            binding.tvOrdersEmptySubtitle.setText(
                    "Lịch sử mua sắm và trạng thái giao hàng của bạn sẽ xuất hiện tại đây."
            );
        }
    }

    private String labelFor(OrderStatus filter) {
        return filter == null ? getString(R.string.category_all) : filter.getLabel();
    }
}
