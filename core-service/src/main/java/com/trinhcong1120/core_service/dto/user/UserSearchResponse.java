package com.trinhcong1120.core_service.dto.user;

import java.util.UUID;

public record UserSearchResponse(UUID id, String username, String email) {}
