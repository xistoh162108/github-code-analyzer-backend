# 🧪 API Testing Flow & GitHub Verification

Follow this workflow to verify all features from authentication to AI analysis.

## 📖 1. Accessing Swagger UI
Once the server is running, open:
🔗 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

### How to test Authenticated Endpoints:
1. Log in via the web frontend (which will handle the OAuth2 flow).
2. Grab the `accessToken` from the browser (Local Storage/Cookie).
3. In Swagger UI, click the **Authorize** button (top-right).
4. Enter `Bearer <your_token>` and click Authorize.

---

## 🔄 2. Core API Testing Workflow

### Step A: Identity & Search
1. **Search**: `GET /api/search?q=test`
   - Verify it returns results from Users, Repos, Teams, and Sprints.
2. **Profile**: `GET /api/users/me`
   - Verify your own GitHub-linked profile data.

### Step B: Repository Integration
1. **Register/Sync**: `POST /api/repos/{repoId}/sync`
   - Use a GitHub `repoId` or URL.
   - Verify it triggers a background synchronization.
2. **Metrics**: `GET /api/repos/{repoId}/metrics`
   - Check if the project health score is calculated.

### Step C: AI Analysis
1. **Recent Commits**: `GET /api/repos/{repoId}/commits`
   - Wait for sync to finish.
2. **Analyze**: `GET /api/repos/{repoId}/commits/{sha}/analysis`
   - Check if GPT-based analysis summary and scores are returned.

---

## 🐙 3. GitHub Integration Checkpoints

### 1. Webhooks
- **Tool**: [smee.io](https://smee.io/) (for local testing) or GitHub App Settings.
- **Events to test**: `push`, `pull_request`, `installation`.
- **Verification**: Check server logs for "Webhook received" messages.

### 2. GitHub App Permissions
Ensure your app has the following **Repository Permissions**:
- `Metadata`: Read-only
- `Contents`: Read-only
- `Issues`: Read/Write (if using automated reporting)
- `Pull Requests`: Read-only

### 3. Installation Flow
- Install the App on a specific repository in GitHub.
- Verify the `WebhookController` handles the installation event and saves the mapping.

---

## 📝 4. Manual Verification List
- [ ] **OAuth2 Login**: Success redirect to frontend.
- [ ] **Token Refresh**: Access token expires -> Refresh token used -> New access token.
- [ ] **Data Persistence**: Sync data remains in MySQL after restart.
- [ ] **Cache Performance**: Repeated requests to metrics/heatmap should be fast (Redis).
