package srusakov;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

/**
 * Hello world!
 *
 */
public class App {
	public static Properties cfg;
	public static Properties env;
	public static String base;
	public static String char_filter;
	public static String phonebase;
	public static String charset;
	public static DirContext dirContext;

	private static void process(String filter) throws Exception {
		try {
			dirContext = new InitialLdapContext(env, null);
			SearchControls sc = new SearchControls();
			String[] attributeFilter = { "samAccountName", "displayName", "telephoneNumber" };
			sc.setReturningAttributes(attributeFilter);
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

			NamingEnumeration results = dirContext.search(base,
					"(&(objectCategory=person)(objectClass=user)(samAccountName=" + filter + "*))", sc);

			int i = 0;
			while (results.hasMore()) {
				i++;
				SearchResult sr = (SearchResult) results.next();

				// For Ascom DECT phone directory
				String name;
				try {
					name = sr.getAttributes().get("displayName").get().toString();
				} catch (NullPointerException ignored) {
					name = "";
				}
				String phone;
				try {
					phone = sr.getAttributes().get("telephoneNumber").get().toString();
				} catch (NullPointerException ignored) {
					phone = "";
				}
				String login = sr.getAttributes().get("samAccountName").get().toString();

				String name_split[] = name.split(" ");
				if (name_split.length < 2)
					continue;
				String lastname = name_split[0];
				String firstname = name_split[1];

				// login
				int pos = login.indexOf('.');
				login = login.toLowerCase();
				String login_firstname = "";
				String login_lastname = login;
				if (pos > 0) {
					login_firstname = login.substring(0, pos);
					login_lastname = login.substring(pos + 1);
				}

				// phone
				phone = phone.replace("-", "");
				phone = phone.replace(" ", "");
				if (phone.isEmpty()) {
					continue;
				}
				if (!phonebase.isEmpty()) {
					if ((pos = phone.indexOf(phonebase)) >= 0) {
						phone = phone.substring(pos + phonebase.length());
						// remove second phone
						if ((pos=phone.indexOf(',')) >= 0) {
							phone = phone.substring(0, pos);
						}
					} else {
						continue;
					}
				}

				// output
				String out = String.format("%s;%s;%s;", firstname, lastname, phone);
				if (!charset.isEmpty()) {
					new PrintStream(System.out, true, charset).println(new String(out.getBytes(charset), charset));
				} else {
					System.out.println(out);
				}
				// System.out.println(out);
				System.out.println(String.format("%s;%s;%s;", login_firstname, login_lastname, phone));

				// Just testing
				// NamingEnumeration<String> res = sr.getAttributes().getIDs();
				// while(res.hasMore()) {
				// String attrName = res.next();
				// String value = attrs.get(attrName).get().toString();
				// System.out.println(i + " " + attrName + ": " + value);
				//
				// }

			}
		} finally {
			dirContext.close();
		}
	}

	public static void main(String[] args) throws Exception {
		cfg = new Properties();
		cfg.load(new FileInputStream("./config.properties"));

		env = new Properties();
		env.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.setProperty(Context.PROVIDER_URL, cfg.getProperty("provider_url")); // "ldap://dc02.cardio.kem.ru:389");
		env.setProperty(Context.SECURITY_PRINCIPAL, cfg.getProperty("username")); // "daemon@cardio.kem.ru");
		env.setProperty(Context.SECURITY_CREDENTIALS, cfg.getProperty("password"));
		env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
		env.setProperty(Context.REFERRAL, "follow");
		base = cfg.getProperty("base");
		char_filter = cfg.getProperty("filter");
		char_filter = char_filter == null ? "" : char_filter;
		phonebase = cfg.getProperty("phonebase");
		phonebase = phonebase == null ? "" : phonebase;
		charset = cfg.getProperty("charset");
		charset = charset == null ? "" : charset;

		if (char_filter.isEmpty()) {
			String abc = "qwertyuiopasdfghjklzxcvbnm";
			for(int i=0; i<abc.length(); i++) {
				process(abc.substring(i, i+1));
			}
		} else {
			process(char_filter);
		}
	}
}
