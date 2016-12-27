package com.example.anas.anastriprecorder;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class PlaceJSONParser {

    /**
     * Receives a JSONObject and returns a list
     */
    List<HashMap<String, String>> parsePlace(JSONObject jObject) {

        JSONArray jPlaces = null;
        try {
            /** Retrieves all the elements in the 'places' array */
            jPlaces = jObject.getJSONArray("predictions");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        /** Invoking getPlaces with the array of json object
                  * where each json object represent a place
                 */
        return getPlaces(jPlaces);
    }

    private List<HashMap<String, String>> getPlaces(JSONArray jPlaces) {
        int placesCount = jPlaces.length();
        List<HashMap<String, String>> placesList = new ArrayList<>();
        HashMap<String, String> place;

        /** Taking each place, parses and adds to list object */
        for (int i = 0; i < placesCount; i++) {
            try {
                /** Call getPlace with place JSON object to parsePlace the place */
                place = getPlace((JSONObject) jPlaces.get(i));
                placesList.add(place);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return placesList;
    }

    /**
     * Parsing the Place JSON object
     */
    private HashMap<String, String> getPlace(JSONObject jPlace) {

        HashMap<String, String> place = new HashMap<>();

        String id;
        String reference;
        String description;

        try {

            description = jPlace.getString("description");
            id = jPlace.getString("id");
            reference = jPlace.getString("reference");

            place.put("description", description);
            place.put("_id", id);
            place.put("reference", reference);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return place;
    }

//================================================================================================
HashMap<String, String> parseDistance(JSONObject jObject) {
        JSONObject jDistance = null;
        try {
            /** Retrieves all the elements in the 'places' array */
            jDistance = jObject
                    .getJSONArray("rows")
                    .getJSONObject(0)
                    .getJSONArray("elements")
                    .getJSONObject(0)
                    .getJSONObject("distance");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return getDistance(jDistance);
    }



    private HashMap<String, String> getDistance(JSONObject jDistance) {

        HashMap<String, String> distance = new HashMap<>();

        String value;
        String text;

        try {

            value = String.valueOf(jDistance.getInt("value"));
            text = jDistance.getString("text");

            distance.put("value", value);
            distance.put("text", text);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return distance;
    }

    //==============================================================================================
}