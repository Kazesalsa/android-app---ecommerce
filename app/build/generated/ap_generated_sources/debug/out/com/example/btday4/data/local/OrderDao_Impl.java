package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.SQLiteConnectionUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.SQLiteConnection;
import androidx.sqlite.SQLiteStatement;
import com.example.btday4.model.OrderWithItems;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlin.Unit;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class OrderDao_Impl implements OrderDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<OrderEntity> __insertAdapterOfOrderEntity;

  private final EntityInsertAdapter<OrderItemEntity> __insertAdapterOfOrderItemEntity;

  public OrderDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfOrderEntity = new EntityInsertAdapter<OrderEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `orders` (`id`,`username`,`recipient_name`,`recipient_phone`,`shipping_address`,`note`,`payment_method`,`subtotal_amount`,`shipping_fee`,`total_amount`,`status`,`created_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final OrderEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getId());
        }
        if (entity.getUsername() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getUsername());
        }
        if (entity.getRecipientName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getRecipientName());
        }
        if (entity.getRecipientPhone() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getRecipientPhone());
        }
        if (entity.getShippingAddress() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getShippingAddress());
        }
        if (entity.getNote() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getNote());
        }
        if (entity.getPaymentMethod() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getPaymentMethod());
        }
        statement.bindLong(8, entity.getSubtotalAmount());
        statement.bindLong(9, entity.getShippingFee());
        statement.bindLong(10, entity.getTotalAmount());
        if (entity.getStatus() == null) {
          statement.bindNull(11);
        } else {
          statement.bindText(11, entity.getStatus());
        }
        statement.bindLong(12, entity.getCreatedAt());
      }
    };
    this.__insertAdapterOfOrderItemEntity = new EntityInsertAdapter<OrderItemEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `order_items` (`id`,`order_id`,`product_id`,`product_name`,`product_image_url`,`unit_price`,`quantity`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final OrderItemEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getOrderId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getOrderId());
        }
        if (entity.getProductId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getProductId());
        }
        if (entity.getProductName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getProductName());
        }
        if (entity.getProductImageUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getProductImageUrl());
        }
        statement.bindLong(6, entity.getUnitPrice());
        statement.bindLong(7, entity.getQuantity());
      }
    };
  }

  @Override
  public void insertOrder(final OrderEntity order) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfOrderEntity.insert(_connection, order);
      return null;
    });
  }

  @Override
  public void insertOrderItems(final List<OrderItemEntity> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfOrderItemEntity.insert(_connection, items);
      return null;
    });
  }

  @Override
  public List<OrderWithItems> getOrdersByUsername(final String username) {
    final String _sql = "SELECT * FROM orders WHERE username = ? ORDER BY created_at DESC";
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (username == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, username);
        }
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfUsername = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "username");
        final int _columnIndexOfRecipientName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "recipient_name");
        final int _columnIndexOfRecipientPhone = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "recipient_phone");
        final int _columnIndexOfShippingAddress = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "shipping_address");
        final int _columnIndexOfNote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "note");
        final int _columnIndexOfPaymentMethod = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "payment_method");
        final int _columnIndexOfSubtotalAmount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "subtotal_amount");
        final int _columnIndexOfShippingFee = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "shipping_fee");
        final int _columnIndexOfTotalAmount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "total_amount");
        final int _columnIndexOfStatus = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "status");
        final int _columnIndexOfCreatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final ArrayMap<String, ArrayList<OrderItemEntity>> _collectionItems = new ArrayMap<String, ArrayList<OrderItemEntity>>();
        while (_stmt.step()) {
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfId);
          }
          if (_tmpKey != null) {
            if (!_collectionItems.containsKey(_tmpKey)) {
              _collectionItems.put(_tmpKey, new ArrayList<OrderItemEntity>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshiporderItemsAscomExampleBtday4DataLocalOrderItemEntity(_connection, _collectionItems);
        final List<OrderWithItems> _result = new ArrayList<OrderWithItems>();
        while (_stmt.step()) {
          final OrderWithItems _item;
          final OrderEntity _tmpOrder;
          if (!(_stmt.isNull(_columnIndexOfId) && _stmt.isNull(_columnIndexOfUsername) && _stmt.isNull(_columnIndexOfRecipientName) && _stmt.isNull(_columnIndexOfRecipientPhone) && _stmt.isNull(_columnIndexOfShippingAddress) && _stmt.isNull(_columnIndexOfNote) && _stmt.isNull(_columnIndexOfPaymentMethod) && _stmt.isNull(_columnIndexOfSubtotalAmount) && _stmt.isNull(_columnIndexOfShippingFee) && _stmt.isNull(_columnIndexOfTotalAmount) && _stmt.isNull(_columnIndexOfStatus) && _stmt.isNull(_columnIndexOfCreatedAt))) {
            final String _tmpId;
            if (_stmt.isNull(_columnIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _stmt.getText(_columnIndexOfId);
            }
            final String _tmpUsername;
            if (_stmt.isNull(_columnIndexOfUsername)) {
              _tmpUsername = null;
            } else {
              _tmpUsername = _stmt.getText(_columnIndexOfUsername);
            }
            final String _tmpRecipientName;
            if (_stmt.isNull(_columnIndexOfRecipientName)) {
              _tmpRecipientName = null;
            } else {
              _tmpRecipientName = _stmt.getText(_columnIndexOfRecipientName);
            }
            final String _tmpRecipientPhone;
            if (_stmt.isNull(_columnIndexOfRecipientPhone)) {
              _tmpRecipientPhone = null;
            } else {
              _tmpRecipientPhone = _stmt.getText(_columnIndexOfRecipientPhone);
            }
            final String _tmpShippingAddress;
            if (_stmt.isNull(_columnIndexOfShippingAddress)) {
              _tmpShippingAddress = null;
            } else {
              _tmpShippingAddress = _stmt.getText(_columnIndexOfShippingAddress);
            }
            final String _tmpNote;
            if (_stmt.isNull(_columnIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _stmt.getText(_columnIndexOfNote);
            }
            final String _tmpPaymentMethod;
            if (_stmt.isNull(_columnIndexOfPaymentMethod)) {
              _tmpPaymentMethod = null;
            } else {
              _tmpPaymentMethod = _stmt.getText(_columnIndexOfPaymentMethod);
            }
            final long _tmpSubtotalAmount;
            _tmpSubtotalAmount = _stmt.getLong(_columnIndexOfSubtotalAmount);
            final long _tmpShippingFee;
            _tmpShippingFee = _stmt.getLong(_columnIndexOfShippingFee);
            final long _tmpTotalAmount;
            _tmpTotalAmount = _stmt.getLong(_columnIndexOfTotalAmount);
            final String _tmpStatus;
            if (_stmt.isNull(_columnIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _stmt.getText(_columnIndexOfStatus);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt);
            _tmpOrder = new OrderEntity(_tmpId,_tmpUsername,_tmpRecipientName,_tmpRecipientPhone,_tmpShippingAddress,_tmpNote,_tmpPaymentMethod,_tmpSubtotalAmount,_tmpShippingFee,_tmpTotalAmount,_tmpStatus,_tmpCreatedAt);
          } else {
            _tmpOrder = null;
          }
          final ArrayList<OrderItemEntity> _tmpItemsCollection;
          final String _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getText(_columnIndexOfId);
          }
          if (_tmpKey_1 != null) {
            _tmpItemsCollection = _collectionItems.get(_tmpKey_1);
          } else {
            _tmpItemsCollection = new ArrayList<OrderItemEntity>();
          }
          _item = new OrderWithItems();
          _item.order = _tmpOrder;
          _item.items = _tmpItemsCollection;
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public OrderWithItems findOrderWithItemsById(final String orderId) {
    final String _sql = "SELECT * FROM orders WHERE id = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (orderId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, orderId);
        }
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfUsername = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "username");
        final int _columnIndexOfRecipientName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "recipient_name");
        final int _columnIndexOfRecipientPhone = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "recipient_phone");
        final int _columnIndexOfShippingAddress = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "shipping_address");
        final int _columnIndexOfNote = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "note");
        final int _columnIndexOfPaymentMethod = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "payment_method");
        final int _columnIndexOfSubtotalAmount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "subtotal_amount");
        final int _columnIndexOfShippingFee = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "shipping_fee");
        final int _columnIndexOfTotalAmount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "total_amount");
        final int _columnIndexOfStatus = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "status");
        final int _columnIndexOfCreatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final ArrayMap<String, ArrayList<OrderItemEntity>> _collectionItems = new ArrayMap<String, ArrayList<OrderItemEntity>>();
        while (_stmt.step()) {
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfId);
          }
          if (_tmpKey != null) {
            if (!_collectionItems.containsKey(_tmpKey)) {
              _collectionItems.put(_tmpKey, new ArrayList<OrderItemEntity>());
            }
          }
        }
        _stmt.reset();
        __fetchRelationshiporderItemsAscomExampleBtday4DataLocalOrderItemEntity(_connection, _collectionItems);
        final OrderWithItems _result;
        if (_stmt.step()) {
          final OrderEntity _tmpOrder;
          if (!(_stmt.isNull(_columnIndexOfId) && _stmt.isNull(_columnIndexOfUsername) && _stmt.isNull(_columnIndexOfRecipientName) && _stmt.isNull(_columnIndexOfRecipientPhone) && _stmt.isNull(_columnIndexOfShippingAddress) && _stmt.isNull(_columnIndexOfNote) && _stmt.isNull(_columnIndexOfPaymentMethod) && _stmt.isNull(_columnIndexOfSubtotalAmount) && _stmt.isNull(_columnIndexOfShippingFee) && _stmt.isNull(_columnIndexOfTotalAmount) && _stmt.isNull(_columnIndexOfStatus) && _stmt.isNull(_columnIndexOfCreatedAt))) {
            final String _tmpId;
            if (_stmt.isNull(_columnIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _stmt.getText(_columnIndexOfId);
            }
            final String _tmpUsername;
            if (_stmt.isNull(_columnIndexOfUsername)) {
              _tmpUsername = null;
            } else {
              _tmpUsername = _stmt.getText(_columnIndexOfUsername);
            }
            final String _tmpRecipientName;
            if (_stmt.isNull(_columnIndexOfRecipientName)) {
              _tmpRecipientName = null;
            } else {
              _tmpRecipientName = _stmt.getText(_columnIndexOfRecipientName);
            }
            final String _tmpRecipientPhone;
            if (_stmt.isNull(_columnIndexOfRecipientPhone)) {
              _tmpRecipientPhone = null;
            } else {
              _tmpRecipientPhone = _stmt.getText(_columnIndexOfRecipientPhone);
            }
            final String _tmpShippingAddress;
            if (_stmt.isNull(_columnIndexOfShippingAddress)) {
              _tmpShippingAddress = null;
            } else {
              _tmpShippingAddress = _stmt.getText(_columnIndexOfShippingAddress);
            }
            final String _tmpNote;
            if (_stmt.isNull(_columnIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _stmt.getText(_columnIndexOfNote);
            }
            final String _tmpPaymentMethod;
            if (_stmt.isNull(_columnIndexOfPaymentMethod)) {
              _tmpPaymentMethod = null;
            } else {
              _tmpPaymentMethod = _stmt.getText(_columnIndexOfPaymentMethod);
            }
            final long _tmpSubtotalAmount;
            _tmpSubtotalAmount = _stmt.getLong(_columnIndexOfSubtotalAmount);
            final long _tmpShippingFee;
            _tmpShippingFee = _stmt.getLong(_columnIndexOfShippingFee);
            final long _tmpTotalAmount;
            _tmpTotalAmount = _stmt.getLong(_columnIndexOfTotalAmount);
            final String _tmpStatus;
            if (_stmt.isNull(_columnIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _stmt.getText(_columnIndexOfStatus);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt);
            _tmpOrder = new OrderEntity(_tmpId,_tmpUsername,_tmpRecipientName,_tmpRecipientPhone,_tmpShippingAddress,_tmpNote,_tmpPaymentMethod,_tmpSubtotalAmount,_tmpShippingFee,_tmpTotalAmount,_tmpStatus,_tmpCreatedAt);
          } else {
            _tmpOrder = null;
          }
          final ArrayList<OrderItemEntity> _tmpItemsCollection;
          final String _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getText(_columnIndexOfId);
          }
          if (_tmpKey_1 != null) {
            _tmpItemsCollection = _collectionItems.get(_tmpKey_1);
          } else {
            _tmpItemsCollection = new ArrayList<OrderItemEntity>();
          }
          _result = new OrderWithItems();
          _result.order = _tmpOrder;
          _result.items = _tmpItemsCollection;
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int countByUsername(final String username) {
    final String _sql = "SELECT COUNT(*) FROM orders WHERE username = ?";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (username == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, username);
        }
        final int _result;
        if (_stmt.step()) {
          _result = (int) (_stmt.getLong(0));
        } else {
          _result = 0;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void updateStatus(final String orderId, final String status) {
    final String _sql = "UPDATE orders SET status = ? WHERE id = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (status == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, status);
        }
        _argIndex = 2;
        if (orderId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, orderId);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int transitionStatus(final String orderId, final String status,
      final List<String> fromStatuses) {
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("UPDATE orders SET status = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" WHERE id = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND status IN (");
    final int _inputSize = fromStatuses == null ? 1 : fromStatuses.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (status == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, status);
        }
        _argIndex = 2;
        if (orderId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, orderId);
        }
        _argIndex = 3;
        if (fromStatuses == null) {
          _stmt.bindNull(_argIndex);
        } else {
          for (String _item : fromStatuses) {
            if (_item == null) {
              _stmt.bindNull(_argIndex);
            } else {
              _stmt.bindText(_argIndex, _item);
            }
            _argIndex++;
          }
        }
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private void __fetchRelationshiporderItemsAscomExampleBtday4DataLocalOrderItemEntity(
      @NonNull final SQLiteConnection _connection,
      @NonNull final ArrayMap<String, ArrayList<OrderItemEntity>> _map) {
    final Set<String> __mapKeySet = _map.keySet();
    if (__mapKeySet.isEmpty()) {
      return;
    }
    if (_map.size() > 999) {
      RelationUtil.recursiveFetchArrayMap(_map, true, (_tmpMap) -> {
        __fetchRelationshiporderItemsAscomExampleBtday4DataLocalOrderItemEntity(_connection, _tmpMap);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT `id`,`order_id`,`product_id`,`product_name`,`product_image_url`,`unit_price`,`quantity` FROM `order_items` WHERE `order_id` IN (");
    final int _inputSize = __mapKeySet == null ? 1 : __mapKeySet.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final SQLiteStatement _stmt = _connection.prepare(_sql);
    int _argIndex = 1;
    if (__mapKeySet == null) {
      _stmt.bindNull(_argIndex);
    } else {
      for (String _item : __mapKeySet) {
        if (_item == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, _item);
        }
        _argIndex++;
      }
    }
    try {
      final int _itemKeyIndex = SQLiteStatementUtil.getColumnIndex(_stmt, "order_id");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _columnIndexOfId = 0;
      final int _columnIndexOfOrderId = 1;
      final int _columnIndexOfProductId = 2;
      final int _columnIndexOfProductName = 3;
      final int _columnIndexOfProductImageUrl = 4;
      final int _columnIndexOfUnitPrice = 5;
      final int _columnIndexOfQuantity = 6;
      while (_stmt.step()) {
        final String _tmpKey;
        if (_stmt.isNull(_itemKeyIndex)) {
          _tmpKey = null;
        } else {
          _tmpKey = _stmt.getText(_itemKeyIndex);
        }
        if (_tmpKey != null) {
          final ArrayList<OrderItemEntity> _tmpRelation = _map.get(_tmpKey);
          if (_tmpRelation != null) {
            final OrderItemEntity _item_1;
            final String _tmpOrderId;
            if (_stmt.isNull(_columnIndexOfOrderId)) {
              _tmpOrderId = null;
            } else {
              _tmpOrderId = _stmt.getText(_columnIndexOfOrderId);
            }
            final String _tmpProductId;
            if (_stmt.isNull(_columnIndexOfProductId)) {
              _tmpProductId = null;
            } else {
              _tmpProductId = _stmt.getText(_columnIndexOfProductId);
            }
            final String _tmpProductName;
            if (_stmt.isNull(_columnIndexOfProductName)) {
              _tmpProductName = null;
            } else {
              _tmpProductName = _stmt.getText(_columnIndexOfProductName);
            }
            final String _tmpProductImageUrl;
            if (_stmt.isNull(_columnIndexOfProductImageUrl)) {
              _tmpProductImageUrl = null;
            } else {
              _tmpProductImageUrl = _stmt.getText(_columnIndexOfProductImageUrl);
            }
            final long _tmpUnitPrice;
            _tmpUnitPrice = _stmt.getLong(_columnIndexOfUnitPrice);
            final int _tmpQuantity;
            _tmpQuantity = (int) (_stmt.getLong(_columnIndexOfQuantity));
            _item_1 = new OrderItemEntity(_tmpOrderId,_tmpProductId,_tmpProductName,_tmpProductImageUrl,_tmpUnitPrice,_tmpQuantity);
            final long _tmpId;
            _tmpId = _stmt.getLong(_columnIndexOfId);
            _item_1.setId(_tmpId);
            _tmpRelation.add(_item_1);
          }
        }
      }
    } finally {
      _stmt.close();
    }
  }
}
