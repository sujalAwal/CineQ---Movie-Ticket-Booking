package com.awal.cineq.publicapi.master_data.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.masterdata.dto.MasterDataResponse;
import com.awal.cineq.publicapi.master_data.service.PublicMasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/master-data")
@RequiredArgsConstructor
@Slf4j
public class PublicMasterDataController {

    private final PublicMasterDataService publicMasterDataService;

    @GetMapping
    public ResponseEntity<ApiResponse<MasterDataResponse>> getMasterData() {
        log.info("getMasterData REQUEST");
        ApiResponse<MasterDataResponse> response = publicMasterDataService.getMasterData();
        return ResponseEntity.ok(response);
    }
}
