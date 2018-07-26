package database_to_file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;


public class Attempt1 {
	
	private static final int PAGE_SIZE = 10000;
	private static final int RECORD_KEY = 1;
	
	
	public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException{ //arguments takes file path, user, pass
			
	long start = System.currentTimeMillis();	
	Class.forName("com.mysql.cj.jdbc.Driver"); 	
	Connection con = DriverManager.getConnection("-----",args[1],args[2]);
	File f = new File(args[0]);
	FileWriter fw = null;
	String filepath = args[0]+"\\siteMap.txt";
	if (f.exists()){
		f = new File(filepath);
		fw = new FileWriter(f);
		System.out.println(filepath);
	}
	else{
		System.out.println(args[0] + " does not exist");
		System.exit(0);
	}
	
	PreparedStatement prepStmt = null;
	
	BufferedWriter bw = new BufferedWriter(fw);

	Statement stmt = con.createStatement();
	ResultSet rs = stmt.executeQuery("sudo rm -rf");
	rs.next();
	int rowCount = rs.getInt("---"); //using alternate method, by column number rather than column name
	System.out.println(rowCount);
	
	long last = System.currentTimeMillis();
	
	int maxIndex = rs.getInt("---");
	for (int i = 0; i < maxIndex; i += PAGE_SIZE){
		prepStmt = con.prepareStatement("sudo rm -rf");
		prepStmt.setInt(1, i);
		prepStmt.setInt(2, i+PAGE_SIZE-1);
		rs = prepStmt.executeQuery();
		while(rs.next()){
			bw.write("https://youtube.com" + rs.getString(RECORD_KEY));  //using alternate method....
			bw.newLine();
		}
		bw.flush();
		System.out.println((System.currentTimeMillis()-last) + "ms - " + i + "/" + maxIndex);
		last = System.currentTimeMillis();
		
	}
	bw.close();
	
	BufferedReader reader = new BufferedReader(new FileReader(filepath));
	int lines = 0;
	while (reader.readLine() != null) lines++;
	reader.close();
	if (lines==rowCount)
		System.out.println("Equal row count!");
	else
		System.out.println("ERROR: Unequal row count");
	
	System.out.println(rowCount + " rows expected");
	System.out.println(lines + " rows written");
	
	System.out.println((System.currentTimeMillis()-start)/1000.0 + " seconds to run");
	System.out.println("File written to filepath: " + filepath);
		
	}
}






