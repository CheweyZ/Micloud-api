package info.micloud.api.apipages;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import info.micloud.api.database.ConnectionPool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class PageAddVisit extends APIPage {

	public PageAddVisit() {
		super("adddoctorvisit", true);
	}

	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	@Override
	public void onLoad(String sessionId, int userId, Map<String, List<String>> params, HttpRequest currentRequest,
			ChannelHandlerContext ctx) {
		Connection connection = ConnectionPool.allocateConnection();
		try {
			if (!params.containsKey("date") || !params.containsKey("patientId")
					|| !params.containsKey("height") || !params.containsKey("weight")
					|| !params.containsKey("supplement")){
				JSONObject error = new JSONObject();
				error.put("boxType", "error");
				error.put("message", "Missing date, patientId, height, weight and/or supplement");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			Date date = null;
			try {
				date = dateFormat.parse(params.get("date").get(0));
			} catch (ParseException e) {
				JSONObject error = new JSONObject();
				error.put("message", "Invalid date format (YYYY-MM-DD)");
				error.put("boxType", "error");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			
			int patientId = -1;
			try {
				patientId = Integer.parseInt(params.get("patientId").get(0));
			} catch (NumberFormatException e) {
				JSONObject error = new JSONObject();
				error.put("message", "Invalid patientId (NaN)");
				error.put("boxType", "error");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			
			JSONObject data = new JSONObject();
			try {
				data.put("height", Integer.parseInt(params.get("height").get(0)));
				data.put("weight", Integer.parseInt(params.get("weight").get(0)));
			} catch (NumberFormatException e){
				JSONObject error = new JSONObject();
				error.put("message", "height and/or weight is not a number");
				error.put("boxType", "error");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			
			try {
				JSONArray supplements = new JSONArray(params.get("supplement").get(0));
				data.put("supplement", supplements);
			} catch (JSONException e){
				JSONObject error = new JSONObject();
				error.put("message", "supplement is not a valid JSON array");
				error.put("boxType", "error");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			
			
			PreparedStatement statement = connection.prepareStatement("INSERT INTO `visits` (visitDate,patientId,doctorId,data) VALUES(?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
			
			statement.setDate(1, new java.sql.Date(date.getTime()));
			statement.setInt(2, patientId);
			statement.setInt(3, userId);
			statement.setString(4, data.toString());
			
			statement.executeUpdate();
			ResultSet set = statement.getGeneratedKeys();
			int recordId = -1;
			if (set.next()){
				recordId = set.getInt(1);
			} else {
				set.close();
				statement.close();
				JSONObject error = new JSONObject();
				error.put("message", "SQL Error: visits table returned no visit id");
				error.put("boxType", "error");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			set.close();
			statement.close();
			
			statement = connection.prepareStatement("SELECT `visits` FROM `patients` WHERE `patientId`=?");
			statement.setInt(1, patientId);
			
			set = statement.executeQuery();
			if (set.next()){
				byte[] visits = set.getBytes("visits");
				ByteBuffer wrapped = null;
				if (visits != null){
					wrapped = ByteBuffer.allocate(visits.length + 4);
					wrapped.putInt(recordId);
					wrapped.put(visits);
				} else {
					wrapped = ByteBuffer.allocate(4);
					wrapped.putInt(recordId);
				}
				set.close();
				statement.close();
				
				statement = connection.prepareStatement("UPDATE `patients` SET `visits`=? WHERE `patientId`=?");
				statement.setBytes(1, wrapped.array());
				statement.setInt(2, patientId);
				
				statement.execute();
				statement.close();
				wrapped.clear();
			} else {
				set.close();
				statement.close();
				JSONObject error = new JSONObject();
				error.put("message", "SQL Error: patient with id "+patientId+" does not exist");
				error.put("boxType", "error");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			
			JSONObject response = new JSONObject();
			response.put("status", "success");
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