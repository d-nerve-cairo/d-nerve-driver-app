package com.example.dnervecairo.api;

import com.example.dnervecairo.api.requests.DriverRegistration;
import com.example.dnervecairo.api.requests.ETARequest;
import com.example.dnervecairo.api.requests.LoginRequest;
import com.example.dnervecairo.api.requests.TripSubmission;
import com.example.dnervecairo.api.requests.WithdrawalRequest;
import com.example.dnervecairo.api.requests.UpdateDriverRequest;
// NEW: Live trip requests
import com.example.dnervecairo.api.requests.StartLiveTripRequest;
import com.example.dnervecairo.api.requests.LiveGpsUpdateRequest;

import com.example.dnervecairo.api.responses.DocumentUploadResponse;
import com.example.dnervecairo.api.responses.DocumentsStatusResponse;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.api.responses.DriversListResponse;
import com.example.dnervecairo.api.responses.DriverScoreResponse;
import com.example.dnervecairo.api.responses.ETAResponse;
import com.example.dnervecairo.api.responses.LeaderboardResponse;
import com.example.dnervecairo.api.responses.LoginResponse;
import com.example.dnervecairo.api.responses.PointsHistoryResponse;
import com.example.dnervecairo.api.responses.RegisterResponse;
import com.example.dnervecairo.api.responses.RouteResponse;
import com.example.dnervecairo.api.responses.TripResponse;
import com.example.dnervecairo.api.responses.TripsListResponse;
import com.example.dnervecairo.api.responses.WithdrawalResponse;
import com.example.dnervecairo.api.responses.WithdrawalHistoryResponse;
import com.example.dnervecairo.api.responses.BadgeResponse;
// NEW: Live trip responses
import com.example.dnervecairo.api.responses.LiveTripStartResponse;
import com.example.dnervecairo.api.responses.LiveTripEndResponse;

import com.google.gson.JsonObject;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.DELETE;
import retrofit2.http.Multipart;
import retrofit2.http.Part;

public interface ApiService {

    // ========== AUTHENTICATION ==========

    @POST("drivers/login")
    Call<LoginResponse> loginDriver(@Body LoginRequest request);

    @POST("drivers/register")
    Call<RegisterResponse> registerDriver(@Body DriverRegistration request);

    // ========== DRIVERS ==========

    @GET("drivers")
    Call<DriversListResponse> getDrivers();

    @PUT("drivers/{driver_id}")
    Call<DriverResponse> updateDriver(
            @Path("driver_id") String driverId,
            @Body UpdateDriverRequest request
    );

    @GET("drivers/{driver_id}")
    Call<DriverResponse> getDriver(@Path("driver_id") String driverId);

    // ========== TRIPS (Batch Mode) ==========

    @POST("trips")
    Call<TripResponse> submitTrip(@Body TripSubmission request);

    @GET("trips")
    Call<TripsListResponse> getTripHistory(
            @Query("driver_id") String driverId,
            @Query("limit") int limit,
            @Query("offset") int offset
    );

    @GET("drivers/{driver_id}/trips")
    Call<Object> getDriverTrips(
            @Path("driver_id") String driverId,
            @Query("limit") int limit,
            @Query("offset") int offset
    );

    // ========== LIVE TRIPS (Real-time Mode) ==========

    /**
     * Start a live trip - call before beginning GPS tracking
     * Returns trip_id to use for GPS updates and trip end
     */
    @POST("trips/start")
    Call<LiveTripStartResponse> startLiveTrip(@Body StartLiveTripRequest request);

    /**
     * Send GPS update during active trip
     * Call every 10s (moving) or 20s (stopped/slow)
     */
    @POST("trips/{trip_id}/gps")
    Call<JsonObject> sendGpsUpdate(
            @Path("trip_id") String tripId,
            @Body LiveGpsUpdateRequest request
    );

    /**
     * End a live trip - triggers scoring and points calculation
     * Returns points earned, quality score, and driver tier
     */
    @POST("trips/{trip_id}/end")
    Call<LiveTripEndResponse> endLiveTrip(@Path("trip_id") String tripId);

    // ========== GAMIFICATION ==========

    @GET("gamification/leaderboard")
    Call<LeaderboardResponse> getLeaderboard(
            @Query("limit") int limit,
            @Query("sort_by") String sortBy
    );

    @GET("gamification/drivers/{driver_id}/score")
    Call<DriverScoreResponse> getDriverScore(@Path("driver_id") String driverId);

    @GET("gamification/drivers/{driver_id}/history")
    Call<PointsHistoryResponse> getPointsHistory(
            @Path("driver_id") String driverId,
            @Query("limit") int limit,
            @Query("offset") int offset
    );

    @POST("gamification/drivers/{driver_id}/withdraw")
    Call<WithdrawalResponse> requestWithdrawal(
            @Path("driver_id") String driverId,
            @Body WithdrawalRequest request
    );

    @GET("gamification/drivers/{driver_id}/withdrawals")
    Call<WithdrawalHistoryResponse> getWithdrawalHistory(@Path("driver_id") String driverId);

    // ========== BADGES ==========

    @GET("badges/driver/{driver_id}/progress")
    Call<List<BadgeResponse>> getBadgeProgress(@Path("driver_id") String driverId);

    @POST("badges/driver/{driver_id}/check")
    Call<JsonObject> checkBadges(@Path("driver_id") String driverId);

    // ========== ROUTES ==========

    @GET("routes")
    Call<List<RouteResponse>> getRoutes();

    // ========== DOCUMENTS ==========

    @GET("documents/driver/{driver_id}")
    Call<DocumentsStatusResponse> getDriverDocuments(@Path("driver_id") String driverId);

    @Multipart
    @POST("documents/driver/{driver_id}/upload")
    Call<DocumentUploadResponse> uploadDocument(
            @Path("driver_id") String driverId,
            @Part("document_type") RequestBody documentType,
            @Part MultipartBody.Part file
    );

    @DELETE("documents/driver/{driver_id}/{document_type}")
    Call<Object> deleteDocument(
            @Path("driver_id") String driverId,
            @Path("document_type") String documentType
    );

    // ========== ETA PREDICTION ==========

    @POST("predict-eta/simple")
    Call<ETAResponse> predictETA(@Body ETARequest request);

    // ========== FCM PUSH NOTIFICATIONS ==========

    /** Upload or refresh the driver's FCM push token */
    @PUT("drivers/{driver_id}/fcm-token")
    Call<JsonObject> updateDriverFcmToken(
            @Path("driver_id") String driverId,
            @Query("fcm_token") String fcmToken
    );

    /** Clear the FCM token on logout */
    @DELETE("drivers/{driver_id}/fcm-token")
    Call<JsonObject> clearDriverFcmToken(@Path("driver_id") String driverId);

    // ========== SYSTEM ==========

    @GET("health")
    Call<Object> healthCheck();
}