package com.trinhcong1120.core_service.controller;

import com.trinhcong1120.core_service.dto.function.FunctionDetailResponse;
import com.trinhcong1120.core_service.dto.function.FunctionResponse;
import com.trinhcong1120.core_service.dto.function.UpdatePermissionRequest;
import com.trinhcong1120.core_service.service.FunctionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Functions")
public class FunctionController {

    private final FunctionService functionService;

    public FunctionController(
            FunctionService functionService) {

        this.functionService = functionService;
    }

    @PreAuthorize("hasAuthority('function_view')")
    @GetMapping
    public ResponseEntity<List<FunctionResponse>>
    getAllFunctions() {

        return ResponseEntity.ok(
                functionService.getAllFunctions()
        );
    }

    @PreAuthorize("hasAuthority('function_view')")
    @GetMapping("/{functionId}")
    public ResponseEntity<List<FunctionDetailResponse>>
    getFunctionDetail(
            @PathVariable Integer functionId) {

        return ResponseEntity.ok(
                functionService
                        .getFunctionDetail(functionId)
        );
    }

    @PreAuthorize("hasAuthority('function_update')")
    @PutMapping("/update-permission")
    public ResponseEntity<Map<String, Object>>
    updatePermission(
            @Valid @RequestBody UpdatePermissionRequest request) {

        return ResponseEntity.ok(
                functionService
                        .updatePermission(request)
        );
    }
}
