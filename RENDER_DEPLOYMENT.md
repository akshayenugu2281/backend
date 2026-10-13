# Deploying Library360 Backend on Render with PostgreSQL

This guide provides end-to-end instructions for deploying the **Library360 Spring Boot backend** on [Render](https://render.com) using a managed **PostgreSQL** database on Render.

---

## 🚀 Key Improvements & Updates Added

1. **PostgreSQL & Flyway 10 Support**:
   - Added `org.postgresql:postgresql` runtime JDBC driver.
   - Added `org.flywaydb:flyway-database-postgresql` for database migrations.
   - Converted all 8 database migrations (`V1` to `V8`) to PostgreSQL dialect (sequences, timestamp types, ANSI quotes).
   - Added sequence synchronization to ensure identity counters work after initial seeding.

2. **Automatic `DATABASE_URL` Parsing**:
   - Created `DatabaseUrlEnvironmentPostProcessor.java`.
   - When Render injects `DATABASE_URL` in the standard format (`postgres://user:pass@host:port/dbname`), the backend automatically converts it into a production JDBC PostgreSQL URL (`jdbc:postgresql://...`) with SSL configuration and credentials.

3. **Optimized Dockerfile for Render**:
   - Tuned JVM memory parameters (`-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xss512k -XX:+ExitOnOutOfMemoryError`) to ensure container stability within Render's 512 MB free tier RAM limits.
   - Automatically binds to Render's dynamic `$PORT` environment variable.

4. **Infrastructure as Code (`render.yaml`)**:
   - Added Render Blueprint file for 1-click deployment of both the database and the web service.

---

## 📋 Deployment Options

Choose either **Option 1 (Recommended: 1-Click Blueprint)** or **Option 2 (Manual Dashboard Setup)**.

---

### Option 1: 1-Click Blueprint Deployment (Recommended)

Render Blueprints read the [render.yaml](file:///render.yaml) file to automatically provision both the PostgreSQL database and the web service, wiring the connection string between them.

1. **Commit and Push to GitHub**:
   ```bash
   git add .
   git commit -m "Configure PostgreSQL and Render deployment"
   git push origin main
   ```

2. **Open Render Blueprints**:
   - Log in to [dashboard.render.com](https://dashboard.render.com/).
   - Click **New +** in the top right corner and select **Blueprint**.
   - Connect your GitHub repository containing `Library360`.

3. **Configure Blueprint Instance**:
   - Render will detect [render.yaml](file:///render.yaml).
   - Set a name (e.g. `library360-production`).
   - Supply the required seed passwords when prompted:
     - `ADMIN_PASSWORD`: A secure password with letters and numbers (at least 6 characters).
     - `LIBRARIAN_PASSWORD`: A secure password with letters and numbers (at least 6 characters).
   - Render will automatically generate `JWT_SECRET` and `APP_SETTINGS_ENCRYPTION_KEY`.
   - Click **Apply**.

4. **Deployment Process**:
   - Render creates the PostgreSQL database first.
   - Render then builds the Docker image and launches the web service.
   - Flyway migrations will run automatically on first boot, populating the schema and initial catalog.

---

### Option 2: Manual Dashboard Setup

If you prefer to configure services individually via the Render web dashboard:

#### Step 1: Create PostgreSQL Database on Render
1. Go to Render Dashboard -> **New +** -> **PostgreSQL**.
2. Set the following fields:
   - **Name**: `library360-db`
   - **Database**: `library360`
   - **User**: `library360`
   - **Region**: Choose the region closest to you (e.g., `Oregon (US West)` or `Frankfurt (EU)`).
   - **Plan**: `Free`
3. Click **Create Database**.
4. Once created, copy the **Internal Database URL** (e.g. `postgres://library360:xxxx@dpg-xxxxx-a:5432/library360`).
   *(Use Internal Database URL for Render services to avoid egress fees and achieve lowest latency).*

#### Step 2: Create Web Service for the Backend
1. Go to Render Dashboard -> **New +** -> **Web Service**.
2. Connect your Git repository.
3. Configure the service:
   - **Name**: `library360-api`
   - **Region**: Select the **same region** as your database.
   - **Root Directory**: `library360-api` (if in a subfolder) or leave blank if root.
   - **Environment / Runtime**: `Docker`
   - **Dockerfile Path**: `./Dockerfile`
   - **Plan**: `Free`
   - **Health Check Path**: `/actuator/health`

4. Add **Environment Variables**:

| Variable | Value / Description |
| :--- | :--- |
| `DATABASE_URL` | Paste your Render PostgreSQL **Internal Database URL** |
| `PORT` | `8081` |
| `CORS_ORIGINS` | Your frontend URL (e.g., `https://library360-library360upgraded.vercel.app,http://localhost:5173`) |
| `COOKIE_SECURE` | `true` |
| `COOKIE_SAME_SITE` | `None` |
| `JWT_SECRET` | 32+ byte random string (base64 encoded) |
| `APP_SETTINGS_ENCRYPTION_KEY` | Exactly 32 byte random string (base64 encoded) |
| `ADMIN_EMAIL` | `admin@library360.local` |
| `ADMIN_PASSWORD` | Strong password (e.g. `Admin123!Pass`) |
| `LIBRARIAN_EMAIL` | `librarian@library360.local` |
| `LIBRARIAN_PASSWORD` | Strong password (e.g. `Librarian123!Pass`) |
| `APP_AI_DEFAULT_MODEL` | `openai/gpt-4o-mini` |

5. Click **Create Web Service**.

---

## 🔍 Verification & Health Check

Once deployed, Render provides an URL: `https://library360-api-xxxx.onrender.com`.

1. **Verify Health Endpoint**:
   ```bash
   curl https://<YOUR_RENDER_APP_URL>/actuator/health
   ```
   Expected response:
   ```json
   {"status":"UP","components":{"db":{"status":"UP"}}}
   ```

2. **Verify Swagger UI**:
   Open in your browser:
   `https://<YOUR_RENDER_APP_URL>/swagger-ui/index.html`

3. **Connect Frontend (e.g., Vercel)**:
   In your frontend project settings, update the environment variable:
   ```env
   VITE_API_URL=https://<YOUR_RENDER_APP_URL>
   ```
   Ensure this frontend domain is listed in the backend's `CORS_ORIGINS` environment variable.
