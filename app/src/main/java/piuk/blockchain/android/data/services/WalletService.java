package piuk.blockchain.android.data.services;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.api.data.Status;
import info.blockchain.wallet.exceptions.ApiException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import piuk.blockchain.android.util.annotations.WebRequest;
import retrofit2.Response;

public class WalletService {

    private WalletApi walletApi;

    public WalletService(WalletApi walletApi) {
        this.walletApi = walletApi;
    }

    /**
     * Get encrypted copy of Payload
     *
     * @param guid      A user's GUID
     * @param sessionId The session ID, retreived from {@link #getSessionId(String)}
     * @return {@link Observable<ResponseBody>} wrapping an encrypted Payload
     */
    @WebRequest
    public Observable<Response<ResponseBody>> getEncryptedPayload(String guid, String sessionId) {
        return walletApi.fetchEncryptedPayload(guid, sessionId);
    }

    /**
     * Gets a session ID from the server
     *
     * @param guid A user's GUID
     * @return {@link Observable<ResponseBody>}
     */
    @WebRequest
    public Observable<String> getSessionId(String guid) {
        return walletApi.getSessionId(guid)
                .map(responseBodyResponse -> {
                    String headers = responseBodyResponse.headers().get("Set-Cookie");
                    if (headers != null) {
                        String[] fields = headers.split(";\\s*");
                        for (String field : fields) {
                            if (field.startsWith("SID=")) {
                                return field.substring(4);
                            }
                        }
                    } else {
                        throw new ApiException("Session ID not found in headers");
                    }
                    return "";
                });
    }

    /**
     * Get the encryption password for pairing
     *
     * @param guid A user's GUID
     * @return {@link Observable<ResponseBody>} wrapping the pairing encryption password
     */
    @WebRequest
    public Observable<ResponseBody> getPairingEncryptionPassword(String guid) {
        return walletApi.fetchPairingEncryptionPassword(guid);
    }

    /**
     * Sends the access key to the server
     *
     * @param key   The PIN identifier
     * @param value The value, randomly generated
     * @param pin   The user's PIN
     * @return An {@link Observable<Boolean>} where the boolean represents success
     */
    @WebRequest
    public Observable<Response<Status>> setAccessKey(String key, String value, String pin) {
        return walletApi.setAccess(key, value, pin);
    }

    /**
     * Validates a user's PIN with the server
     *
     * @param key The PIN identifier
     * @param pin The user's PIN
     * @return A {@link Response<Status>} which may or may not contain the field "success"
     */
    @WebRequest
    public Observable<Response<Status>> validateAccess(String key, String pin) {
        return walletApi.validateAccess(key, pin)
                .doOnError(throwable -> {
                    if (throwable.getMessage().contains("Incorrect PIN")) {
                        throw new InvalidCredentialsException("Incorrect PIN");
                    }
                });
    }

    /**
     * Logs an event to the backend for analytics purposes to work out which features are used most often.
     *
     * @param event An event as a String
     * @return  An {@link Observable} wrapping a {@link Status} object
     * @see EventService
     */
    @WebRequest
    public Observable<Status> logEvent(String event) {
        return walletApi.logEvent(event);
    }

}
