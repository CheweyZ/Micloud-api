package info.micloud.api.apipages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import info.micloud.api.database.ConnectionPool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class PageSearchPatients extends APIPage {

	public PageSearchPatients() {
		super("patientsearch", true);
	}
	
	@Override
	public void onLoad(String sessionId, int userId, Map<String, List<String>> params, HttpRequest currentRequest,
			ChannelHandlerContext ctx) {
		Connection connection = ConnectionPool.allocateConnection();
		try {
			boolean familySearch = true;
			if (!params.containsKey("family") || params.get("family").get(0).equals("false") || params.get("family").get(0).equals("0")){
				familySearch = false;
			}
			PreparedStatement statement = null;
			if (familySearch){
				if (!params.containsKey("lastName")){
					JSONObject error = new JSONObject();
					error.put("boxType", "error");
					error.put("message", "lastName not specified (in family search)");
					writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
					return;
				}
				statement = connection.prepareStatement("SELECT `patientId`, `firstName`, `lastName`, `dob` FROM `patients` WHERE `lastName`=?;");
				statement.setString(1, params.get("lastName").get(0));
			} else {
				if (!params.containsKey("lastName") || !params.containsKey("firstName")){
					JSONObject error = new JSONObject();
					error.put("boxType", "error");
					error.put("message", "firstName and/or lastName not specified (in non-family search)");
					writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
					return;
				}
				statement = connection.prepareStatement("SELECT `patientId`, `firstName`, `lastName`, `dob` FROM `patients` WHERE `lastName`=? AND `firstName`=?;");
				statement.setString(1, params.get("lastName").get(0));
				statement.setString(2, params.get("firstName").get(0));
			}
			JSONArray patients = new JSONArray();
			
			ResultSet set = statement.executeQuery();
			while (set.next()){
				JSONObject patient = new JSONObject();
				patient.put("name", set.getString("firstName") + " " + set.getString("lastName"));
				patient.put("patientId", set.getInt("patientId"));
				patient.put("dob", dateFormat.format(set.getDate("dob")));
				patients.put(patient);
			}
			set.close();
			if (patients.length() == 0){
				JSONObject response = new JSONObject();
				response.put("boxType", "error");
				response.put("message", "Search returned 0 results");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.OK, response);
				return;
			}
			JSONObject response = new JSONObject();
			response.put("boxType", "patientSearchResults");
			response.put("content", patients);
			writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.OK, response);
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			
			JSONObject error = new JSONObject();
			error.put("message", "500 - SQLException on page "+this.pageName);
			error.put("boxType", "error");
			writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.INTERNAL_SERVER_ERROR, error);
			return;
		} finally {
			ConnectionPool.unallocateConnection(connection);
		}
	}
}