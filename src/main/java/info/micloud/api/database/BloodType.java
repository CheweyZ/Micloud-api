package info.micloud.api.database;

public enum BloodType {

	O_POSITIVE(0, "O Positive"),O_NEGATIVE(1, "O Negative"),O_UNKNOWN(2, "O Unknown"),
	A_POSITIVE(3, "A Positive"),A_NEGATIVE(4, "A Negative"),A_UNKNOWN(5, "A Unknown"),
	B_POSITIVE(6, "B Positive"),B_NEGATIVE(7, "B Negative"),B_UNKNOWN(8, "B Unknown"),
	AB_POSITIVE(9, "AB Positive"),AB_NEGATIVE(10, "AB Negative"),AB_UNKNOWN(11, "AB Unknown"),
	UNKNOWN(12, "Unknown");
	
	private final int sqlId;
	private final String name;
	private BloodType(int sqlId, String name){
		this.sqlId = sqlId;
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}
	
	public int getSqlId(){
		return this.sqlId;
	}
	
	public static BloodType fromSqlId(int id){
		for (BloodType type : values()){
			if (type.getSqlId() == id){
				return type;
			}
		}
		return null;
	}
	
	public static BloodType fromName(String name){
		for (BloodType type : values()){
			if (type.getName().equalsIgnoreCase(name)){
				return type;
			}
		}
		return null;
	}
}
