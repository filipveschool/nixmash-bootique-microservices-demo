package com.nixmash.jangles.utils;

import com.nixmash.jangles.core.JanglesConfiguration;
import com.nixmash.jangles.core.JanglesConnections;
import com.nixmash.jangles.db.cn.JanglesConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.tomcat.jdbc.pool.DataSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class JanglesUtils {

	public static String pluralize(String singular) {
		String plural = singular;
		int singularLength = StringUtils.length(singular);
		if (StringUtils.right(singular, 1) == "y")
			plural = StringUtils.left(singular, singularLength - 1) + "ies";
		else
			plural = singular + "s";
		return plural;
	}

	public static String lowerPluralize(String singular) {
		return StringUtils.uncapitalize(pluralize(singular));
	}

	public static boolean isInTestingMode() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		List<StackTraceElement> list = Arrays.asList(stackTrace);
		for (StackTraceElement element : list) {
			if (element.getClassName().startsWith("org.junit.")) {
				return true;
			}
		}
		return false;
	}

	public static void configureTestDb(String sql) throws IOException, SQLException {

		JanglesConfiguration janglesConfiguration = new JanglesConfiguration();
		String sqlScript = janglesConfiguration.globalPropertiesPath + sql;
		JanglesConnection janglesConnection = getTestConnection();
		String url = janglesConnection.getUrl();
		String dbuser = janglesConnection.getUsername();
		String dbpassword = janglesConnection.getPassword();
		DataSource ds = new DataSource();
		ds.setDriverClassName(janglesConnection.getDriver());
		ds.setUrl(janglesConnection.getUrl());
		ds.setUsername(janglesConnection.getUsername());
		ds.setPassword(janglesConnection.getPassword());
		Connection conn = ds.getConnection();
		Statement st = conn.createStatement();
		File script = new File(sqlScript);
		ScriptRunner sr = new ScriptRunner(conn);
		sr.setLogWriter(null);
		Reader reader = new BufferedReader(new FileReader(script));
		sr.runScript(reader);
		reader.close();
		st.close();
		ds.close();
		conn.close();
	}


	@SuppressWarnings({"Duplicates", "ConstantConditions"})
	private static JanglesConnection getTestConnection() {
		JanglesConfiguration janglesConfiguration = new JanglesConfiguration();
		JanglesConnections janglesConnections = null;

			try {
				JAXBContext jc = JAXBContext.newInstance(JanglesConnections.class);
				Unmarshaller unmarshaller = jc.createUnmarshaller();
				File xml = new File(janglesConfiguration.connectionXmlPath);
				janglesConnections = (JanglesConnections) unmarshaller.unmarshal(xml);
			} catch (JAXBException e) {
				e.printStackTrace();
			}

			return janglesConnections.getConnections().stream()
					.filter(s -> s
							.getName()
							.equalsIgnoreCase(janglesConfiguration.testDbConnectionName))
					.findFirst()
					.get();

	}

	public static void showAllProperties() {
		Map<String, String> env = System.getenv();
		for (String envName : env.keySet()) {
			System.out.format("%s=%s%n", envName, env.get(envName));
		}

		Properties systemProperties = System.getProperties();
		Enumeration<?> enuProp = systemProperties.propertyNames();
		while (enuProp.hasMoreElements()) {
			String propertyName = (String) enuProp.nextElement();
			String propertyValue = systemProperties.getProperty(propertyName);
			System.out.println(propertyName + ": " + propertyValue);
		}
	}

}
