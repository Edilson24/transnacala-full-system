# TransNacala Web Admin

Java server-rendered administration panel for managing accounts, routes,
stops, route order, vehicles and assignments through the TransNacala REST API.
The browser never receives an API access token and this project never connects
directly to MySQL.

## Requirements and local run

- JDK 11 or newer
- Maven 3.8+
- JDK 11 or newer (Tomcat 10.1 requires Java 11+)
- Apache Tomcat 10.1.x (tested target: 10.1.57)
- A running TransNacala API with an administrator account

From this directory, build the WAR:

```powershell
mvn clean package
```

This application uses Jakarta Servlet 6.0 / JSP 3.1 and is not compatible with
Tomcat 9 or a `javax.servlet` runtime. Deploy the generated file
`target/transnacala-web-admin.war` into Tomcat 10.1's `webapps` directory, start
Tomcat, then open `http://localhost:8080/transnacala-web-admin/`. The WAR name
sets the context path; the application root redirects to the administrator
login. Confirm Tomcat reports deployment of `/transnacala-web-admin` in
`logs/catalina.*.log` before opening the page. If the WAR is renamed, the URL
context changes to match the new name.

Configure the API endpoint for the Tomcat process before starting it:

```powershell
$env:TRANSNACALA_API_URL = "http://127.0.0.1:8000"
```

When Tomcat and the API run on the same computer, the local address above is
enough; LocalTunnel is not required. Use LocalTunnel or another HTTPS tunnel
only when Tomcat must reach an API running on a different computer/network, or
when exposing the development API externally.

For IntelliJ's SmartTomcat run configuration, reload the Maven project after
changing `pom.xml`, then stop and start the SmartTomcat configuration again.
SmartTomcat creates its own context descriptor and dependency list; an existing
run can retain the previous `javax.servlet`/JSTL 1.2 libraries even after the
WAR has been rebuilt.

## Troubleshooting API login

- If the login form says the API cannot be contacted, start FastAPI and confirm
  `http://127.0.0.1:8000/health/live` responds. Set `TRANSNACALA_API_URL` in
  Tomcat's run configuration if the API uses another address; restart Tomcat
  after changing the variable.
- Uvicorn warnings `Unsupported upgrade request` and `Invalid HTTP request
  received` during login indicate an HTTP/2 upgrade attempt. The admin client
  explicitly uses HTTP/1.1 for Uvicorn compatibility; rebuild the WAR and
  restart/redeploy Tomcat after updating it.
- If it reports that the database is not configured or unavailable, configure
  `MYSQL_USER` and `MYSQL_PASSWORD` (or `DATABASE_URL`) in the API process's
  `.env`, ensure the MySQL service is running, and restart the API. Check
  `http://127.0.0.1:8000/health/ready` before retrying login.
- The web admin expects `TRANSNACALA_API_URL` to be the API base address, such
  as `http://127.0.0.1:8000`, not a Tomcat URL and not an address ending in
  `/api/v1`.

If Tomcat responds with 404 for the context, verify that
`webapps/transnacala-web-admin.war` exists, that the application started
successfully, and that the browser URL matches its WAR filename. A failed
deployment will have the cause in Tomcat's `logs/catalina.*.log`.

For remote environments, use the API's HTTPS URL and protect the admin site
with HTTPS as well. The API token is kept only in the server-side HTTP session;
the session cookie is HttpOnly, form submissions use CSRF tokens, and logout
invalidates the session. Do not expose the development API tunnel as a
production endpoint.

## Admin features

- Secure administrator login and session expiry.
- Separate overview, routes, team/fleet and stops pages; users and vehicles
  share the team page.
- Create routes from a modal, edit them, and remove them from the passenger
  catalogue by deactivating them.
- Add a stop from a route row; the API creates the stop and ordered route link
  in one transaction.
- Manage route stop membership and order from the route page.
- Create, view, edit, reset passwords and deactivate/reactivate user accounts.
- View route stop order and create/edit/deactivate/reactivate stops.
- Create vehicles with API-generated public codes and assign active drivers
  and routes.

Pagination in the UI requests the first 100 records from each API collection.
The API remains the authority for permissions, validation, unique codes and
catalogue synchronization.
