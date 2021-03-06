package com.ibm.cfc.godsplan.assistant;

import java.util.Optional;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watson.developer_cloud.assistant.v1.Assistant;
import com.ibm.watson.developer_cloud.assistant.v1.model.Context;
import com.ibm.watson.developer_cloud.assistant.v1.model.InputData;
import com.ibm.watson.developer_cloud.assistant.v1.model.MessageOptions;
import com.ibm.watson.developer_cloud.assistant.v1.model.MessageOptions.Builder;
import com.ibm.watson.developer_cloud.assistant.v1.model.MessageResponse;

/**
 * A class to interact with the GodsPlan Watson Assistant
 */
public class WatsonAssistantBot
{

   private static final String WORKSPACE_ID = "e9fc9a95-fbfb-4210-b8e8-bd40cb3bebe2";
   private static final String ENC_PASSWD = "5oZIyaeGU1Pc";
   private static final String USERNAME = "333a833c-fe0a-4f10-af9e-739f368ff725";
   private static final String REST_API_VERSION = "2018-02-16";
   private final Assistant servissimo;
   private Optional<MessageResponse> lastResponse;
   protected static final Logger logger = LoggerFactory.getLogger(WatsonAssistantBot.class);

   /**
    * zero argument constructor
    */
   public WatsonAssistantBot()
   {
      this.servissimo = new Assistant(REST_API_VERSION);
      this.servissimo.setUsernameAndPassword(USERNAME, ENC_PASSWD);
      this.lastResponse = Optional.empty();
   }

   /**
    * @param context
    *           context to provide with the input
    * @param input
    *           the message to send to Watson assistant
    * @return the return text from Watson assistant
    */
   public String sendAssistantMessage(Optional<Context> context, Optional<InputData> input)
   {
      logger.info("Querying Watson with input '{}'", input);
      MessageOptions options = buildOptions(context, input);
      MessageResponse resp = this.servissimo.message(options).execute();
      lastResponse = Optional.ofNullable(resp);
      logger.trace("Watson assistant response: '{}'", resp);
      return getResponseText(resp);
   }

   private String getResponseText(MessageResponse resp)
   {
      StringJoiner joiner = new StringJoiner(" | ");
      for (String output : resp.getOutput().getText())
      {
         joiner.add(output);
      }
      return joiner.toString();
   }

   private MessageOptions buildOptions(Optional<Context> context, Optional<InputData> input)
   {
      Builder msgBuilder = new MessageOptions.Builder(WORKSPACE_ID);
      if (context.isPresent())
      {
         msgBuilder = msgBuilder.context(context.get());
      }
      if (input.isPresent() && context.isPresent())
      {
         msgBuilder = msgBuilder.input(input.get());
      }
      return msgBuilder.build();
   }

   /**
    * @return the most recent context
    */
   public Optional<Context> getLastContext()
   {
      Context context = null;
      if (lastResponse.isPresent())
      {
         context = lastResponse.get().getContext();
      }
      return Optional.ofNullable(context);
   }
}
