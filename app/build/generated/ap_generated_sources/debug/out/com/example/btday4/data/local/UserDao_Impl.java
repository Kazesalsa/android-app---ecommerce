package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class UserDao_Impl implements UserDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<UserEntity> __insertAdapterOfUserEntity;

  public UserDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfUserEntity = new EntityInsertAdapter<UserEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `users` (`username`,`display_name`,`password`,`phone`,`address`,`login_time`,`email`,`gender`,`birth_date`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final UserEntity entity) {
        if (entity.getUsername() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getUsername());
        }
        if (entity.getDisplayName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getDisplayName());
        }
        if (entity.getPassword() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getPassword());
        }
        if (entity.getPhone() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getPhone());
        }
        if (entity.getAddress() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getAddress());
        }
        if (entity.getLoginTime() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getLoginTime());
        }
        if (entity.getEmail() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getEmail());
        }
        if (entity.getGender() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getGender());
        }
        if (entity.getBirthDate() == null) {
          statement.bindNull(9);
        } else {
          statement.bindText(9, entity.getBirthDate());
        }
      }
    };
  }

  @Override
  public void upsert(final UserEntity user) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfUserEntity.insert(_connection, user);
      return null;
    });
  }

  @Override
  public UserEntity findByUsername(final String username) {
    final String _sql = "SELECT * FROM users WHERE username = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (username == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, username);
        }
        final int _columnIndexOfUsername = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "username");
        final int _columnIndexOfDisplayName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "display_name");
        final int _columnIndexOfPassword = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "password");
        final int _columnIndexOfPhone = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "phone");
        final int _columnIndexOfAddress = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "address");
        final int _columnIndexOfLoginTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "login_time");
        final int _columnIndexOfEmail = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "email");
        final int _columnIndexOfGender = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "gender");
        final int _columnIndexOfBirthDate = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "birth_date");
        final UserEntity _result;
        if (_stmt.step()) {
          final String _tmpUsername;
          if (_stmt.isNull(_columnIndexOfUsername)) {
            _tmpUsername = null;
          } else {
            _tmpUsername = _stmt.getText(_columnIndexOfUsername);
          }
          final String _tmpDisplayName;
          if (_stmt.isNull(_columnIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName);
          }
          final String _tmpPassword;
          if (_stmt.isNull(_columnIndexOfPassword)) {
            _tmpPassword = null;
          } else {
            _tmpPassword = _stmt.getText(_columnIndexOfPassword);
          }
          final String _tmpPhone;
          if (_stmt.isNull(_columnIndexOfPhone)) {
            _tmpPhone = null;
          } else {
            _tmpPhone = _stmt.getText(_columnIndexOfPhone);
          }
          final String _tmpAddress;
          if (_stmt.isNull(_columnIndexOfAddress)) {
            _tmpAddress = null;
          } else {
            _tmpAddress = _stmt.getText(_columnIndexOfAddress);
          }
          final String _tmpLoginTime;
          if (_stmt.isNull(_columnIndexOfLoginTime)) {
            _tmpLoginTime = null;
          } else {
            _tmpLoginTime = _stmt.getText(_columnIndexOfLoginTime);
          }
          final String _tmpEmail;
          if (_stmt.isNull(_columnIndexOfEmail)) {
            _tmpEmail = null;
          } else {
            _tmpEmail = _stmt.getText(_columnIndexOfEmail);
          }
          final String _tmpGender;
          if (_stmt.isNull(_columnIndexOfGender)) {
            _tmpGender = null;
          } else {
            _tmpGender = _stmt.getText(_columnIndexOfGender);
          }
          final String _tmpBirthDate;
          if (_stmt.isNull(_columnIndexOfBirthDate)) {
            _tmpBirthDate = null;
          } else {
            _tmpBirthDate = _stmt.getText(_columnIndexOfBirthDate);
          }
          _result = new UserEntity(_tmpUsername,_tmpDisplayName,_tmpPassword,_tmpPhone,_tmpAddress,_tmpLoginTime,_tmpEmail,_tmpGender,_tmpBirthDate);
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
  public void updateProfile(final String username, final String displayName, final String email,
      final String phone, final String gender, final String birthDate, final String address) {
    final String _sql = "UPDATE users SET display_name = ?, email = ?, phone = ?, gender = ?, birth_date = ?, address = ? WHERE username = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (displayName == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, displayName);
        }
        _argIndex = 2;
        if (email == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, email);
        }
        _argIndex = 3;
        if (phone == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, phone);
        }
        _argIndex = 4;
        if (gender == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, gender);
        }
        _argIndex = 5;
        if (birthDate == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, birthDate);
        }
        _argIndex = 6;
        if (address == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, address);
        }
        _argIndex = 7;
        if (username == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, username);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void updateShippingInfo(final String username, final String displayName,
      final String phone, final String address) {
    final String _sql = "UPDATE users SET display_name = ?, phone = ?, address = ? WHERE username = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (displayName == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, displayName);
        }
        _argIndex = 2;
        if (phone == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, phone);
        }
        _argIndex = 3;
        if (address == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, address);
        }
        _argIndex = 4;
        if (username == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, username);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void updatePassword(final String username, final String newPassword) {
    final String _sql = "UPDATE users SET password = ? WHERE username = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (newPassword == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, newPassword);
        }
        _argIndex = 2;
        if (username == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, username);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void deleteByUsername(final String username) {
    final String _sql = "DELETE FROM users WHERE username = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (username == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, username);
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
