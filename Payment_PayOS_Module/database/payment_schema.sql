-- =========================================================================
-- DATABASE SCHEMA CHO MODULE THANH TOÁN (PAYMENT & PAYOS)
-- PostgreSQL / Neon Database
-- =========================================================================

-- 1. Định nghĩa kiểu ENUM cho trạng thái thanh toán
DO $$ BEGIN
    CREATE TYPE enum_thanhtoan_trangthai AS ENUM ('DANGXULY', 'THANHCONG', 'THATBAI');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 2. Định nghĩa kiểu ENUM cho trạng thái đơn hàng (tham chiếu)
DO $$ BEGIN
    CREATE TYPE enum_donhang_trangthai AS ENUM ('CHUATHANHTOAN', 'DATHANHTOAN', 'DANGXULY', 'DANGGIAO', 'HOANTHANH', 'DAHUY');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 3. Cấu trúc bảng ThanhToan
CREATE TABLE IF NOT EXISTS "ThanhToan" (
    "maTT" VARCHAR(50) PRIMARY KEY,
    "maDonHang" VARCHAR(50) NOT NULL,
    "ngayThanhToan" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "soTien" NUMERIC(14, 2) NOT NULL CHECK ("soTien" >= 0),
    "trangThai" enum_thanhtoan_trangthai NOT NULL DEFAULT 'DANGXULY',
    "Deleted" BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_thanhtoan_donhang FOREIGN KEY ("maDonHang") 
        REFERENCES "DonHang"("maDonHang") ON DELETE CASCADE
);

-- Index tối ưu tìm kiếm theo mã đơn hàng
CREATE INDEX IF NOT EXISTS idx_thanhtoan_madonhang ON "ThanhToan"("maDonHang");

-- 4. Lưu ý về cột ghiChu trên bảng DonHang:
-- Hệ thống sử dụng cột "ghiChu" trên bảng "DonHang" để lưu "orderCode" của PayOS
-- giúp đối chiếu 2 chiều giữa mã đơn hàng và mã giao dịch PayOS khi nhận Webhook.
-- ALTER TABLE "DonHang" ADD COLUMN IF NOT EXISTS "ghiChu" TEXT;
