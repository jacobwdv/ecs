/* _______________________________________________________ {COPYRIGHT-TOP} _____
 * IBM Confidential
 * IBM Lift CLI Source Materials
 *
 * (C) Copyright IBM Corp. 2018  All Rights Reserved.
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 * _______________________________________________________ {COPYRIGHT-END} _____*/

package com.ibm.cfc.godsplan.cloudant;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.cfc.godsplan.cloudant.model.LocationContext;
import com.ibm.cfc.godsplan.maps.model.GoogleAddressInformation;

/**
 * Responsible for persisting information related to chat context. Stores documents in {@link #DB}.
 * 
 * See {@link LocationContext} for the data stored in this database.
 */
public class LocationPersistence
{
   /**
    * Cloudant database that contains documents defined by {@link LocationContext}
    */
   public static final String DB = "locationcontext";
   
   /**
    * Field in {@link LocationContext} that represents {@link GoogleAddressInformation#getLongitude()}
    */
   public static final String LONGITUDE = "longitude";

   /**
    * Field in {@link LocationContext} that represents {@link GoogleAddressInformation#getLatitude()}
    */
   public static final String LATITUDE = "latitude";

   /**
    * Field in {@link LocationContext} that represents {@link GoogleAddressInformation#getFormattedAddress()}
    */
   public static final String FORMATTED_ADDRESS = "formattedAddress";

   /**
    * Field in {@link LocationContext} that represents {@link LocationContext#getAddressConfirmed()}
    */
   public static final String CONFIRMED_ADDRESS = "confirmedAddress";

   /**
    * Field in {@link LocationContext} that holds data in {@link GoogleAddressInformation}
    */
   public static final String LOCATION = "location";

   protected static final Logger logger = LoggerFactory.getLogger(LocationPersistence.class);

   
   Database db;
   JsonDocumentComposer compose;
   JsonParser parser;

   /**
    * 
    * @param locationDb
    */
   public LocationPersistence(Database locationDb)
   {
      this.db = locationDb;
      compose = new JsonDocumentComposer();
      parser = new JsonParser();
   }

   /**
    * 
    * @param phoneNumber
    * @param address
    */
   public void persist(String phoneNumber, GoogleAddressInformation address)
   {
      logger.info("saving address information for '{}'", phoneNumber);
      try (InputStream is = db.find(phoneNumber);)
      {
         JsonElement doc = compose.jsonFromStream(is);
         JsonObject json = doc.getAsJsonObject();
         addLocation(address,json);
         db.update(json);
      }
      catch (NoDocumentException e)
      {
         JsonObject json = compose.blankDocument(phoneNumber);
         addLocation(address, json);
         db.save(json);
      }
      catch (IOException e1)
      {
         e1.printStackTrace(); // log or rethrow as somethin else our app handles
      }
   }
   
   /**
    * 
    * @param phoneNumber
    * @return Optional<LocationContext>
    */
   public Optional<LocationContext> retrieve(String phoneNumber)
   {
      logger.info("retrieving address information for '{}'", phoneNumber);
      Optional<LocationContext> locationContext;
      try
      {
         locationContext = Optional.of(db.find(LocationContext.class, phoneNumber));
      }
      catch (NoDocumentException nde)
      {
         logger.info("No address information for '{}' found", phoneNumber);
         locationContext = Optional.empty();
      }

      return locationContext;
   }
   
   /**
    * 
    * @param phoneNumber
    * @param confirm
    *           true if user confirmed address is correct, false if user says address is incorrect
    */
   public void persistAddressConfirmation(String phoneNumber, boolean confirm)
   {
      logger.info("saving address confirmation for '{}'", phoneNumber);
      try (InputStream is = db.find(phoneNumber);)
      {
         JsonElement doc = compose.jsonFromStream(is);
         JsonObject json = doc.getAsJsonObject();
         json.addProperty(CONFIRMED_ADDRESS, confirm);
         db.update(json);
      }
      catch (NoDocumentException e)
      {
         logger.info("no address found for '{}' to confirm, phoneNumber");
      }
      catch (IOException e1)
      {
         e1.printStackTrace(); // log or rethrow as somethin else our app handles
      }
   }
   
   /**
    * 
    * @param phoneNumber
    */
   public void remove(String phoneNumber)
   {
      logger.info("removing location context for '{}'", phoneNumber);
      try (InputStream is = db.find(phoneNumber);)
      {
         JsonElement doc = compose.jsonFromStream(is);
         JsonObject json = doc.getAsJsonObject();
         db.remove(json);
      }
      catch (NoDocumentException e)
      {
         logger.info("no location context found for '{}'", phoneNumber);
      }
      catch (IOException e1)
      {
         e1.printStackTrace();
      }
   }
   
   /**
    * 
    * @param address
    * @param json
    */
   public void addLocation(GoogleAddressInformation address, JsonObject json)
   {
      JsonObject location = new JsonObject();
      location.addProperty(FORMATTED_ADDRESS, address.getFormattedAddress());
      location.addProperty(LATITUDE, address.getLatitude());
      location.addProperty(LONGITUDE, address.getLongitude());
      json.add(LOCATION, location);
      json.addProperty(CONFIRMED_ADDRESS, false);
   }
}
