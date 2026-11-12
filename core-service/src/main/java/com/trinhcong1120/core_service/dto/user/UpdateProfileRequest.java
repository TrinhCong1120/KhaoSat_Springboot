package com.trinhcong1120.core_service.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(max = 50, message = "Số điện thoại không được vượt quá 50 ký tự")
    private String phoneNumber;

    private String avatarUrl;

    private Boolean botNotificationEnabled;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String fullName, String email, String phoneNumber, String avatarUrl, Boolean botNotificationEnabled) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.botNotificationEnabled = botNotificationEnabled;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Boolean getBotNotificationEnabled() {
        return botNotificationEnabled;
    }

    public void setBotNotificationEnabled(Boolean botNotificationEnabled) {
        this.botNotificationEnabled = botNotificationEnabled;
    }
}
