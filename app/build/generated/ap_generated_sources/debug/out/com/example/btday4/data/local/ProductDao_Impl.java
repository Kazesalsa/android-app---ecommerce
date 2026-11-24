package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.example.btday4.model.ProductWithRating;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class ProductDao_Impl implements ProductDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<Product> __insertAdapterOfProduct;

  private final EntityDeleteOrUpdateAdapter<Product> __updateAdapterOfProduct;

  public ProductDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfProduct = new EntityInsertAdapter<Product>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `products` (`id`,`name`,`price`,`original_price`,`short_info`,`category`,`image_url`,`brand`,`description`,`specs`,`warranty`,`stock`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Product entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getName());
        }
        if (entity.getPrice() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getPrice());
        }
        if (entity.getOriginalPrice() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getOriginalPrice());
        }
        if (entity.getShortInfo() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getShortInfo());
        }
        if (entity.getCategory() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getCategory());
        }
        if (entity.getImageUrl() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getImageUrl());
        }
        if (entity.getBrand() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getBrand());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(9);
        } else {
          statement.bindText(9, entity.getDescription());
        }
        if (entity.getSpecs() == null) {
          statement.bindNull(10);
        } else {
          statement.bindText(10, entity.getSpecs());
        }
        if (entity.getWarranty() == null) {
          statement.bindNull(11);
        } else {
          statement.bindText(11, entity.getWarranty());
        }
        statement.bindLong(12, entity.getStock());
      }
    };
    this.__updateAdapterOfProduct = new EntityDeleteOrUpdateAdapter<Product>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `products` SET `id` = ?,`name` = ?,`price` = ?,`original_price` = ?,`short_info` = ?,`category` = ?,`image_url` = ?,`brand` = ?,`description` = ?,`specs` = ?,`warranty` = ?,`stock` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Product entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getName());
        }
        if (entity.getPrice() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getPrice());
        }
        if (entity.getOriginalPrice() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getOriginalPrice());
        }
        if (entity.getShortInfo() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getShortInfo());
        }
        if (entity.getCategory() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getCategory());
        }
        if (entity.getImageUrl() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getImageUrl());
        }
        if (entity.getBrand() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getBrand());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(9);
        } else {
          statement.bindText(9, entity.getDescription());
        }
        if (entity.getSpecs() == null) {
          statement.bindNull(10);
        } else {
          statement.bindText(10, entity.getSpecs());
        }
        if (entity.getWarranty() == null) {
          statement.bindNull(11);
        } else {
          statement.bindText(11, entity.getWarranty());
        }
        statement.bindLong(12, entity.getStock());
        if (entity.getId() == null) {
          statement.bindNull(13);
        } else {
          statement.bindText(13, entity.getId());
        }
      }
    };
  }

  @Override
  public void insertMissing(final List<Product> products) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfProduct.insert(_connection, products);
      return null;
    });
  }

  @Override
  public long insertSingle(final Product product) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfProduct.insertAndReturnId(_connection, product);
    });
  }

  @Override
  public void updateAll(final List<Product> products) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfProduct.handleMultiple(_connection, products);
      return null;
    });
  }

  @Override
  public void updateSingle(final Product product) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfProduct.handle(_connection, product);
      return null;
    });
  }

  @Override
  public List<Product> getAll() {
    final String _sql = "SELECT * FROM products";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfPrice = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "price");
        final int _columnIndexOfOriginalPrice = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "original_price");
        final int _columnIndexOfShortInfo = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "short_info");
        final int _columnIndexOfCategory = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "category");
        final int _columnIndexOfImageUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "image_url");
        final int _columnIndexOfBrand = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "brand");
        final int _columnIndexOfDescription = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "description");
        final int _columnIndexOfSpecs = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "specs");
        final int _columnIndexOfWarranty = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "warranty");
        final int _columnIndexOfStock = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "stock");
        final List<Product> _result = new ArrayList<Product>();
        while (_stmt.step()) {
          final Product _item;
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
          _item = new Product(_tmpId,_tmpName,_tmpPrice,_tmpOriginalPrice,_tmpShortInfo,_tmpCategory,_tmpImageUrl,_tmpBrand,_tmpDescription,_tmpSpecs,_tmpWarranty,_tmpStock);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<ProductWithRating> getAllWithRating() {
    final String _sql = "SELECT p.*, COALESCE(AVG(r.rating), 0.0) AS avg_rating, COUNT(r.id) AS review_count FROM products p LEFT JOIN reviews r ON p.id = r.product_id GROUP BY p.id";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfPrice = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "price");
        final int _columnIndexOfOriginalPrice = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "original_price");
        final int _columnIndexOfShortInfo = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "short_info");
        final int _columnIndexOfCategory = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "category");
        final int _columnIndexOfImageUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "image_url");
        final int _columnIndexOfBrand = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "brand");
        final int _columnIndexOfDescription = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "description");
        final int _columnIndexOfSpecs = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "specs");
        final int _columnIndexOfWarranty = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "warranty");
        final int _columnIndexOfStock = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "stock");
        final int _columnIndexOfAvgRating = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "avg_rating");
        final int _columnIndexOfReviewCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "review_count");
        final List<ProductWithRating> _result = new ArrayList<ProductWithRating>();
        while (_stmt.step()) {
          final ProductWithRating _item;
          final Product _tmpProduct;
          if (!(_stmt.isNull(_columnIndexOfId) && _stmt.isNull(_columnIndexOfName) && _stmt.isNull(_columnIndexOfPrice) && _stmt.isNull(_columnIndexOfOriginalPrice) && _stmt.isNull(_columnIndexOfShortInfo) && _stmt.isNull(_columnIndexOfCategory) && _stmt.isNull(_columnIndexOfImageUrl) && _stmt.isNull(_columnIndexOfBrand) && _stmt.isNull(_columnIndexOfDescription) && _stmt.isNull(_columnIndexOfSpecs) && _stmt.isNull(_columnIndexOfWarranty) && _stmt.isNull(_columnIndexOfStock))) {
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
            _tmpProduct = new Product(_tmpId,_tmpName,_tmpPrice,_tmpOriginalPrice,_tmpShortInfo,_tmpCategory,_tmpImageUrl,_tmpBrand,_tmpDescription,_tmpSpecs,_tmpWarranty,_tmpStock);
          } else {
            _tmpProduct = null;
          }
          _item = new ProductWithRating();
          _item.avgRating = _stmt.getDouble(_columnIndexOfAvgRating);
          _item.reviewCount = (int) (_stmt.getLong(_columnIndexOfReviewCount));
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
  public Product findById(final String id) {
    final String _sql = "SELECT * FROM products WHERE id = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (id == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, id);
        }
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfPrice = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "price");
        final int _columnIndexOfOriginalPrice = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "original_price");
        final int _columnIndexOfShortInfo = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "short_info");
        final int _columnIndexOfCategory = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "category");
        final int _columnIndexOfImageUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "image_url");
        final int _columnIndexOfBrand = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "brand");
        final int _columnIndexOfDescription = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "description");
        final int _columnIndexOfSpecs = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "specs");
        final int _columnIndexOfWarranty = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "warranty");
        final int _columnIndexOfStock = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "stock");
        final Product _result;
        if (_stmt.step()) {
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
          _result = new Product(_tmpId,_tmpName,_tmpPrice,_tmpOriginalPrice,_tmpShortInfo,_tmpCategory,_tmpImageUrl,_tmpBrand,_tmpDescription,_tmpSpecs,_tmpWarranty,_tmpStock);
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
  public int count() {
    final String _sql = "SELECT COUNT(*) FROM products";
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
  public int countMissingDetails() {
    final String _sql = "SELECT COUNT(*) FROM products WHERE image_url IS NULL OR image_url = '' OR description IS NULL OR description = '' OR specs IS NULL OR specs = ''";
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
