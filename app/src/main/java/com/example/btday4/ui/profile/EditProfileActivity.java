package com.example.btday4.ui.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.btday4.R;
import com.example.btday4.data.repository.UserSession;
import com.example.btday4.databinding.ActivityEditProfileBinding;
import com.example.btday4.util.UiUtils;
import com.example.btday4.util.ValidationUtils;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;
import java.util.Locale;

/**
 * Full-screen profile editor using inline 3-field birthdate inputs (eliminating DatePickerDialog popup).
 */
public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.editProfileRoot);

        userSession = new UserSession(this);

        binding.toolbarEditProfile.setNavigationOnClickListener(v -> finish());
        UiUtils.clearErrorOnType(binding.etEditName, binding.tilEditName);
        UiUtils.clearErrorOnType(binding.etEditEmail, binding.tilEditEmail);
        UiUtils.clearErrorOnType(binding.etEditPhone, binding.tilEditPhone);
        UiUtils.clearErrorOnType(binding.etEditAddress, binding.tilEditAddress);

        setupInlineDateAutoAdvance();
        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());

        loadCurrentProfile();
    }

    private void setupInlineDateAutoAdvance() {
        TextWatcher clearDateError = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvBirthDateError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        binding.etBirthDay.addTextChangedListener(clearDateError);
        binding.etBirthMonth.addTextChangedListener(clearDateError);
        binding.etBirthYear.addTextChangedListener(clearDateError);

        binding.etBirthDay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.length() == 2) {
                    binding.etBirthMonth.requestFocus();
                }
            }
        });

        binding.etBirthMonth.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.length() == 2) {
                    binding.etBirthYear.requestFocus();
                }
            }
        });
    }

    private void loadCurrentProfile() {
        userSession.loadCurrentUser(user -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (user == null) {
                binding.etEditName.setText(userSession.getDisplayName());
                return;
            }
            binding.etEditName.setText(nonNull(user.getDisplayName()));
            binding.etEditEmail.setText(nonNull(user.getEmail()));
            binding.etEditPhone.setText(nonNull(user.getPhone()));
            binding.etEditAddress.setText(nonNull(user.getAddress()));

            String birthDate = nonNull(user.getBirthDate()).trim();
            if (birthDate.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                String[] parts = birthDate.split("/");
                binding.etBirthDay.setText(parts[0]);
                binding.etBirthMonth.setText(parts[1]);
                binding.etBirthYear.setText(parts[2]);
            }

            String gender = nonNull(user.getGender());
            if ("Nam".equalsIgnoreCase(gender)) {
                binding.chipGenderMale.setChecked(true);
            } else if ("Nữ".equalsIgnoreCase(gender)) {
                binding.chipGenderFemale.setChecked(true);
            } else if ("Khác".equalsIgnoreCase(gender)) {
                binding.chipGenderOther.setChecked(true);
            }
        });
    }

    private void saveProfile() {
        String name = textOf(binding.etEditName);
        String email = textOf(binding.etEditEmail);
        String phone = ValidationUtils.normalizePhone(textOf(binding.etEditPhone));
        String address = textOf(binding.etEditAddress);
        String gender = selectedGender();

        String dayStr = textOf(binding.etBirthDay);
        String monthStr = textOf(binding.etBirthMonth);
        String yearStr = textOf(binding.etBirthYear);
        String formattedBirthDate = "";

        boolean valid = true;
        if (!ValidationUtils.isNotBlank(name)) {
            binding.tilEditName.setError("Vui lòng nhập họ và tên");
            valid = false;
        }
        if (!email.isEmpty() && !ValidationUtils.isValidEmail(email)) {
            binding.tilEditEmail.setError("Định dạng email không hợp lệ");
            valid = false;
        }
        if (!phone.isEmpty() && !ValidationUtils.isValidPhone(phone)) {
            binding.tilEditPhone.setError("Số điện thoại không hợp lệ (VD: 0912345678)");
            valid = false;
        }

        boolean anyDatePartEntered = !dayStr.isEmpty() || !monthStr.isEmpty() || !yearStr.isEmpty();
        if (anyDatePartEntered) {
            if (dayStr.isEmpty() || monthStr.isEmpty() || yearStr.length() < 4) {
                showBirthDateError("Vui lòng nhập đủ Ngày, Tháng và Năm (4 chữ số)");
                valid = false;
            } else {
                try {
                    int d = Integer.parseInt(dayStr);
                    int m = Integer.parseInt(monthStr);
                    int y = Integer.parseInt(yearStr);
                    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                    if (!ValidationUtils.isValidBirthDate(d, m, y, currentYear)) {
                        showBirthDateError("Ngày sinh không hợp lệ (VD: 15 / 08 / 2003)");
                        valid = false;
                    } else {
                        formattedBirthDate = String.format(Locale.US, "%02d/%02d/%04d", d, m, y);
                    }
                } catch (NumberFormatException e) {
                    showBirthDateError("Ngày sinh không hợp lệ");
                    valid = false;
                }
            }
        }

        if (!valid) {
            return;
        }

        binding.btnSaveProfile.setEnabled(false);
        userSession.updateProfile(name, email, phone, gender, formattedBirthDate, address, () -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            setResult(RESULT_OK);
            finish();
        });
    }

    private void showBirthDateError(String message) {
        binding.tvBirthDateError.setText(message);
        binding.tvBirthDateError.setVisibility(View.VISIBLE);
    }

    private String selectedGender() {
        int checkedId = binding.chipGroupGender.getCheckedChipId();
        if (checkedId == R.id.chipGenderMale) {
            return "Nam";
        }
        if (checkedId == R.id.chipGenderFemale) {
            return "Nữ";
        }
        if (checkedId == R.id.chipGenderOther) {
            return "Khác";
        }
        return "";
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }
}
