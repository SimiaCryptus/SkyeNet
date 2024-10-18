Based on the provided code, here's a document detailing the servlet path layout for the application:

# Servlet Path Layout

The application uses a hierarchical structure for its servlet paths, with different functionalities mapped to specific URLs. Below is a breakdown of the main servlet paths and
their purposes:

## Root Level Paths

1. `/` - Welcome page (WelcomeServlet)
2. `/api` - API endpoint (WelcomeServlet)
3. `/logout` - Logout functionality (LogoutServlet)
4. `/proxy` - Proxy HTTP requests (ProxyHttpServlet)
5. `/userInfo` - User information (UserInfoServlet)
6. `/userSettings` - User settings (UserSettingsServlet)
7. `/usage` - Usage information (UsageServlet)
8. `/apiKeys` - API key management (ApiKeyServlet)
9. `/stats` - Server statistics (StatisticsServlet)

## Authentication

* `/oauth2callback` - OAuth2 callback URL for Google authentication

## Application-Specific Paths

For each child web application defined in `childWebApps`, a new path is created. These paths are dynamically generated based on the `ApplicationServer` instances.

## Common Paths for Each Application

Within each application-specific path, the following common servlets are typically available:

1. `/` - Default servlet
2. `/ws` - WebSocket endpoint
3. `/newSession` - Create a new session
4. `/appInfo` - Application information
5. `/userInfo` - User information (may overlap with root level)
6. `/usage` - Usage information (may overlap with root level)
7. `/fileIndex/*` - File indexing
8. `/fileZip` - File compression
9. `/sessions` - Session listing
10. `/settings` - Session settings
11. `/threads` - Session threads
12. `/share` - Session sharing
13. `/delete` - Delete session
14. `/cancel` - Cancel threads

## Notes

* The exact paths may vary depending on the specific implementation of each `ApplicationServer` instance.
* Some paths (like `/userInfo` and `/usage`) appear both at the root level and within application-specific contexts. The application-specific versions may provide more tailored
  information.
* The application uses filters for Cross-Origin Resource Sharing (CORS) and authentication/authorization checks.
* WebSocket support is configured for real-time communication.
* The server is set up to handle both HTTP and HTTPS connections, with the latter being used when running in server mode.

This layout provides a comprehensive structure for handling various aspects of the application, from user management and authentication to application-specific functionalities and
session handling.
