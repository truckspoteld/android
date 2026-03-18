package com.truckspot.eld.api

import com.truckspot.eld.models.AddLogSuccessResponse
import com.truckspot.eld.models.AllLogsResponse
import com.truckspot.eld.models.CertifyModelItem
import com.truckspot.eld.models.GetAllLogsResponse
import com.truckspot.eld.models.GetCompanyById
import com.truckspot.eld.models.GetLogsByDateRequest
import com.truckspot.eld.models.GetLogsByDateResponse
import com.truckspot.eld.models.GetLogsResponse
import com.truckspot.eld.models.GetReportsResponse
import com.truckspot.eld.models.HomeDataModel
import com.truckspot.eld.models.DriverCodriversResponse
import com.truckspot.eld.models.DriverShipmentResponse
import com.truckspot.eld.models.DvirCreateResponse
import com.truckspot.eld.models.DvirListResponse
import com.truckspot.eld.models.LogIdRequest
import com.truckspot.eld.models.LogResponse
import com.truckspot.eld.models.LoginResponse
import com.truckspot.eld.models.ReportsDataResponse
import com.truckspot.eld.models.UnidentifiedResponse
import com.truckspot.eld.request.AddLogRequest
import com.truckspot.eld.request.AddLogRequestunauth
import com.truckspot.eld.request.AddOffsetRequest
import com.truckspot.eld.request.DriverShipmentRequest
import com.truckspot.eld.request.DvirCreateRequest
import com.truckspot.eld.request.LoginRequest
import com.truckspot.eld.request.updateLogRequest
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import userModelItem

interface TruckSpotAPI {

    @POST("api/v1/login")
    suspend fun login(
        @Query("source") source: String = "mobile",
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    @POST("api/v1/addLog")
    suspend fun addLog(@Body addLogRequest: AddLogRequest?):Response<AddLogSuccessResponse>

    @PUT("api/v1/add_unidentified")
    suspend fun addOffset(@Body addOffsetRequest: AddOffsetRequest): Response<UnidentifiedResponse>

    @GET("api/v1/get_unidentified")
    suspend fun getOffset(@Query("vin_no") vin: String): Response<UnidentifiedResponse>

    @POST("api/v1/addLogUnauth")
    suspend fun addLogUnauth(@Body addLogRequest: AddLogRequestunauth): Response<LogResponse>
    @GET("api/v1/getLogs") // Replace with the correct endpoint path
    suspend fun getLogs(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("days") days: Int
    ): Response<GetLogsResponse>

    @GET("api/v1/getAlllog")
    suspend fun getAllLog(): Response<GetAllLogsResponse>

    @GET("api/v1//getalllogs")
    suspend fun getAllLogs(): Response<AllLogsResponse>

    @GET("api/v1/get_company/{id}")
    suspend fun getCompanyById(
        @Path("id") id: Int
    ): Response<GetCompanyById>

    @GET("api/v1/get_certified")
    suspend fun getDates(): Response<List<CertifyModelItem>>

    @GET("api/v1/get_users")
    suspend fun getUsers(): Response<List<userModelItem>>

    @PUT("api/v1/updatelog")
    suspend fun updateLog(@Body updateLogRequest: updateLogRequest):Response<LogResponse>

    @PUT("api/v1/updatelog")
    suspend fun updateLogbyuser(@Body updateLogRequest: updateLogRequest):Response<LogResponse>

    @PUT("api/v1/updated_certified")
    suspend fun updatedCertified(@Body updateCertifiedRequest: CertifyModelItem):Response<CertifyModelItem>
    @HTTP(method = "DELETE", path = "api/v1/deletelog", hasBody = true)
    suspend fun deleteLog(@Body logId: LogIdRequest): Response<LogResponse>
     @GET("api/v1/get_reports")
    suspend fun getReportsData(): Response<ReportsDataResponse>

    @GET("api/v1/getReportLogs")
    suspend fun getReportsLogs(): Response<GetReportsResponse>


    @GET("api/v1/getalllogs")
    suspend fun getHomeData(): Response<HomeDataModel>

    @POST("api/v1/getAllLogbyidandtime")
    suspend fun getLogByDate(
        @Body request: GetLogsByDateRequest
    ): Response<GetLogsByDateResponse>

    @GET("api/v1/driver/codrivers")
    suspend fun getMyCodrivers(): Response<DriverCodriversResponse>

    @GET("api/v1/driver/shipment-assignment/active")
    suspend fun getActiveDriverShipment(): Response<DriverShipmentResponse>

    @POST("api/v1/driver/shipment-assignment")
    suspend fun upsertDriverShipment(
        @Body request: DriverShipmentRequest
    ): Response<DriverShipmentResponse>

    @POST("api/v1/driver/dvir")
    suspend fun submitDVIR(
        @Body request: DvirCreateRequest
    ): Response<DvirCreateResponse>

    @GET("api/v1/driver/dvir")
    suspend fun getDriverDVIRReports(
        @Query("status") status: String? = null
    ): Response<DvirListResponse>

    @GET("api/v1/driver/dvir/open")
    suspend fun getDriverOpenDVIR(): Response<DvirCreateResponse>


    @GET("api/v1/download-csv/{output}")
    @Streaming
    fun downloadCSV(@Path("output") output: String, @Body body: RequestBody): Call<ResponseBody>

    @GET("api/v1/driver/review")
    suspend fun getDriverReview(): Response<com.truckspot.eld.models.DriverReviewResponse>
}
