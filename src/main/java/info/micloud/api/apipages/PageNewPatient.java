package info.micloud.api.apipages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import info.micloud.api.database.BloodType;
import info.micloud.api.database.ConnectionPool;
import info.micloud.api.database.Gender;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class PageNewPatient extends APIPage {

	public PageNewPatient() {
		super("makepatient", true);
	}

	@Override
	public void onLoad(String sessionId, int userId, Map<String, List<String>> params, HttpRequest currentRequest,
			ChannelHandlerContext ctx) {
		Connection connection = ConnectionPool.allocateConnection();
		try {
			if (!params.containsKey("firstName") || !params.containsKey("lastName")
					|| !params.containsKey("bloodType") || !params.containsKey("dob")
					|| !params.containsKey("gender")){
				JSONObject error = new JSONObject();
				error.put("boxType", "error");
				error.put("message", "Missing firstName, lastName, bloodType, dob and/or gender");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			BloodType bloodType = BloodType.fromName(params.get("bloodType").get(0));
			if (bloodType == null){
				JSONObject error = new JSONObject();
				error.put("message", "Invalid blood type");
				error.put("boxType", "error");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			Gender gender = Gender.fromName(params.get("gender").get(0));
			if (gender == null){
				JSONObject error = new JSONObject();
				error.put("message", "Invalid gender");
				error.put("boxType", "error");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			Date dob = null;
			try {
				dob = dateFormat.parse(params.get("dob").get(0));
			} catch (ParseException e) {
				JSONObject error = new JSONObject();
				error.put("message", "Invalid date format (YYYY-MM-DD)");
				error.put("boxType", "error");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			PreparedStatement statement = connection.prepareStatement("INSERT INTO `patients` (firstName, lastName, dob, bloodType, gender) VALUES(?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
			
			String firstName = params.get("firstName").get(0);
			firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
			statement.setString(1, firstName);
			
			String lastName = params.get("lastName").get(0);
			lastName = lastName.substring(0, 1).toUpperCase() + lastName.substring(1).toLowerCase();
			statement.setString(2, lastName);
			
			statement.setDate(3, new java.sql.Date(dob.getTime()));
			statement.setInt(4, bloodType.getSqlId());
			statement.setInt(5, gender.getSqlId());
			
			
			statement.executeUpdate();
			ResultSet set = statement.getGeneratedKeys();
			int recordId = -1;
			if (set.next()){
				recordId = set.getInt(1);
			}
			statement.close();
			
			JSONObject response = new JSONObject();
			response.put("status", "success");
			response.put("patientId", recordId);
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
