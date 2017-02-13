package info.micloud.api.main;

import info.micloud.api.MiCloud;

public class Bootstrap {

	public static void main(String[] args){
		
		//Throws a runtime exception and will exit the program if configuration is invalid
		Configuration.onLoad(args);
		
		MiCloud.getInstance().init();
	}
}
