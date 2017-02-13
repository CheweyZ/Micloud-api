package info.micloud.api.apipages;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import info.micloud.api.database.BloodType;
import info.micloud.api.database.ConnectionPool;
import info.micloud.api.database.Gender;
import info.micloud.api.main.Configuration;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class PageGetPatientProfile extends APIPage {

	public PageGetPatientProfile() {
		super("getpatientprofile", true);
	}

	@Override
	public void onLoad(String sessionId, int userId, Map<String, List<String>> params, HttpRequest currentRequest,
			ChannelHandlerContext ctx) {
		if (params.containsKey("patientId")){
			Connection connection = ConnectionPool.allocateConnection();
			try {
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM `patients` WHERE `patientId`=?;");
				statement.setString(1, params.get("patientId").get(0));
				ResultSet set = statement.executeQuery();
				if (set.next()){
					JSONObject response = new JSONObject();
					response.put("boxType", "patientProfile");
					response.put("firstName", set.getString("firstName"));
					response.put("lastName", set.getString("lastName"));
					response.put("dob", dateFormat.format(new java.util.Date(set.getDate("dob").getTime())));
					response.put("bloodType", BloodType.fromSqlId(set.getInt("bloodType")).getName());
					response.put("gender", Gender.fromSqlId(set.getInt("gender")).getName());
					response.put("patientId", params.get("patientId").get(0));
					
					//VISITS
					InputStream binary = set.getBinaryStream("visits");
					if (binary != null){
						int visitsAmount = Configuration.SHOW_LAST_VISITS;
						set.close();
						Map<Integer, String> doctorNameCache = new HashMap<>();
						JSONArray visits = new JSONArray();
						for (int i = 0;i < visitsAmount;i++){
							byte[] nextInt = new byte[4];
							int l = binary.read(nextInt);
							int recordId = -1;
							if (l == 4){
								ByteBuffer wrapped = ByteBuffer.wrap(nextInt);
								recordId = wrapped.getInt();
							} else {
								break;
							}
							PreparedStatement visitStatement = connection.prepareStatement("SELECT `visitDate`, `data`, `doctorId` FROM `visits` WHERE `visitId`=?;");
							visitStatement.setInt(1, recordId);
							ResultSet visitSet = visitStatement.executeQuery();
							if (visitSet.next()){
								Date visitDate = visitSet.getDate("visitDate");
								String visitDateString = dateFormat.format(visitDate);
								JSONObject data = new JSONObject(visitSet.getString("data"));
								JSONObject visitData = new JSONObject();
								visitData.put("date", visitDateString);
								int doctorId = visitSet.getInt("doctorId");
								visitSet.close();
								String doctorName = "";
								if (doctorNameCache.containsKey(doctorId)){
									doctorName = doctorNameCache.get(doctorId);
									visitData.put("doctor", doctorName);
								} else {
									String name = lookupDoctorName(connection, doctorId);
									doctorNameCache.put(doctorId, name);
									doctorName = name;
									visitData.put("doctor", doctorName);
								}
								if (data.has("weight")){
									visitData.put("weight", data.get("weight"));
								}
								if (data.has("height")){
									visitData.put("height", data.get("height"));
								}
								if (data.has("supplement")){
									visitData.put("supplement", data.get("supplement"));
								}
								visits.put(visitData);
							}
						}
						response.put("visits", visits);
					} else {
						JSONArray visits = new JSONArray();
						response.put("visits", visits);
					}
					writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.OK, response);
					return;
				} else {
					set.close();
					JSONObject error = new JSONObject();
					error.put("boxType", "error");
					error.put("message", "Patient does not exist");
					writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
					return;
				}
			} catch (SQLException e) {
				e.printStackTrace();
				
				JSONObject error = new JSONObject();
				error.put("message", "500 - SQLException on page "+this.pageName);
				error.put("boxType", "error");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.INTERNAL_SERVER_ERROR, error);
				return;
			} catch (IOException e){
				e.printStackTrace();
				
				JSONObject error = new JSONObject();
				error.put("message", "500 - IOException on page "+this.pageName);
				error.put("boxType", "error");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.INTERNAL_SERVER_ERROR, error);
				return;
			} finally {
				ConnectionPool.unallocateConnection(connection);
			}
		} else {
			JSONObject error = new JSONObject();
			error.put("boxType", "error");
			error.put("message", "No patientId specified");
			writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
			return;
		}
	}
	
	private String lookupDoctorName(Connection connection, int id) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("SELECT `firstName`, `lastName` FROM `users` WHERE `userid`=?");
		statement.setInt(1, id);
		ResultSet set = statement.executeQuery();
		if (set.next()){
			String firstName = set.getString("firstName");
			String lastName = set.getString("lastName");
			set.close();
			
			String name = firstName + " " + lastName;
			return name;
		} else {
			return "Unknown";
		}
	}
}
