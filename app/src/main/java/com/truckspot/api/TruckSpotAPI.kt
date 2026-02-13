package com.truckspot.api

import com.truckspot.models.AddLogSuccessResponse
import com.truckspot.models.AllLogsResponse
import com.truckspot.models.CertifyModelItem
import com.truckspot.models.GetAllLogsResponse
import com.truckspot.models.GetCompanyById
import com.truckspot.models.GetLogsByDateRequest
import com.truckspot.models.GetLogsByDateResponse
import com.truckspot.models.GetLogsResponse
import com.truckspot.models.GetReportsResponse
import com.truckspot.models.HomeDataModel
import com.truckspot.models.LogIdRequest
import com.truckspot.models.LogResponse
import com.truckspot.models.LoginResponse
import com.truckspot.models.ReportsDataResponse
import com.truckspot.models.UnidentifiedResponse
import com.truckspot.request.AddLogRequest
import com.truckspot.request.AddLogRequestunauth
import com.truckspot.request.AddOffsetRequest
import com.truckspot.request.LoginRequest
import com.truckspot.request.updateLogRequest
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import userModelItem

interface TruckSpotAPI {

    @POST("api/v1/login")
    suspend fun login(@Body loginRequest: LoginRequest, @Query("source") source: String):Response<LoginResponse>

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


    @GET("api/v1/download-csv/{output}")

    @Streaming
    fun downloadCSV(@Path("output") output: String, @Body body: RequestBody): Call<ResponseBody>

}