package it.reply.selly.mobileapp;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import cz.msebera.android.httpclient.Header;
import it.reply.selly.mobileapp.utility.HttpClient;
import it.reply.selly.mobileapp.utility.MessagesListAdapter;
import it.reply.selly.mobileapp.utility.SellyMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String MESSAGE_POST_PARAM_KEY = "text";
    private static final String RESPONSE_TEXT = "responseText";
    private static final String RESPONSE_IMAGE_LINK = "responseImageLink";
    private static final String RESPONSE_IMAGE_NAME = "responseImageName";
    private static final String AMAZON = "amazon";
    private static final String TRAVEL_INSURANCE = "travel_insurance";

    private EditText inputForm;

    private MessagesListAdapter adapter;
    private List<SellyMessage> messageList = new ArrayList<>();

    private JSONObject lastResponseFromChatbot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getViewReferences();
        initAdapter();
        loadWelcomeMessage();
    }

    private void initAdapter() {
        final ListView listView = (ListView) findViewById(R.id.list_view_messages);
        adapter = new MessagesListAdapter(this, messageList);
        listView.setAdapter(adapter);
    }

    private void loadWelcomeMessage(){
        messageList.add(new SellyMessage(getString(R.string.welcome_message), true));
        adapter.notifyDataSetChanged();
    }

    private void getViewReferences() {
        inputForm = (EditText) findViewById(R.id.inputMsg);
    }

    public void onSendMessageButtonClick(View view) {
        if ("si".equalsIgnoreCase(inputForm.getText().toString()) || "sì".equals(inputForm.getText().toString())
                || "ok".equalsIgnoreCase(inputForm.getText().toString()) || "va bene".equalsIgnoreCase(inputForm.getText().toString())){
            messageList.add(new SellyMessage(inputForm.getText().toString(), false));
            adapter.notifyDataSetChanged();
            showUpSellingProposal();
        } else if ("no".equalsIgnoreCase(inputForm.getText().toString())){
            messageList.add(new SellyMessage(inputForm.getText().toString(), false));
            defaultAnswerForYesInput();
        } else {
            messageList.add(new SellyMessage(inputForm.getText().toString(), false));
            adapter.notifyDataSetChanged();
            sendMessageToServer(inputForm.getText().toString(), false);
        }

        inputForm.setText("");
    }

    private void sendMessageToServer(final String messageToSend, boolean isMock) {
        if (isMock){
            messageList.add(new SellyMessage("Server says bla bla bla", false));
            adapter.notifyDataSetChanged();
            return;
        }

        HttpClient.post("/", new RequestParams(MESSAGE_POST_PARAM_KEY, messageToSend), new JsonHttpResponseHandler() {
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.e(TAG, "A problem occurred");
                Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "Message received");

                lastResponseFromChatbot = response;
                try {
                    messageList.add(new SellyMessage(response.getString(RESPONSE_TEXT), true));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                adapter.notifyDataSetChanged();

//                /*We are bad people for doing this (and other fancy stuff). We are out of time, shall God forgive us*/
//                try {
//                    messageList.add(new SellyMessage(response.getString(RESPONSE_TEXT), true));
//                    messageList.add(new SellyMessage(response.getString(RESPONSE_TEXT), true, true,
//                            response.getString(RESPONSE_IMAGE_LINK), response.getString(RESPONSE_IMAGE_NAME)));
//                    adapter.notifyDataSetChanged();
//                } catch (JSONException e) {
//                    Log.e(TAG, "An error occurred while transforming JSON received from BE");
//                    adapter.notifyDataSetChanged();
//                }
            }
        });
    }

    private void showUpSellingProposal(){
        if (lastResponseFromChatbot == null){
            defaultAnswerForYesInput();
            return;
        }

        try {
            if (lastResponseFromChatbot.getString(RESPONSE_IMAGE_NAME).equalsIgnoreCase(AMAZON)){
                showImageAndUrl();
                showDiscountMessage();
                createFakeNotitication();
            }
            if (lastResponseFromChatbot.getString(RESPONSE_IMAGE_NAME).equalsIgnoreCase(TRAVEL_INSURANCE)){
                showGeolacalizationActivation();
                showTravelInsuranceMessage();
                showImageAndUrl();
            }
        } catch (JSONException e) {
            defaultAnswerForYesInput();
        }
    }

    private void showImageAndUrl() throws JSONException {
        messageList.add(new SellyMessage(lastResponseFromChatbot.getString(RESPONSE_TEXT), true, true,
                lastResponseFromChatbot.getString(RESPONSE_IMAGE_LINK), lastResponseFromChatbot.getString(RESPONSE_IMAGE_NAME)));
        adapter.notifyDataSetChanged();
    }

    private void showDiscountMessage(){
        messageList.add(new SellyMessage(getString(R.string.amazon_discount), true));
    }

    private void showTravelInsuranceMessage(){
        messageList.add(new SellyMessage(getString(R.string.travel_insurance), true));
        adapter.notifyDataSetChanged();
    }

    private void showGeolacalizationActivation(){
        messageList.add(new SellyMessage(getString(R.string.travel_insurance_activation), true));
        adapter.notifyDataSetChanged();
    }

    private void defaultAnswerForYesInput(){
        messageList.add(new SellyMessage(getString(R.string.ok), true));
        adapter.notifyDataSetChanged();
    }

    private void createFakeNotitication(){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.gmail)
                        .setContentTitle("My notification")
                        .setContentText("Buono sconto Amazon per te");

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, mBuilder.build());
    }
}
