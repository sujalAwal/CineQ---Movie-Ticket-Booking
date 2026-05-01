package com.awal.cineq.publicapi.master_data.service;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.masterdata.dto.MasterDataResponse;

public interface PublicMasterDataService {
    ApiResponse<MasterDataResponse> getMasterData();
}
