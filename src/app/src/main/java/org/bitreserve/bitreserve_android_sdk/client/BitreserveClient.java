package org.bitreserve.bitreserve_android_sdk.client;

import com.darylteo.rx.promises.java.Promise;

import org.bitreserve.bitreserve_android_sdk.BuildConfig;
import org.bitreserve.bitreserve_android_sdk.client.restadapter.BitreserveRestAdapter;
import org.bitreserve.bitreserve_android_sdk.client.retrofitpromise.RetrofitPromise;
import org.bitreserve.bitreserve_android_sdk.client.session.SessionManager;
import org.bitreserve.bitreserve_android_sdk.exception.BitreserveSdkNotInitializedException;
import org.bitreserve.bitreserve_android_sdk.exception.StateMatchException;
import org.bitreserve.bitreserve_android_sdk.model.AuthenticationResponse;
import org.bitreserve.bitreserve_android_sdk.model.Rate;
import org.bitreserve.bitreserve_android_sdk.model.Reserve;
import org.bitreserve.bitreserve_android_sdk.model.Token;
import org.bitreserve.bitreserve_android_sdk.model.User;
import org.bitreserve.bitreserve_android_sdk.service.OAuth2Service;
import org.bitreserve.bitreserve_android_sdk.service.TickerService;
import org.bitreserve.bitreserve_android_sdk.util.Header;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import java.util.List;

/**
 * Bitreserve client.
 */

public class BitreserveClient {

    private static Context applicationContext;
    private static Boolean sdkInitialized = false;

    private Token token;

    /**
     * Initialize the client.
     */

    public static synchronized void initialize(Context context) {
        if (sdkInitialized) {
            return;
        }

        if (context == null) {
            return;
        }

        applicationContext = context;
        sdkInitialized = true;
    }

    /**
     * Constructor.
     */

    public BitreserveClient() throws BitreserveSdkNotInitializedException {
        if (!sdkInitialized) {
            throw new BitreserveSdkNotInitializedException("The SDK has not been initialized, make sure to call BitreserveClient.initialize(context)");
        }

        this.token = new Token();
        this.token.setBitreserveRestAdapter(new BitreserveRestAdapter());
    }

    /**
     * Constructor.
     *
     * @param bearerToken The user bearer token.
     */

    public BitreserveClient(String bearerToken) throws BitreserveSdkNotInitializedException {
        if (!sdkInitialized) {
            throw new BitreserveSdkNotInitializedException("The SDK has not been initialized, make sure to call BitreserveClient.initialize(context)");
        }

        this.token = new Token(bearerToken);
        this.token.setBitreserveRestAdapter(new BitreserveRestAdapter());
    }

    /**
     * Starts the authorization flow.
     *
     * @param context The context where the Bitreserve connect flow starts.
     * @param clientId The client id.
     * @param state The state.
     */

    public void beginAuthorization(Context context, String clientId, List<String> scopes, String state) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri clientUri = Uri.parse(String.format("%s/authorize/%s?scope=%s&state=%s", BuildConfig.AUTHORIZATION_SERVER_URL, clientId, TextUtils.join(" ", scopes), state));

        intent.setData(clientUri);
        context.startActivity(intent);
    }

    /**
     * Completes the authorization flow.
     *
     * @param uri The uri returned by the intent called by the begin authorization flow.
     * @param clientId The client secret.
     * @param clientSecret The client id.
     * @param state The state.
     *
     * @return the {@link AuthenticationResponse}.
     */

    public Promise<AuthenticationResponse> completeAuthorization(Uri uri, String clientId, String clientSecret, String grantType, String state) {
        RetrofitPromise<AuthenticationResponse> promise = new RetrofitPromise<>();
        OAuth2Service oAuth2Service = this.getToken().getBitreserveRestAdapter().create(OAuth2Service.class);

        if (state.compareTo(uri.getQueryParameter("state")) != 0) {
            promise.reject(new StateMatchException("State does not match."));
        }

        oAuth2Service.requestToken(Header.encodeCredentialsForBasicAuthorization(clientId, clientSecret), uri.getQueryParameter("code"), grantType, promise);

        return promise;
    }

    public static Context getApplicationContext() {
        return applicationContext;
    }

    /**
     * Gets the reserve model.
     *
     * @return a {@link Reserve}.
     */

    public Reserve getReserve() {
        return new Reserve();
    }

    /**
     * Gets all exchanges rates for all currency pairs.
     *
     * @return a {@link Promise<List<Rate>>} with all exchanges rates for all currency pairs.
     */

    public Promise<List<Rate>> getTicker() {
        RetrofitPromise<List<Rate>> promise = new RetrofitPromise<>();
        TickerService tickerService = this.getToken().getBitreserveRestAdapter().create(TickerService.class);

        tickerService.getAllTickers(promise);

        return promise;
    }

    /**
     * Gets all exchanges rates relative to a given currency.
     *
     * @param currency The filter currency.
     *
     * @return a {@link Promise<List<Rate>>} with all exchanges rates relative to a given currency.
     */

    public Promise<List<Rate>> getTickersByCurrency(String currency) {
        RetrofitPromise<List<Rate>> promise = new RetrofitPromise<>();
        TickerService tickerService = this.getToken().getBitreserveRestAdapter().create(TickerService.class);

        tickerService.getAllTickersByCurrency(currency, promise);

        return promise;
    }

    /**
     * Gets the bitreserve client token.
     *
     * @return the {@link Token}
     */

    public Token getToken() {
        return token;
    }

    /**
     * Sets the bitreserve token.
     *
     * @param token The {@link Token}.
     */

    public void setToken(Token token) {
        this.token = token;
    }

    /**
     * Gets the current user.
     *
     * @return a {@link Promise<User>} with the user.
     */

    public Promise<User> getUser() {
        return this.getToken().getUser();
    }

    /**
     * Invalidates user current session.
     */

    public void invalidateSession() {
        SessionManager.INSTANCE.invalidateSession();
    }

}