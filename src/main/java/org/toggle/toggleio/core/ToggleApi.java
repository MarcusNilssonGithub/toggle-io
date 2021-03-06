package org.toggle.toggleio.core;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import net.jstick.api.Device;
import org.toggle.toggleio.application.controller.Controller;
import net.jstick.api.Tellstick;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * This class is responsible for register the ip on the API or update the IP on the API with help of the token received.
 * The class requires that the toggle-api is up and running or else you will get a ConnectionException
 */
public class ToggleApi {
    private Controller controller;
    private Tellstick tellstick;
    private String registerUrl;
    private String updateUrl;
    private String removeUrl;

    /**
     * @param controller
     * @param registerUrl
     * @param updateUrl
     */
    public ToggleApi(Controller controller, String registerUrl, String updateUrl, String removeUrl) {
        this.controller = controller;
        this.registerUrl = registerUrl;
        this.updateUrl = updateUrl;
        this.removeUrl = removeUrl;
        this.tellstick = new Tellstick();
    }

    /**
     * This function will request a new slot on the API or
     * update the IP on the API to the current one if the TOKEN is NOT expired
     */
    public void removeDevice(String token)throws IOException, JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", token);
        HttpURLConnection con = sendJSONRequest(removeUrl, jsonObject);
        con.getResponseCode();
    }
    private HttpURLConnection sendJSONRequest(String endUrl, JSONObject jsonObject)throws IOException {
        URL url = null;

        try {
            url = new URL(endUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonObject.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }


            return con;
        } catch (Exception e) {
            throw new ConnectException("Could not connect to API");
        }
    }
    public void requestSlot() {
        int numberOfDevices = tellstick.getNumberOfDevices();
        if (numberOfDevices == 0) return;
        System.out.flush();
        System.out.println("Registering devices...");
        register();
    }

    private void register() {
        String token;
        ArrayList<Device> deviceList = tellstick.getDevices();
        for (int i = 0; i < deviceList.size(); i++) {
            Device device = deviceList.get(i);
            token = controller.getToken(device.getId());
            System.out.print("Name: " + device.getName() + ", ");
            try {
                if (token.length() == 0) throw new IOException();
                if (token.equals("0")) token = sendRequestRegister();
                else token = sendRequestUpdate(device);
                if (token.equals("0")) System.out.print("COULD NOT REGISTER DEVICE\n");
                else {
                    controller.setToken(device.getId(), token);
                    System.out.print("Token: " + token + "\n\n");
                }
            } catch (IOException | JSONException io) {
                System.out.print("COULD NOT REGISTER DEVICE\n");
            }
        }
    }

    private String sendRequestRegister() throws IOException, JSONException {
        URL url = null;

        try {
            url = new URL(registerUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                JSONObject jsonObject = new JSONObject(response.toString());
                System.out.println(con.getResponseCode());

                return (String) jsonObject.get("token");

            }
        } catch (Exception e) {
            throw new ConnectException("Could not connect to API");
        }


    }

    private String sendRequestUpdate(Device device) throws IOException, JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", controller.getToken(device.getId()));
        try {
            HttpURLConnection con = sendJSONRequest(updateUrl, jsonObject);
            if (con.getResponseCode() == 467) return sendRequestRegister();
            else if (con.getResponseCode() == 200) return controller.getToken(device.getId());
            else return "0";
        } catch (Exception e) {
            throw new ConnectException("Could not connect to API");
        }
    }
}
