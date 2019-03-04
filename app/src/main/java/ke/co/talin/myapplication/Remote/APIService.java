package ke.co.talin.myapplication.Remote;

import ke.co.talin.myapplication.Model.DataMessage;
import ke.co.talin.myapplication.Model.MyResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAb3hPicU:APA91bEouWDxvo6z9TrRcjAKpd-DlEMSEf71PRW_JOPL8vabdE_FYgaUx0dCgzAhfIRZNnTdDVExQRA9g-uWi4pVR7pS7cf_PlGrYYTXEjNW9QzVG_9-TbbwQyH_KYa1D_uh7RoP3497H1obyau_QuSBp10VGAKcQA"
    })
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body DataMessage body);
}
