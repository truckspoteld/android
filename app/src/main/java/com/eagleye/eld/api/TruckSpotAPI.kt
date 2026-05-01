package com.eagleye.eld.api

import com.eagleye.eld.models.AddLogSuccessResponse
import com.eagleye.eld.models.TelemetryRequest
import com.eagleye.eld.models.TelemetryResponse
import com.eagleye.eld.models.VehicleHealthResponse
import com.eagleye.eld.models.FleetDashboardResponse
import com.eagleye.eld.models.AllLogsResponse
import com.eagleye.eld.models.CertifyModelItem
import com.eagleye.eld.models.GetAllLogsResponse
import com.eagleye.eld.models.GetCompanyById
import com.eagleye.eld.models.GetLogsByDateRequest
import com.eagleye.eld.models.GetLogsByDateResponse
import com.eagleye.eld.models.GetLogsResponse
import com.eagleye.eld.models.GetReportsResponse
import com.eagleye.eld.models.FmcsaEmailTransferRequest
import com.eagleye.eld.models.FmcsaEmailTransferResponse
import com.eagleye.eld.models.FmcsaWebServiceTransferRequest
import com.eagleye.eld.models.FmcsaWebServiceTransferResponse
import com.eagleye.eld.models.HomeDataModel
import com.eagleye.eld.models.DriverCodriversResponse
import com.eagleye.eld.models.DriverShipmentResponse
import com.eagleye.eld.models.DvirCreateResponse
import com.eagleye.eld.models.DvirListResponse
import com.eagleye.eld.models.LogIdRequest
import com.eagleye.eld.models.LogResponse
import com.eagleye.eld.models.LoginResponse
import com.eagleye.eld.models.PaperLogsEmailRequest
import com.eagleye.eld.models.PaperLogsEmailResponse
import com.eagleye.eld.models.ReportsDataResponse
import com.eagleye.eld.models.UnidentifiedResponse
import com.eagleye.eld.request.AddLogRequest
import com.eagleye.eld.request.AddLogRequestunauth
import com.eagleye.eld.request.AddOffsetRequest
import com.eagleye.eld.request.CodriverLoginRequest
import com.eagleye.eld.request.DriverShipmentRequest
import com.eagleye.eld.request.DvirCreateRequest
import com.eagleye.eld.request.LoginRequest
import com.eagleye.eld.request.updateLogRequest
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

    @POST("api/v1/fmcsa/email-transfer")
    suspend fun sendFmcsaEmailTransfer(
        @Query("start") startDate: String,
        @Query("end") endDate: String,
        @Body request: FmcsaEmailTransferRequest
    ): Response<FmcsaEmailTransferResponse>

    @POST("api/v1/fmcsa/webservice-transfer")
    suspend fun sendFmcsaWebServiceTransfer(
        @Query("start") startDate: String,
        @Query("end") endDate: String,
        @Body request: FmcsaWebServiceTransferRequest
    ): Response<FmcsaWebServiceTransferResponse>

    @POST("api/v1/paper-logs/email")
    suspend fun sendPaperLogsByEmail(
        @Body request: PaperLogsEmailRequest
    ): Response<PaperLogsEmailResponse>

    @GET("api/v1/driver/codrivers")
    suspend fun getMyCodrivers(): Response<DriverCodriversResponse>

    @POST("api/v1/driver/codriver/login")
    suspend fun codriverLogin(
        @Body body: CodriverLoginRequest
    ): Response<LoginResponse>

    @POST("api/v1/driver/codriver/logout")
    suspend fun codriverLogout(): Response<LoginResponse>

    @POST("api/v1/login")
    suspend fun loginWithUsername(
        @Query("source") source: String = "mobile",
        @Body body: LoginRequest
    ): Response<LoginResponse>

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
    suspend fun getDriverReview(): Response<com.eagleye.eld.models.DriverReviewResponse>

    // Predictive Maintenance
    @POST("api/v1/telemetry")
    suspend fun saveTelemetry(@Body request: TelemetryRequest): Response<TelemetryResponse>

    @GET("api/v1/telemetry/vehicle/{vehicleId}/health")
    suspend fun getVehicleHealth(@Path("vehicleId") vehicleId: Int): Response<VehicleHealthResponse>

    @GET("api/v1/telemetry/fleet/dashboard")
    suspend fun getFleetDashboard(): Response<FleetDashboardResponse>

    @POST("api/v1/addLogWithException")
    suspend fun addLogWithException(@Body request: com.eagleye.eld.request.AddLogWithExceptionRequest): Response<AddLogSuccessResponse>
}
