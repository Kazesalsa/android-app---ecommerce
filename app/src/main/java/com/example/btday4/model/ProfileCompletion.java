package com.example.btday4.model;

import com.example.btday4.data.local.UserEntity;
import com.example.btday4.util.ValidationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Computes profile completeness percentage and missing fields from a UserEntity.
 */
public final class ProfileCompletion {

    private static final int TOTAL_FIELDS = 6;

    private final int completedFields;
    private final List<String> missingFieldLabels;

    private ProfileCompletion(int completedFields, List<String> missingFieldLabels) {
        this.completedFields = completedFields;
        this.missingFieldLabels = Collections.unmodifiableList(missingFieldLabels);
    }

    public static ProfileCompletion evaluate(UserEntity user) {
        List<String> missing = new ArrayList<>();
        if (user == null) {
            missing.add("Họ và tên");
            missing.add("Email");
            missing.add("Số điện thoại");
            missing.add("Giới tính");
            missing.add("Ngày sinh");
            missing.add("Địa chỉ giao hàng");
            return new ProfileCompletion(0, missing);
        }

        int filled = 0;
        if (ValidationUtils.isNotBlank(user.getDisplayName())) {
            filled++;
        } else {
            missing.add("Họ và tên");
        }

        if (ValidationUtils.isNotBlank(user.getEmail())) {
            filled++;
        } else {
            missing.add("Email");
        }

        if (ValidationUtils.isNotBlank(user.getPhone())) {
            filled++;
        } else {
            missing.add("Số điện thoại");
        }

        if (ValidationUtils.isNotBlank(user.getGender())) {
            filled++;
        } else {
            missing.add("Giới tính");
        }

        if (ValidationUtils.isNotBlank(user.getBirthDate())) {
            filled++;
        } else {
            missing.add("Ngày sinh");
        }

        if (ValidationUtils.isNotBlank(user.getAddress())) {
            filled++;
        } else {
            missing.add("Địa chỉ giao hàng");
        }

        return new ProfileCompletion(filled, missing);
    }

    public int getPercent() {
        return (int) Math.round((completedFields * 100.0) / TOTAL_FIELDS);
    }

    public boolean isComplete() {
        return completedFields >= TOTAL_FIELDS;
    }

    public List<String> getMissingFieldLabels() {
        return missingFieldLabels;
    }

    public String getMissingSummary() {
        if (missingFieldLabels.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < missingFieldLabels.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(missingFieldLabels.get(i));
        }
        return sb.toString();
    }
}
