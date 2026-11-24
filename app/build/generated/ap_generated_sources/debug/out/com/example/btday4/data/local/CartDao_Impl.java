package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.SQLiteConnection;
import androidx.sqlite.SQLiteStatement;
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
public final class CartDao_Impl implements CartDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<CartItemEntity> __insertAdapterOfCartItemEntity;

  public CartDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfCartItemEntity = new EntityInsertAdapter<CartItemEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cart_items` (`product_id`,`quantity`,`updated_at`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final CartItemEntity entity) {
        if (entity.getProductId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getProductId());
        }
        statement.bindLong(2, entity.getQuantity());
        statement.bindLong(3, entity.getUpdatedAt());
      }
    };
  }

  @Override
  public void upsert(final CartItemEntity item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfCartItemEntity.insert(_connection, item);
      return null;
    });
  }

  @Override
  public List<CartDao.CartRow> getAllWithProduct() {
    final String _sql = "SELECT * FROM cart_items ORDER BY updated_at DESC";
    return DBUtil.performBlocking(__db, true, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfProductId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "product_id");
        final int _columnIndexOfQuantity = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "quantity");
        final int _columnIndexOfUpdatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "updated_at");
        final ArrayMap<String, Product> _collectionProduct = new ArrayMap<String, Product>();
        while (_stmt.step()) {
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfProductId)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfProductId);
          }
          if (_tmpKey != null) {
            _collectionProduct.put(_tmpKey, null);
          }
        }
        _stmt.reset();
        __fetchRelationshipproductsAscomExampleBtday4DataLocalProduct(_connection, _collectionProduct);
        final List<CartDao.CartRow> _result = new ArrayList<CartDao.CartRow>();
        while (_stmt.step()) {
          final CartDao.CartRow _item;
          final CartItemEntity _tmpEntry;
          if (!(_stmt.isNull(_columnIndexOfProductId) && _stmt.isNull(_columnIndexOfQuantity) && _stmt.isNull(_columnIndexOfUpdatedAt))) {
            final String _tmpProductId;
            if (_stmt.isNull(_columnIndexOfProductId)) {
              _tmpProductId = null;
            } else {
              _tmpProductId = _stmt.getText(_columnIndexOfProductId);
            }
            final int _tmpQuantity;
            _tmpQuantity = (int) (_stmt.getLong(_columnIndexOfQuantity));
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt);
            _tmpEntry = new CartItemEntity(_tmpProductId,_tmpQuantity,_tmpUpdatedAt);
          } else {
            _tmpEntry = null;
          }
          final Product _tmpProduct;
          final String _tmpKey_1;
          if (_stmt.isNull(_columnIndexOfProductId)) {
            _tmpKey_1 = null;
          } else {
            _tmpKey_1 = _stmt.getText(_columnIndexOfProductId);
          }
          if (_tmpKey_1 != null) {
            _tmpProduct = _collectionProduct.get(_tmpKey_1);
          } else {
            _tmpProduct = null;
          }
          _item = new CartDao.CartRow();
          _item.entry = _tmpEntry;
          _item.product = _tmpProduct;
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public CartItemEntity findByProductId(final String productId) {
    final String _sql = "SELECT * FROM cart_items WHERE product_id = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (productId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, productId);
        }
        final int _columnIndexOfProductId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "product_id");
        final int _columnIndexOfQuantity = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "quantity");
        final int _columnIndexOfUpdatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "updated_at");
        final CartItemEntity _result;
        if (_stmt.step()) {
          final String _tmpProductId;
          if (_stmt.isNull(_columnIndexOfProductId)) {
            _tmpProductId = null;
          } else {
            _tmpProductId = _stmt.getText(_columnIndexOfProductId);
          }
          final int _tmpQuantity;
          _tmpQuantity = (int) (_stmt.getLong(_columnIndexOfQuantity));
          final long _tmpUpdatedAt;
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt);
          _result = new CartItemEntity(_tmpProductId,_tmpQuantity,_tmpUpdatedAt);
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
  public int getTotalQuantity() {
    final String _sql = "SELECT COALESCE(SUM(quantity), 0) FROM cart_items";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
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
  public void deleteByProductId(final String productId) {
    final String _sql = "DELETE FROM cart_items WHERE product_id = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (productId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, productId);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void clearAll() {
    final String _sql = "DELETE FROM cart_items";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private void __fetchRelationshipproductsAscomExampleBtday4DataLocalProduct(
      @NonNull final SQLiteConnection _connection, @NonNull final ArrayMap<String, Product> _map) {
    final Set<String> __mapKeySet = _map.keySet();
    if (__mapKeySet.isEmpty()) {
      return;
    }
    if (_map.size() > 999) {
      RelationUtil.recursiveFetchArrayMap(_map, false, (_tmpMap) -> {
        __fetchRelationshipproductsAscomExampleBtday4DataLocalProduct(_connection, _tmpMap);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = new StringBuilder();
    _stringBuilder.append("SELECT `id`,`name`,`price`,`original_price`,`short_info`,`category`,`image_url`,`brand`,`description`,`specs`,`warranty`,`stock` FROM `products` WHERE `id` IN (");
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
      final int _itemKeyIndex = SQLiteStatementUtil.getColumnIndex(_stmt, "id");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _columnIndexOfId = 0;
      final int _columnIndexOfName = 1;
      final int _columnIndexOfPrice = 2;
      final int _columnIndexOfOriginalPrice = 3;
      final int _columnIndexOfShortInfo = 4;
      final int _columnIndexOfCategory = 5;
      final int _columnIndexOfImageUrl = 6;
      final int _columnIndexOfBrand = 7;
      final int _columnIndexOfDescription = 8;
      final int _columnIndexOfSpecs = 9;
      final int _columnIndexOfWarranty = 10;
      final int _columnIndexOfStock = 11;
      while (_stmt.step()) {
        final String _tmpKey;
        if (_stmt.isNull(_itemKeyIndex)) {
          _tmpKey = null;
        } else {
          _tmpKey = _stmt.getText(_itemKeyIndex);
        }
        if (_tmpKey != null) {
          if (_map.containsKey(_tmpKey)) {
            final Product _item_1;
            final String _tmpId;
            if (_stmt.isNull(_columnIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _stmt.getText(_columnIndexOfId);
            }
            final String _tmpName;
            if (_stmt.isNull(_columnIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _stmt.getText(_columnIndexOfName);
            }
            final String _tmpPrice;
            if (_stmt.isNull(_columnIndexOfPrice)) {
              _tmpPrice = null;
            } else {
              _tmpPrice = _stmt.getText(_columnIndexOfPrice);
            }
            final String _tmpOriginalPrice;
            if (_stmt.isNull(_columnIndexOfOriginalPrice)) {
              _tmpOriginalPrice = null;
            } else {
              _tmpOriginalPrice = _stmt.getText(_columnIndexOfOriginalPrice);
            }
            final String _tmpShortInfo;
            if (_stmt.isNull(_columnIndexOfShortInfo)) {
              _tmpShortInfo = null;
            } else {
              _tmpShortInfo = _stmt.getText(_columnIndexOfShortInfo);
            }
            final String _tmpCategory;
            if (_stmt.isNull(_columnIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _stmt.getText(_columnIndexOfCategory);
            }
            final String _tmpImageUrl;
            if (_stmt.isNull(_columnIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _stmt.getText(_columnIndexOfImageUrl);
            }
            final String _tmpBrand;
            if (_stmt.isNull(_columnIndexOfBrand)) {
              _tmpBrand = null;
            } else {
              _tmpBrand = _stmt.getText(_columnIndexOfBrand);
            }
            final String _tmpDescription;
            if (_stmt.isNull(_columnIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _stmt.getText(_columnIndexOfDescription);
            }
            final String _tmpSpecs;
            if (_stmt.isNull(_columnIndexOfSpecs)) {
              _tmpSpecs = null;
            } else {
              _tmpSpecs = _stmt.getText(_columnIndexOfSpecs);
            }
            final String _tmpWarranty;
            if (_stmt.isNull(_columnIndexOfWarranty)) {
              _tmpWarranty = null;
            } else {
              _tmpWarranty = _stmt.getText(_columnIndexOfWarranty);
            }
            final int _tmpStock;
            _tmpStock = (int) (_stmt.getLong(_columnIndexOfStock));
            _item_1 = new Product(_tmpId,_tmpName,_tmpPrice,_tmpOriginalPrice,_tmpShortInfo,_tmpCategory,_tmpImageUrl,_tmpBrand,_tmpDescription,_tmpSpecs,_tmpWarranty,_tmpStock);
            _map.put(_tmpKey, _item_1);
          }
        }
      }
    } finally {
      _stmt.close();
    }
  }
}
