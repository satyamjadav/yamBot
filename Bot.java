import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.JsonParser;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.watson.language_translator.v3.*;
import com.ibm.watson.language_translator.v3.model.TranslateOptions;
import com.ibm.watson.language_translator.v3.model.TranslationResult;
import org.jibble.pircbot.*;
import java.net.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// create Bot object, which extends PircBot
public class Bot extends PircBot {

    // default constructor
    public Bot() {
        // set name to yamBot
        this.setName("yamBot");
    }

    // this method is called whenever a message is sent to the #pircbot channel
    public void onMessage(String channel, String sender, String login, String hostname, String message) {

        // split the message sent by user into 2 parts, keyWord (first word) and parameters (rest of the message)
        String[] messageArr = message.split(" ", 2);

        String keyWord = messageArr[0];
        String parameters = messageArr[1];

        String API_url;

        // if the keyWord is !weather and the second word is all numbers
        if(keyWord.toLowerCase().equalsIgnoreCase("!weather") && isNumeric(parameters)) {

            // constructs URL using parameters
            API_url = "https://api.openweathermap.org/data/2.5/weather?zip=" + parameters + ",us&appid=APIKEY";

            // calls weatherAPI method
            callWeatherAPI(API_url);
        }
        else if(keyWord.equalsIgnoreCase("!weather") && !isNumeric(parameters)) {
            // if the keyWord is weather and the second word is not all numbers
            sendMessage("#pircbot", "Input a numerical ZIP code");
        }
        else if(keyWord.equalsIgnoreCase("!translate")) {
            // split the parameters into two separate parts and then call API
            String languageISO;
            String translateThis;
            String[] splitCommand = parameters.split(" ", 2);
            languageISO = splitCommand[0];
            translateThis = splitCommand[1];
            callTranslateAPI(languageISO, translateThis, sender);
        }
    }

    // checks if a String is all numbers
    public boolean isNumeric(String s) {
        for(int i = 0; i < s.length(); i++) {
            if(!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // calling API asynchronously using Java 11 HttpClient
    public void callWeatherAPI(String url) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::parseWeatherAPI)
                .join();
    }

    // parses the JSON file received by the openweathermap.com API using GSON
    public void parseWeatherAPI(String response) {
        JsonObject object = JsonParser.parseString(response).getAsJsonObject();

        // if we can't find ZIP code entered by user
        if(object.getAsJsonObject().get("cod").getAsString().equals("404")) {
            sendMessage("#pircbot", "Cannot find requested ZIP code, please try again");
        }
        else {
            // otherwise parse the JSON for information and send a String
            String desc = object.getAsJsonArray("weather").get(0).getAsJsonObject().get("description").getAsString();
            String cityName = object.getAsJsonObject().get("name").getAsString();
            double low = object.getAsJsonObject().get("main").getAsJsonObject().get("temp_min").getAsDouble();
            double high = object.getAsJsonObject().get("main").getAsJsonObject().get("temp_max").getAsDouble();

            int lowF = convertToFahrenheit(low);
            int highF = convertToFahrenheit(high);

            sendMessage("#pircbot", cityName + " currently has " + desc + " with a low of " + lowF + " and a high of " + highF);
        }
    }

    // converts Kelvin to Fahrenheit
    public static int convertToFahrenheit(double n) {
        return (int)Math.round((1.8 * (n - 273)) + 32);
    }

    // connects to IBM Cloud Translation API
    public void callTranslateAPI(String language, String input, String user) {
        IamAuthenticator authenticator = new IamAuthenticator("APIKEY");
        LanguageTranslator languageTranslator = new LanguageTranslator("2018-05-01", authenticator);
        languageTranslator.setServiceUrl("https://api.us-south.language-translator.watson.cloud.ibm.com/instances/14dcd7d8-a5c4-4bd2-a41c-cd4bf5edc4cc");

        try {
            TranslateOptions translateOptions = new TranslateOptions.Builder()
                    .addText(input)
                    .modelId(language)
                    .build();

            TranslationResult result = languageTranslator.translate(translateOptions)
                    .execute().getResult();

            parseTranslationAPI(result.toString(), user);
        }
        catch(ServiceResponseException e) {
            sendMessage("#pircbot", "Service returned status code " + e.getStatusCode() + ": " + e.getMessage());
        }
    }

    public void parseTranslationAPI(String response, String user) {
        JsonObject object = JsonParser.parseString(response).getAsJsonObject();
        String trans = object.get("translations").getAsJsonArray().get(0).getAsJsonObject().get("translation").getAsString();

        sendMessage("#pircbot", user + ": " + trans);
    }
}