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
public final class PaymentDao_Impl implements PaymentDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<PaymentEntity> __insertAdapterOfPaymentEntity;

  public PaymentDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfPaymentEntity = new EntityInsertAdapter<PaymentEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `payments` (`order_code`,`order_id`,`payment_link_id`,`amount`,`description`,`checkout_url`,`qr_code`,`bank_bin`,`account_number`,`account_name`,`status`,`expired_at`,`created_at`,`paid_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final PaymentEntity entity) {
        statement.bindLong(1, entity.getOrderCode());
        if (entity.getOrderId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getOrderId());
        }
        if (entity.getPaymentLinkId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getPaymentLinkId());
        }
        statement.bindLong(4, entity.getAmount());
        if (entity.getDescription() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getDescription());
        }
        if (entity.getCheckoutUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getCheckoutUrl());
        }
        if (entity.getQrCode() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getQrCode());
        }
        if (entity.getBankBin() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getBankBin());
        }
        if (entity.getAccountNumber() == null) {
          statement.bindNull(9);
        } else {
          statement.bindText(9, entity.getAccountNumber());
        }
        if (entity.getAccountName() == null) {
          statement.bindNull(10);
        } else {
          statement.bindText(10, entity.getAccountName());
        }
        if (entity.getStatus() == null) {
          statement.bindNull(11);
        } else {
          statement.bindText(11, entity.getStatus());
        }
        statement.bindLong(12, entity.getExpiredAtSeconds());
        statement.bindLong(13, entity.getCreatedAtMillis());
        statement.bindLong(14, entity.getPaidAtMillis());
      }
    };
  }

  @Override
  public void upsert(final PaymentEntity payment) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfPaymentEntity.insert(_connection, payment);
      return null;
    });
  }

  @Override
  public PaymentEntity findLatestByOrderId(final String orderId) {
    final String _sql = "SELECT * FROM payments WHERE order_id = ? ORDER BY created_at DESC LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (orderId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, orderId);
        }
        final int _columnIndexOfOrderCode = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "order_code");
        final int _columnIndexOfOrderId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "order_id");
        final int _columnIndexOfPaymentLinkId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "payment_link_id");
        final int _columnIndexOfAmount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "amount");
        final int _columnIndexOfDescription = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "description");
        final int _columnIndexOfCheckoutUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "checkout_url");
        final int _columnIndexOfQrCode = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "qr_code");
        final int _columnIndexOfBankBin = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "bank_bin");
        final int _columnIndexOfAccountNumber = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "account_number");
        final int _columnIndexOfAccountName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "account_name");
        final int _columnIndexOfStatus = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "status");
        final int _columnIndexOfExpiredAtSeconds = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "expired_at");
        final int _columnIndexOfCreatedAtMillis = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final int _columnIndexOfPaidAtMillis = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "paid_at");
        final PaymentEntity _result;
        if (_stmt.step()) {
          final long _tmpOrderCode;
          _tmpOrderCode = _stmt.getLong(_columnIndexOfOrderCode);
          final String _tmpOrderId;
          if (_stmt.isNull(_columnIndexOfOrderId)) {
            _tmpOrderId = null;
          } else {
            _tmpOrderId = _stmt.getText(_columnIndexOfOrderId);
          }
          final String _tmpPaymentLinkId;
          if (_stmt.isNull(_columnIndexOfPaymentLinkId)) {
            _tmpPaymentLinkId = null;
          } else {
            _tmpPaymentLinkId = _stmt.getText(_columnIndexOfPaymentLinkId);
          }
          final long _tmpAmount;
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount);
          final String _tmpDescription;
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null;
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription);
          }
          final String _tmpCheckoutUrl;
          if (_stmt.isNull(_columnIndexOfCheckoutUrl)) {
            _tmpCheckoutUrl = null;
          } else {
            _tmpCheckoutUrl = _stmt.getText(_columnIndexOfCheckoutUrl);
          }
          final String _tmpQrCode;
          if (_stmt.isNull(_columnIndexOfQrCode)) {
            _tmpQrCode = null;
          } else {
            _tmpQrCode = _stmt.getText(_columnIndexOfQrCode);
          }
          final String _tmpBankBin;
          if (_stmt.isNull(_columnIndexOfBankBin)) {
            _tmpBankBin = null;
          } else {
            _tmpBankBin = _stmt.getText(_columnIndexOfBankBin);
          }
          final String _tmpAccountNumber;
          if (_stmt.isNull(_columnIndexOfAccountNumber)) {
            _tmpAccountNumber = null;
          } else {
            _tmpAccountNumber = _stmt.getText(_columnIndexOfAccountNumber);
          }
          final String _tmpAccountName;
          if (_stmt.isNull(_columnIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = _stmt.getText(_columnIndexOfAccountName);
          }
          final String _tmpStatus;
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmpStatus = null;
          } else {
            _tmpStatus = _stmt.getText(_columnIndexOfStatus);
          }
          final long _tmpExpiredAtSeconds;
          _tmpExpiredAtSeconds = _stmt.getLong(_columnIndexOfExpiredAtSeconds);
          final long _tmpCreatedAtMillis;
          _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis);
          final long _tmpPaidAtMillis;
          _tmpPaidAtMillis = _stmt.getLong(_columnIndexOfPaidAtMillis);
          _result = new PaymentEntity(_tmpOrderCode,_tmpOrderId,_tmpPaymentLinkId,_tmpAmount,_tmpDescription,_tmpCheckoutUrl,_tmpQrCode,_tmpBankBin,_tmpAccountNumber,_tmpAccountName,_tmpStatus,_tmpExpiredAtSeconds,_tmpCreatedAtMillis,_tmpPaidAtMillis);
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
  public PaymentEntity findByOrderCode(final long orderCode) {
    final String _sql = "SELECT * FROM payments WHERE order_code = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, orderCode);
        final int _columnIndexOfOrderCode = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "order_code");
        final int _columnIndexOfOrderId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "order_id");
        final int _columnIndexOfPaymentLinkId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "payment_link_id");
        final int _columnIndexOfAmount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "amount");
        final int _columnIndexOfDescription = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "description");
        final int _columnIndexOfCheckoutUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "checkout_url");
        final int _columnIndexOfQrCode = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "qr_code");
        final int _columnIndexOfBankBin = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "bank_bin");
        final int _columnIndexOfAccountNumber = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "account_number");
        final int _columnIndexOfAccountName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "account_name");
        final int _columnIndexOfStatus = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "status");
        final int _columnIndexOfExpiredAtSeconds = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "expired_at");
        final int _columnIndexOfCreatedAtMillis = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final int _columnIndexOfPaidAtMillis = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "paid_at");
        final PaymentEntity _result;
        if (_stmt.step()) {
          final long _tmpOrderCode;
          _tmpOrderCode = _stmt.getLong(_columnIndexOfOrderCode);
          final String _tmpOrderId;
          if (_stmt.isNull(_columnIndexOfOrderId)) {
            _tmpOrderId = null;
          } else {
            _tmpOrderId = _stmt.getText(_columnIndexOfOrderId);
          }
          final String _tmpPaymentLinkId;
          if (_stmt.isNull(_columnIndexOfPaymentLinkId)) {
            _tmpPaymentLinkId = null;
          } else {
            _tmpPaymentLinkId = _stmt.getText(_columnIndexOfPaymentLinkId);
          }
          final long _tmpAmount;
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount);
          final String _tmpDescription;
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null;
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription);
          }
          final String _tmpCheckoutUrl;
          if (_stmt.isNull(_columnIndexOfCheckoutUrl)) {
            _tmpCheckoutUrl = null;
          } else {
            _tmpCheckoutUrl = _stmt.getText(_columnIndexOfCheckoutUrl);
          }
          final String _tmpQrCode;
          if (_stmt.isNull(_columnIndexOfQrCode)) {
            _tmpQrCode = null;
          } else {
            _tmpQrCode = _stmt.getText(_columnIndexOfQrCode);
          }
          final String _tmpBankBin;
          if (_stmt.isNull(_columnIndexOfBankBin)) {
            _tmpBankBin = null;
          } else {
            _tmpBankBin = _stmt.getText(_columnIndexOfBankBin);
          }
          final String _tmpAccountNumber;
          if (_stmt.isNull(_columnIndexOfAccountNumber)) {
            _tmpAccountNumber = null;
          } else {
            _tmpAccountNumber = _stmt.getText(_columnIndexOfAccountNumber);
          }
          final String _tmpAccountName;
          if (_stmt.isNull(_columnIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = _stmt.getText(_columnIndexOfAccountName);
          }
          final String _tmpStatus;
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmpStatus = null;
          } else {
            _tmpStatus = _stmt.getText(_columnIndexOfStatus);
          }
          final long _tmpExpiredAtSeconds;
          _tmpExpiredAtSeconds = _stmt.getLong(_columnIndexOfExpiredAtSeconds);
          final long _tmpCreatedAtMillis;
          _tmpCreatedAtMillis = _stmt.getLong(_columnIndexOfCreatedAtMillis);
          final long _tmpPaidAtMillis;
          _tmpPaidAtMillis = _stmt.getLong(_columnIndexOfPaidAtMillis);
          _result = new PaymentEntity(_tmpOrderCode,_tmpOrderId,_tmpPaymentLinkId,_tmpAmount,_tmpDescription,_tmpCheckoutUrl,_tmpQrCode,_tmpBankBin,_tmpAccountNumber,_tmpAccountName,_tmpStatus,_tmpExpiredAtSeconds,_tmpCreatedAtMillis,_tmpPaidAtMillis);
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
  public void updateStatus(final long orderCode, final String status, final long paidAtMillis) {
    final String _sql = "UPDATE payments SET status = ?, paid_at = ? WHERE order_code = ?";
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
        _stmt.bindLong(_argIndex, paidAtMillis);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, orderCode);
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
