package srv;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Dienste des Servers. Hier wird das Protokoll f�r den Nachrichten-Transfer
 * implementiert.
 * 
 * @author Gruppe5
 */
/*
 * Ohne @Path("") Fehler: The ResourceConfig instance does not contain any root
 * resource classes.
 */
@Path("")
public class ServerResponse {
	/** Benutzerliste. */
	static Map<String, Benutzer> map = new HashMap<>();
	/**
	 * Abfangen einer Message des Benutzers. Wenn das Format zul�ssig ist sendet
	 * der Server 201.Wenn das Format nicht zul�ssig ist sendet der Server 400.
	 *
	 * @param jsonFormat
	 *            - Nachricht des Benutzers.
	 * @return Response - Antwort des Servers an den Client.
	 * @throws JSONException
	 *             - Bei Problemen mit Json.
	 * @throws ParseException
	 *             - Bei Problemen mit Umwandeln.
	 */
	private static final String uri = "http://141.19.142.57:5001";

	@PUT
	@Path("/send")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response putMessage(String jsonFormat) {
		Message message = null;
		JSONObject j = null;
		Date date = null;
		Benutzer benutzer = null;
		Client client = Client.create();
		
		try {
			j = new JSONObject(jsonFormat);
		} catch (JSONException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (j.has("date")) {
			try {
				date = Message.stringToDate(j.optString("date"));
			} catch (ParseException e) {
				e.printStackTrace();
				return Response.status(Status.BAD_REQUEST).build();
			}
		} else {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (j.has("from") && j.has("to") && j.has("text") && j.has("token")) {
			try {
				message = new Message(j.getString("token"), j.getString("from"), j.getString("to"), date,
						j.getString("text"), j.optInt("sequence"));
			} catch (JSONException e) {
				e.printStackTrace();
				return Response.status(Status.BAD_REQUEST).build();
			}
		} else {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (!map.containsKey(j.optString("to"))) {
			map.put(j.optString("to"), new Benutzer(j.optString("to")));
		}
		benutzer = map.get(j.optString("to"));
		if (Message.isJSONValid(jsonFormat) && Message.isTokenValid(message.token) && message.token != null
				&& message.from != null && message.to != null && message.date != null && message.text != null) {
			JSONObject json = new JSONObject();
			try {
				json.put("token", j.optString("token"));
				json.put("pseudonym", j.optString("from"));
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			// String response=client.resource(uri +"/auth")
			// .accept(MediaType.APPLICATION_JSON)
			// .type(MediaType.APPLICATION_JSON)
			// .post(String.class,json);
			// JSONObject antw;
			// client.destroy();
			// try {
			// antw = new JSONObject(response);
			// } catch (JSONException e2) {
			//
			// e2.printStackTrace();
			// return Response.status(Status.BAD_REQUEST).build();
			// }
			// try {
			// if(!antw.get("success").equals("true")){
			// return Response.status(Status.UNAUTHORIZED).build();
			// }
			// } catch (JSONException e1) {
			// e1.printStackTrace();
			// return Response.status(Status.BAD_REQUEST).build();
			// }
			ClientResponse response = client.resource(uri + "/auth").accept(MediaType.APPLICATION_JSON)
					.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, json.toString());

			// try {
			// benutzer.expDate=Message.stringToDate(antw.optString("expire-date"));
			// } catch (ParseException e1) {
			//
			// e1.printStackTrace();
			// return Response.status(Status.BAD_REQUEST).build();
			// }
			if (response.getStatus() != 200) {
				return Response.status(Status.UNAUTHORIZED).build();
			}
			message = new Message(j.optString("token"), j.optString("from"), j.optString("to"), date,
					j.optString("text"), benutzer.sequence += 1);
			benutzer.msgliste.offer(message);
			try {
				return Response.status(Status.CREATED).entity(message.datenKorrekt().toString()).build();
			} catch (JSONException e) {
				e.printStackTrace();
				return Response.status(Status.BAD_REQUEST).build();
			}
			// } else if (message.date.equals(benutzer.expDate) ||
			// message.date.after(benutzer.expDate)) {
			// return Response.status(Status.UNAUTHORIZED).build();
			// } else if (!Message.isTokenValid(message.token)) {
			//
			// return Response.status(Status.UNAUTHORIZED).build();
		} else {
			return Response.status(Status.BAD_REQUEST).entity("Bad format").build();
		}
	}

	/**
	 * Der Client holt die Nachrichten vom Server mit GET �ber --> @Path.
	 *
	 * @param user_id
	 *            - Der Name des Benutzers
	 * @param sequence
	 *            - Die Sequenznummer der letzten erhaltenen Nachricht.
	 * @return Response - Antwort des Servers
	 * @throws JSONException
	 *             - Bei Problemen mit Json
	 */
	@GET
	@Path("/messages/{user_id}/{sequence_number}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMessage(@PathParam("user_id") String user_id, @PathParam("sequence_number") int sequence,
			@Context HttpHeaders header) {

		JSONArray jArray = null;
		MultivaluedMap<String, String> hmap = header.getRequestHeaders();
		String token = hmap.get("Authorization").get(0).substring(6);
		Client client = Client.create();
		String antwort;
		JSONObject antwJ;

		if (hmap.get("Authorization") == null || hmap.get("Authorization").isEmpty()) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		if (map.containsKey(user_id)) {
			if (!map.get(user_id).msgliste.isEmpty()) {
				Benutzer benutzer = map.get(user_id);
				if (token.equals(benutzer.token)) {
					if (new Date().before(benutzer.expDate)) {
						try {
							jArray = benutzer.getMessageAsJson(sequence);
						} catch (JSONException e) {
							e.printStackTrace();
							return Response.status(Status.BAD_REQUEST).build();
						}
						benutzer.deleteMsg(sequence);
						if (jArray.length() == 0) {
							return Response.status(Status.NO_CONTENT).build();
						}
						try {
							return Response.status(Status.OK).entity(jArray.toString(3))
									.type(MediaType.APPLICATION_JSON).build();
						} catch (JSONException e) {
							e.printStackTrace();
							return Response.status(Status.BAD_REQUEST).build();
						}
					}

				} else {
					JSONObject jsonobject = new JSONObject();
					try {
						jsonobject.put("token", token);
						jsonobject.put("pseudonym", benutzer.name);
					} catch (JSONException e) {
						e.printStackTrace();
						return Response.status(Status.BAD_REQUEST).build();
					}
					antwort = client.resource(uri + "/auth").accept(MediaType.APPLICATION_JSON)
							.type(MediaType.APPLICATION_JSON).post(String.class, jsonobject);
					client.destroy();

					try {
						antwJ = new JSONObject(antwort);

					} catch (JSONException e) {
						e.printStackTrace();
						return Response.status(Status.BAD_REQUEST).build();

					}
					if (antwJ.optString("success").equals("true")) {
						try {
							benutzer.expDate = Message.stringToDate(antwJ.optString("expire-date"));
						} catch (ParseException e) {
							e.printStackTrace();
							return Response.status(Status.BAD_REQUEST).build();
						}

						try {
							return Response.status(Status.OK).entity(jArray.toString(3))
									.type(MediaType.APPLICATION_JSON).build();
						} catch (JSONException e) {
							e.printStackTrace();
							return Response.status(Status.BAD_REQUEST).build();
						}

					} else {
						return Response.status(Status.UNAUTHORIZED).build();
					}

				}
			} else {
				return Response.status(Status.NO_CONTENT).build();
			}
		} else {

			return Response.status(Status.NO_CONTENT).build();
		}
		return Response.status(Status.NO_CONTENT).build();
	}

	/**
	 * @see getMessage oben.
	 * @param user_id
	 * @return Response
	 * @throws JSONException
	 */
	@GET
	@Path("/messages/{user_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMessage(@PathParam("user_id") String user_id, @Context HttpHeaders header) throws JSONException {

		return getMessage(user_id, 0, header);
	}

}