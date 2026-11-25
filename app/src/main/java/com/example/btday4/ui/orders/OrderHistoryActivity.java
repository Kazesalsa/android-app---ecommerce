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
import com.example.btday4.ui.main.MainActivity;
import com.example.btday4.util.Constants;
import com.example.btday4.util.UiUtils;
import com.example.btday4.util.ValidationUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the signed-in user's order history with 3 streamlined status groups:
 * Chờ thanh toán QR, Đang giao hàng, and Đã hủy.
 */
public class OrderHistoryActivity extends AppCompatActivity {

    /**
     * Tab order shown to the user; {@code null} means "Tất cả".
     * {@link OrderStatus#PROCESSING} groups active/paid orders (PROCESSING, PAID, DELIVERED).
     */
    private static final OrderStatus[] TAB_FILTERS = {
            null,
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.PROCESSING,
            OrderStatus.CANCELLED
    };

    private ActivityOrderHistoryBinding binding;
    private OrderAdapter adapter;
    private UserSession userSession;
    private final List<ItemOrderTabBinding> tabs = new ArrayList<>();
    private final List<OrderWithItems> allOrders = new ArrayList<>();
    private OrderStatus selectedFilter = null;
    private boolean hasLoadedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.setStatusBarLightIcons(this, true);
        UiUtils.applyEdgeToEdgeBars(binding.orderHistoryRoot, binding.toolbarOrders, null);

        userSession = new UserSession(this);
        binding.toolbarOrders.setNavigationOnClickListener(v -> finish());

        String initialStatusExtra = getIntent().getStringExtra(Constants.EXTRA_ORDER_STATUS_FILTER);
        if (ValidationUtils.isNotBlank(initialStatusExtra)) {
            OrderStatus parsed = OrderStatus.fromCode(initialStatusExtra);
            if (parsed == OrderStatus.PAID || parsed == OrderStatus.DELIVERED) {
                parsed = OrderStatus.PROCESSING;
            }
            for (OrderStatus candidate : TAB_FILTERS) {
                if (candidate == parsed) {
                    selectedFilter = parsed;
                    break;
                }
            }
        }

        adapter = new OrderAdapter(orderWithItems -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra(Constants.EXTRA_ORDER_ID, orderWithItems.order.getId());
            intent.putExtra(Constants.EXTRA_IS_NEW_ORDER, false);
            startActivity(intent);
        });
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);

        binding.btnOrdersExploreStore.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(Constants.EXTRA_OPEN_PRODUCTS, true);
            startActivity(intent);
            finish();
        });

        buildTabs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void buildTabs() {
        LayoutInflater inflater = LayoutInflater.from(this);
        int initialIndex = 0;
        for (int i = 0; i < TAB_FILTERS.length; i++) {
            OrderStatus filter = TAB_FILTERS[i];
            if (filter == selectedFilter) {
                initialIndex = i;
            }
            ItemOrderTabBinding tab = ItemOrderTabBinding.inflate(inflater, binding.llOrderTabs, false);
            tab.tvTabLabel.setText(labelFor(filter));
            tab.getRoot().setOnClickListener(v -> selectFilter(filter, tab.getRoot()));
            binding.llOrderTabs.addView(tab.getRoot());
            tabs.add(tab);
        }
        styleTabs();
        if (initialIndex > 0 && initialIndex < tabs.size()) {
            final View activeTabView = tabs.get(initialIndex).getRoot();
            binding.scrollOrderTabs.post(() -> binding.scrollOrderTabs.smoothScrollTo(
                    Math.max(0, activeTabView.getLeft() - (binding.scrollOrderTabs.getWidth() - activeTabView.getWidth()) / 2),
                    0
            ));
        }
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
        if (hasLoadedOnce) {
            applyFilter();
        }
    }

    private void styleTabs() {
        for (int i = 0; i < tabs.size(); i++) {
            ItemOrderTabBinding tab = tabs.get(i);
            UiUtils.styleUnderlineTab(tab.tvTabLabel, tab.viewTabIndicator, TAB_FILTERS[i] == selectedFilter);
        }
    }

    private void loadOrders() {
        if (!hasLoadedOnce) {
            binding.progressOrders.setVisibility(View.VISIBLE);
            binding.llOrdersEmpty.setVisibility(View.GONE);
        }
        OrderRepository.getOrdersForUser(this, userSession.getUsername(), orders -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            hasLoadedOnce = true;
            binding.progressOrders.setVisibility(View.GONE);
            allOrders.clear();
            allOrders.addAll(orders);
            applyFilter();
        });
    }

    private void applyFilter() {
        List<OrderWithItems> visible = new ArrayList<>();
        for (OrderWithItems item : allOrders) {
            OrderStatus st = OrderStatus.fromCode(item.order.getStatus());
            if (matchesFilter(st, selectedFilter)) {
                visible.add(item);
            }
        }
        adapter.submitList(visible);

        boolean empty = visible.isEmpty();
        binding.llOrdersEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty && selectedFilter != null) {
            binding.tvOrdersEmptyTitle.setText("Không có đơn hàng nào");
            binding.tvOrdersEmptySubtitle.setText(
                    "Các đơn ở trạng thái \"" + labelFor(selectedFilter) + "\" sẽ hiển thị tại đây."
            );
        } else if (empty) {
            binding.tvOrdersEmptyTitle.setText(R.string.empty_orders);
            binding.tvOrdersEmptySubtitle.setText(
                    "Lịch sử mua sắm và trạng thái giao hàng của bạn sẽ xuất hiện tại đây."
            );
        }
    }

    private boolean matchesFilter(OrderStatus orderStatus, OrderStatus filter) {
        if (filter == null) {
            return true;
        }
        if (filter == OrderStatus.PROCESSING) {
            return orderStatus == OrderStatus.PROCESSING
                    || orderStatus == OrderStatus.PAID
                    || orderStatus == OrderStatus.DELIVERED;
        }
        return orderStatus == filter;
    }

    private String labelFor(OrderStatus filter) {
        return filter == null ? getString(R.string.category_all) : filter.getLabel();
    }
}
