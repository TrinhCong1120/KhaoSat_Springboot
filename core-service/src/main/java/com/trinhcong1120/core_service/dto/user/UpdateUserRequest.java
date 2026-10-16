package com.trinhcong1120.core_service.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.List;

public class UpdateUserRequest {

    private Boolean isActive;

    @Email(message = "Email không đúng định dạng")
    private String email;

    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;

    private List<UUID> roleIds;
    private Long telegramChatId;
    private boolean telegramChatIdPresent;

    public UpdateUserRequest() {
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<UUID> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<UUID> roleIds) {
        this.roleIds = roleIds;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(Long telegramChatId) {
        this.telegramChatId = telegramChatId;
        this.telegramChatIdPresent = true;
    }

    @JsonIgnore
    public boolean isTelegramChatIdPresent() {
        return telegramChatIdPresent;
    }
}
