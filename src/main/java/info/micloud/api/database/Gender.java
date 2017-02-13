package info.micloud.api.database;

public enum Gender {

	MALE(0, "Male"), FEMALE(1, "Female");
	
	private final int sqlId;
	private final String name;
	private Gender(int sqlId, String name){
		this.sqlId = sqlId;
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}
	
	public int getSqlId(){
		return this.sqlId;
	}
	
	public static Gender fromSqlId(int id){
		for (Gender type : values()){
			if (type.getSqlId() == id){
				return type;
			}
		}
		return null;
	}
	
	public static Gender fromName(String name){
		for (Gender type : values()){
			if (type.getName().equalsIgnoreCase(name)){
				return type;
			}
		}
		return null;
	}
}