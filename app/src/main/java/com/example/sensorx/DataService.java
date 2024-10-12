package com.example.sensorx;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DataService {
    private final String URL;
    private final OkHttpClient client;

    public interface ResponseListener<T> {
        void onFailure(T value);

        void onResponse(T value);
    }

    DataService(String URL) {
        this.URL = URL;
        client = new OkHttpClient();
    }

    public void testConn(ResponseListener<String> responseListener) {
        Request request = new Request.Builder().url(URL + "/test").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                responseListener.onFailure("Can't make request");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    responseListener.onResponse("success");
                } else {
                    responseListener.onFailure(response.message());
                }
            }
        });
    }

    public boolean sendFile(File file, ResponseListener<String> responseListener) {
        try {
            OkHttpClient extendedTimeoutClient = client.newBuilder()
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build();

            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("avatar", file.getName(),
                            RequestBody.create(file,MediaType.parse("text/csv")))
                    .build();

            Request request = new Request.Builder()
                    .url(BuildConfig.URL + "/addNewData")
                    .post(requestBody)
                    .addHeader("x-api-key", BuildConfig.API_KEY)
                    .build();

            extendedTimeoutClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    responseListener.onFailure("request timed out 408");
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        responseListener.onResponse("success");
                    } else {
                        responseListener.onFailure(response.toString());
                    }
                }
            });

            return true;
        } catch (Exception ex) {
            // Handle the error
        }
        return false;

    }

    public void sendData(String data, ResponseListener<String> responseListener) {

        RequestBody requestBody = RequestBody.create(data, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(BuildConfig.URL + "/addNewData")
                .post(requestBody)
                .addHeader("x-api-key", BuildConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                responseListener.onFailure("Can't make request");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    responseListener.onResponse("success");
                } else {
                    responseListener.onFailure(response.toString());
                }
            }
        });

    }

}