package com.example.roobab.mongodumper;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

public interface SignalServer {

    @GET("/")
    public void simpleGet();

    @POST("/api/signals")
    public void dumpSignals(@Body TypedJsonString signalJson, Callback<Response> cb);

    @POST("/api/collectSignals")
    public void collectLocationSignals(@Body TypedJsonString locationName, Callback<Response> cb);
}
