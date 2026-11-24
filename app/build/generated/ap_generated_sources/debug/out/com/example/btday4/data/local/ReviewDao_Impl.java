package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.example.btday4.model.UserReviewWithProduct;
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
public final class ReviewDao_Impl implements ReviewDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<ReviewEntity> __insertAdapterOfReviewEntity;

  private final EntityInsertAdapter<ReviewEntity> __insertAdapterOfReviewEntity_1;

  public ReviewDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfReviewEntity = new EntityInsertAdapter<ReviewEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `reviews` (`id`,`product_id`,`username`,`author_name`,`rating`,`comment`,`created_at`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final ReviewEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getId());
        }
        if (entity.getProductId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getProductId());
        }
        if (entity.getUsername() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getUsername());
        }
        if (entity.getAuthorName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getAuthorName());
        }
        statement.bindLong(5, entity.getRating());
        if (entity.getComment() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getComment());
        }
        statement.bindLong(7, entity.getCreatedAt());
      }
    };
    this.__insertAdapterOfReviewEntity_1 = new EntityInsertAdapter<ReviewEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `reviews` (`id`,`product_id`,`username`,`author_name`,`rating`,`comment`,`created_at`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final ReviewEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getId());
        }
        if (entity.getProductId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getProductId());
        }
        if (entity.getUsername() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getUsername());
        }
        if (entity.getAuthorName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getAuthorName());
        }
        statement.bindLong(5, entity.getRating());
        if (entity.getComment() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getComment());
        }
        statement.bindLong(7, entity.getCreatedAt());
      }
    };
  }

  @Override
  public void upsert(final ReviewEntity review) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfReviewEntity.insert(_connection, review);
      return null;
    });
  }

  @Override
  public void insertMissing(final List<ReviewEntity> reviews) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfReviewEntity_1.insert(_connection, reviews);
      return null;
    });
  }

  @Override
  public List<ReviewEntity> getByProductId(final String productId) {
    final String _sql = "SELECT * FROM reviews WHERE product_id = ? ORDER BY created_at DESC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (productId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, productId);
        }
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfProductId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "product_id");
        final int _columnIndexOfUsername = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "username");
        final int _columnIndexOfAuthorName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "author_name");
        final int _columnIndexOfRating = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "rating");
        final int _columnIndexOfComment = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "comment");
        final int _columnIndexOfCreatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final List<ReviewEntity> _result = new ArrayList<ReviewEntity>();
        while (_stmt.step()) {
          final ReviewEntity _item;
          final String _tmpId;
          if (_stmt.isNull(_columnIndexOfId)) {
            _tmpId = null;
          } else {
            _tmpId = _stmt.getText(_columnIndexOfId);
          }
          final String _tmpProductId;
          if (_stmt.isNull(_columnIndexOfProductId)) {
            _tmpProductId = null;
          } else {
            _tmpProductId = _stmt.getText(_columnIndexOfProductId);
          }
          final String _tmpUsername;
          if (_stmt.isNull(_columnIndexOfUsername)) {
            _tmpUsername = null;
          } else {
            _tmpUsername = _stmt.getText(_columnIndexOfUsername);
          }
          final String _tmpAuthorName;
          if (_stmt.isNull(_columnIndexOfAuthorName)) {
            _tmpAuthorName = null;
          } else {
            _tmpAuthorName = _stmt.getText(_columnIndexOfAuthorName);
          }
          final int _tmpRating;
          _tmpRating = (int) (_stmt.getLong(_columnIndexOfRating));
          final String _tmpComment;
          if (_stmt.isNull(_columnIndexOfComment)) {
            _tmpComment = null;
          } else {
            _tmpComment = _stmt.getText(_columnIndexOfComment);
          }
          final long _tmpCreatedAt;
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt);
          _item = new ReviewEntity(_tmpId,_tmpProductId,_tmpUsername,_tmpAuthorName,_tmpRating,_tmpComment,_tmpCreatedAt);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public ReviewEntity findByProductAndUser(final String productId, final String username) {
    final String _sql = "SELECT * FROM reviews WHERE product_id = ? AND username = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (productId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, productId);
        }
        _argIndex = 2;
        if (username == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, username);
        }
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfProductId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "product_id");
        final int _columnIndexOfUsername = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "username");
        final int _columnIndexOfAuthorName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "author_name");
        final int _columnIndexOfRating = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "rating");
        final int _columnIndexOfComment = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "comment");
        final int _columnIndexOfCreatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final ReviewEntity _result;
        if (_stmt.step()) {
          final String _tmpId;
          if (_stmt.isNull(_columnIndexOfId)) {
            _tmpId = null;
          } else {
            _tmpId = _stmt.getText(_columnIndexOfId);
          }
          final String _tmpProductId;
          if (_stmt.isNull(_columnIndexOfProductId)) {
            _tmpProductId = null;
          } else {
            _tmpProductId = _stmt.getText(_columnIndexOfProductId);
          }
          final String _tmpUsername;
          if (_stmt.isNull(_columnIndexOfUsername)) {
            _tmpUsername = null;
          } else {
            _tmpUsername = _stmt.getText(_columnIndexOfUsername);
          }
          final String _tmpAuthorName;
          if (_stmt.isNull(_columnIndexOfAuthorName)) {
            _tmpAuthorName = null;
          } else {
            _tmpAuthorName = _stmt.getText(_columnIndexOfAuthorName);
          }
          final int _tmpRating;
          _tmpRating = (int) (_stmt.getLong(_columnIndexOfRating));
          final String _tmpComment;
          if (_stmt.isNull(_columnIndexOfComment)) {
            _tmpComment = null;
          } else {
            _tmpComment = _stmt.getText(_columnIndexOfComment);
          }
          final long _tmpCreatedAt;
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt);
          _result = new ReviewEntity(_tmpId,_tmpProductId,_tmpUsername,_tmpAuthorName,_tmpRating,_tmpComment,_tmpCreatedAt);
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
  public List<UserReviewWithProduct> getUserReviewsWithProduct(final String username) {
    final String _sql = "SELECT r.*, p.name AS product_name, p.image_url AS product_image_url FROM reviews r INNER JOIN products p ON r.product_id = p.id WHERE r.username = ? ORDER BY r.created_at DESC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (username == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, username);
        }
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfProductId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "product_id");
        final int _columnIndexOfUsername = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "username");
        final int _columnIndexOfAuthorName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "author_name");
        final int _columnIndexOfRating = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "rating");
        final int _columnIndexOfComment = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "comment");
        final int _columnIndexOfCreatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final int _columnIndexOfProductName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "product_name");
        final int _columnIndexOfProductImageUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "product_image_url");
        final List<UserReviewWithProduct> _result = new ArrayList<UserReviewWithProduct>();
        while (_stmt.step()) {
          final UserReviewWithProduct _item;
          final ReviewEntity _tmpReview;
          if (!(_stmt.isNull(_columnIndexOfId) && _stmt.isNull(_columnIndexOfProductId) && _stmt.isNull(_columnIndexOfUsername) && _stmt.isNull(_columnIndexOfAuthorName) && _stmt.isNull(_columnIndexOfRating) && _stmt.isNull(_columnIndexOfComment) && _stmt.isNull(_columnIndexOfCreatedAt))) {
            final String _tmpId;
            if (_stmt.isNull(_columnIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _stmt.getText(_columnIndexOfId);
            }
            final String _tmpProductId;
            if (_stmt.isNull(_columnIndexOfProductId)) {
              _tmpProductId = null;
            } else {
              _tmpProductId = _stmt.getText(_columnIndexOfProductId);
            }
            final String _tmpUsername;
            if (_stmt.isNull(_columnIndexOfUsername)) {
              _tmpUsername = null;
            } else {
              _tmpUsername = _stmt.getText(_columnIndexOfUsername);
            }
            final String _tmpAuthorName;
            if (_stmt.isNull(_columnIndexOfAuthorName)) {
              _tmpAuthorName = null;
            } else {
              _tmpAuthorName = _stmt.getText(_columnIndexOfAuthorName);
            }
            final int _tmpRating;
            _tmpRating = (int) (_stmt.getLong(_columnIndexOfRating));
            final String _tmpComment;
            if (_stmt.isNull(_columnIndexOfComment)) {
              _tmpComment = null;
            } else {
              _tmpComment = _stmt.getText(_columnIndexOfComment);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt);
            _tmpReview = new ReviewEntity(_tmpId,_tmpProductId,_tmpUsername,_tmpAuthorName,_tmpRating,_tmpComment,_tmpCreatedAt);
          } else {
            _tmpReview = null;
          }
          _item = new UserReviewWithProduct();
          if (_stmt.isNull(_columnIndexOfProductName)) {
            _item.productName = null;
          } else {
            _item.productName = _stmt.getText(_columnIndexOfProductName);
          }
          if (_stmt.isNull(_columnIndexOfProductImageUrl)) {
            _item.productImageUrl = null;
          } else {
            _item.productImageUrl = _stmt.getText(_columnIndexOfProductImageUrl);
          }
          _item.review = _tmpReview;
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int countByUsername(final String username) {
    final String _sql = "SELECT COUNT(*) FROM reviews WHERE username = ?";
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
  public int countAll() {
    final String _sql = "SELECT COUNT(*) FROM reviews";
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
  public void deleteById(final String reviewId) {
    final String _sql = "DELETE FROM reviews WHERE id = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (reviewId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, reviewId);
        }
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
}
