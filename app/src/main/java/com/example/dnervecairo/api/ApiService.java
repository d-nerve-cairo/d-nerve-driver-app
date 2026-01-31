package com.example.dnervecairo.api;

import com.example.dnervecairo.api.requests.DriverRegistration;
import com.example.dnervecairo.api.requests.TripSubmission;
import com.example.dnervecairo.api.requests.WithdrawalRequest;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.api.responses.DriversListResponse;
import com.example.dnervecairo.api.responses.DriverScoreResponse;
import com.example.dnervecairo.api.responses.LeaderboardResponse;
import com.example.dnervecairo.api.responses.PointsHistoryResponse;
import com.example.dnervecairo.api.responses.TripResponse;
import com.example.dnervecairo.api.responses.TripsListResponse;
import com.example.dnervecairo.api.responses.WithdrawalResponse;
import com.example.dnervecairo.api.requests.UpdateDriverRequest;
import com.example.dnervecairo.api.responses.WithdrawalHistoryResponse;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ========== DRIVERS ==========

    @POST("drivers/register")
    Call<DriverResponse> registerDriver(@Body DriverRegistration request);

    @GET("drivers")
    Call<DriversListResponse> getDrivers();

    @PUT("drivers/{driver_id}")
    Call<DriverResponse> updateDriver(
            @Path("driver_id") String driverId,
            @Body UpdateDriverRequest request
    );

    @GET("drivers/{driver_id}")
    Call<DriverResponse> getDriver(@Path("driver_id") String driverId);

    // ========== TRIPS ==========

    @POST("trips")
    Call<TripResponse> submitTrip(@Body TripSubmission request);


    // Trip History
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

    @GET("gamification/drivers/{driver_id}/withdrawals")
    Call<WithdrawalHistoryResponse> getWithdrawalHistory(@Path("driver_id") String driverId);

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

    // ========== SYSTEM ==========

    @GET("health")
    Call<Object> healthCheck();
}
