package com.project.app;

import com.project.datasource.DbInitializer;

public class ProjectClientApplication {
	
	public static void main(String[] args) {
		DbInitializer.init();
	}

}
