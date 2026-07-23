# Angular Frontend Workspace

This folder is reserved for the Angular modular application source.

Expected flow:
1. Build Angular app from this folder.
2. Publish build output into ../src/main/resources/static.
3. Run Spring Boot module to serve the UI at http://localhost:8080.

## Angular Hot Reload (Recommended for UI Development)

Run backend API host:
1. From module root, start Spring Boot on port 8080.

Run Angular dev server with live reload:
1. cd frontend
2. npm run start:hot
3. Open http://localhost:4200

Notes:
- API calls to /api/* are proxied to http://localhost:8080 via proxy.conf.json.
- Class/template/style changes reload automatically in the browser.
- For HMR mode, use npm run start:hmr.

## Static Build Watch Mode (Alternative)

If you want Spring Boot static hosting while editing frontend files:
1. cd frontend
2. npm run build:watch

This continuously rebuilds into ../src/main/resources/static.
