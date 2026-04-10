package com.awal.cineq.publicapi.master_data.service.impl;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.masterdata.dto.MasterDataResponse;
import com.awal.cineq.masterdata.service.MasterDataService;
import com.awal.cineq.publicapi.master_data.service.PublicMasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicMasterDataServiceImpl implements PublicMasterDataService {

    private final MasterDataService masterDataService;

    @Override
    public ApiResponse<MasterDataResponse> getMasterData() {
        log.info("getMasterData STARTED");
        try {
            MasterDataResponse data = masterDataService.getAllMasterData();
            log.info("getMasterData END");
            return ApiResponse.success("Master data fetched successfully", data);
        } catch (Exception e) {
            log.error("getMasterData ERROR", e);
            throw e;
        }
    }
}
