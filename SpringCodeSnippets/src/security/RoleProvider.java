package security;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import com.custom.RoleProvider;

public class SpringRoleProvider implements RoleProvider {

	Logger log = Logger.getLogger(SpringRoleProvider.class);

	@Override
	public ArrayList<String> getApplicationRoles(String userId) throws Exception {

		DataSource db = null;
		Connection conn = null;
		 PreparedStatement stat = null;
		ArrayList<String> grps = new ArrayList<String>();
		try {
			String pass = InitialContext.doLookup("java:comp/env/login/MyDatabaseLogin");
			db = (DataSource) InitialContext.doLookup("java:/jdbc/MyDatabasePasswordFromServerXml");
			conn = db.getConnection("MyDatabaseLogin", pass);
			stat = conn.prepareStatement(ROLE_LOOKUP);
			//userId is passed from the server cookie, getting the id of the currently logged in user
			stat.setString(1, userId);
			ResultSet results = stat.executeQuery();
			Boolean isAuthorized = false;
			while (results.next()) {
				//Possible User Access Roles include ACCESS_LEVEL_READ and ACCESS_LEVEL_WRITE
				grps.add(results.getString("UserAccessRole"));
			}
		}
		catch (Exception e) {
			log.fatal("DB Lookup Failed for RefTables Query :" + e.toString(), e);
		}
		finally {
			try {
				stat.close();
				conn.close();
			}
			catch(Exception e) {}
		}
		return grps;
	}

	static String ROLE_LOOKUP = 
			"SELECT UserAccessRole FROM MyDatabase.myschema.MyRoleTable WHERE PersonId = ? AND Active = 1";



}

