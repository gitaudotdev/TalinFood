package ke.co.talin.myapplication.Remote;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class GoogleRetrofitClient {
    private static Retrofit retrofit =null;


    public static Retrofit getGoogleApiClient(String baseUrl){

        if(retrofit==null)
        {
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }

        return retrofit;
    }


}
