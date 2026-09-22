package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.dto.dashboard.DashboardResponse;
import com.trinhcong1120.survey_service.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/Dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  public DashboardController(
          DashboardService dashboardService
  ) {
    this.dashboardService = dashboardService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<DashboardResponse> getDashboard() {

    return ResponseEntity.ok(
            dashboardService.getDashboard()
    );
  }
}