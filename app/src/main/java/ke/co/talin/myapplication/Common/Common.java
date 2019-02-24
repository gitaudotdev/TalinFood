package ke.co.talin.myapplication.Common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import ke.co.talin.myapplication.Model.User;
import ke.co.talin.myapplication.Remote.APIService;
import ke.co.talin.myapplication.Remote.IGoogleService;
import ke.co.talin.myapplication.Remote.RetrofitClient;

public class Common {
    public static User currentUser;

    public static String PHONE_TEXT = "userPhone";

    public static  String convertCodeToStatus(String status) {
        if(status.equals("0"))
            return "Placed";
        else if(status.equals("1"))
            return "On my Way";
        else
            return "Shipped";
    }

    public static final String BASE_URL ="https://fcm.googleapis.com/";
    public static final String GOOGLE_API_URL ="https://maps.googleapis.com/";

    public static APIService getFCMService(){
        return RetrofitClient.getClient(BASE_URL).create(APIService.class);
    }

    public static IGoogleService getGoogleMapService(){
        return RetrofitClient.getGoogleApiClient(GOOGLE_API_URL).create(IGoogleService.class);
    }


    public static final String DELETE = "Delete";
    public static final String USER_KEY = "User";
    public static final String PWD_KEY = "password";

    public static final String Consumer_KEY = "6arc83HZAvM5iVNFhss22h40hmKMr7Kg";
    public static final String Consumer_SECRET = "BnbDgMizTVWPdbGz";


    public static boolean isConnectedToInternet(Context context){
        ConnectivityManager connectivityManager =(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(connectivityManager!=null)
        {
            NetworkInfo[] info = connectivityManager.getAllNetworkInfo();
            if(info !=null)
            {
                for(int i=0;i<info.length;i++)
                {
                    if(info[i].getState()==NetworkInfo.State.CONNECTED)
                        return true;
                }
            }
        }
        return false;
    }
}
