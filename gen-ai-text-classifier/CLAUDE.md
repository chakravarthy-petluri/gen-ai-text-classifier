# Claude Code Configuration

## Project: GenAI Text Classifier Backend

A Spring Boot REST API for classifying text using multiple GenAI models (Google Gemini and OpenAI ChatGPT).

---

## Authorized Actions

### ✅ ALWAYS Allowed (No Approval Needed)
- Reading and exploring code files
- Searching the codebase (Glob, Grep)
- Viewing Git history and status
- Research and analysis tasks
- Running tests and builds

### ✅ Allowed With Explanation
- **Editing code files**: Fix bugs, improve code quality, implement features
- **Writing new files**: Only when necessary for the task
- **Creating commits**: When explicitly requested or after code changes
- **Running Bash commands**: For building, testing, development tasks

### ⚠️ Requires Explicit Approval
- **Git push/pull**: Especially to main/master branch
- **Deleting files or directories**: Destructive operations
- **Modifying CI/CD pipelines**: Infrastructure changes
- **Installing new dependencies**: May affect project compatibility

### ❌ NOT Allowed
- Force-pushing code
- Skipping git hooks
- Committing without permission
- Making changes to `.env` file (credentials)
- Modifying project structure without discussion

---

## Development Guidelines

### Code Style
- **Language**: Java 17
- **Framework**: Spring Boot 3.4.2
- **Build Tool**: Maven
- **Code Quality**: Use Lombok for boilerplate reduction
- **Logging**: Use SLF4J with @Slf4j annotation (avoid System.out/err)

### Commit Messages
- **Format**: Clear, descriptive commit messages
- **Include**: Co-Authored-By footer for Claude contributions
- **Example**:
  ```
  Fix NullPointerException in response parsing

  Changed Collections.emptyList() to ArrayList to allow mutation.
  This fixes the bug where classifications couldn't be added to results.

  Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
  ```

### Code Organization
```
src/main/java/com/petluri/gen_ai_text_classifier/
├── config/              # Spring configuration classes
├── controller/          # REST endpoints
├── model/               # Data models (DTOs)
├── service/             # Business logic interfaces
│   └── impl/            # Service implementations
└── util/                # Utility classes
```

### Best Practices
- Keep API keys and secrets in `.env`, never commit to repo
- Add new GenAI models in `GenAITypeSelector`
- Handle all external API calls with try-catch
- Return appropriate HTTP status codes
- Maintain backward compatibility in API endpoints

---

## Common Workflows

### Bug Fixes
1. Identify the issue with tests or logs
2. Fix the code
3. Run tests to verify
4. Commit with explanation
5. No push required unless requested

### Adding Features
1. Discuss the approach if unclear
2. Implement the feature
3. Add error handling
4. Test thoroughly
5. Commit when done

### Code Cleanup
1. Identify unnecessary code/configs
2. Make cleanup changes
3. Verify nothing breaks
4. Commit with "Clean up" message
5. Can push to master if changes are non-breaking

### Merging Branches
1. Always fetch first: `git fetch`
2. Switch to target branch
3. Merge source branch
4. Resolve conflicts if any
5. Push only with explicit approval

---

## Project Structure Notes

### Key Files
- **pom.xml**: Maven dependencies and build configuration
- **compose.yaml**: Docker Compose for MySQL database
- **.env**: Environment variables (DO NOT COMMIT)
- **application.properties**: Spring Boot configuration
- **HELP.md**: Spring Boot generated help (can be ignored)

### Important Classes
- `GenAiTextClassifierApplication`: Main entry point with logging
- `ClassifiersController`: REST endpoint for classification
- `GenAITypeSelector`: Routes requests to correct service implementation
- `ClassificationServiceUsingGemini/ChatGPT`: API integrations
- `GeminiResponseParser`: Parses Gemini API responses

---

## API Specifications

### Endpoint
```
POST /api/classifiers/classify
```

### Request Body
```json
{
  "genAIType": "gemini" | "chatgpt",
  "genAIModel": "gemini-1.5-flash" | "gpt-4" | etc,
  "genAIAPIKey": "your-api-key-here",
  "textToClassifyList": ["text1", "text2", ...],
  "attributeList": ["attr1", "attr2", ...]
}
```

### Response
```json
[
  {
    "textToClassify": "text1",
    "attribute": "attr1"
  },
  ...
]
```

### Error Handling
- **400 Bad Request**: Invalid model name
- **500 Internal Server Error**: API call failure or parsing error

---

## Testing & Building

### Build Project
```bash
mvn clean install
```

### Run Tests
```bash
mvn test
```

### Run Application
```bash
# Start MySQL
docker-compose up

# In another terminal
mvn spring-boot:run
```

### Access Application
- API: `http://localhost:8080/api/classifiers/classify`
- Swagger Docs: `http://localhost:8080/swagger-ui/index.html` (if enabled)

---

## Dependencies

### Core
- spring-boot-starter-web: REST API support
- spring-boot-starter-test: Testing framework
- lombok: Reduce boilerplate code
- org.json: JSON parsing
- jackson-databind: JSON serialization

### Database (Optional)
- MySQL 8.0: Via Docker Compose

---

## CORS Configuration

**Current Settings** (see `CorsConfig.java`):
- **Allowed Origins**: `*` (all origins)
- **Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS
- **Allowed Headers**: All

⚠️ **Production Note**: Change allowed origins to specific domain for security

---

## Questions for Implementation

When starting a task, Claude should clarify:
1. **Unclear requirements**: Ask user for specifics
2. **Multiple approaches**: Present options with trade-offs
3. **Architecture decisions**: Discuss before implementing
4. **Breaking changes**: Confirm impact before proceeding

---

## Permission Summary

| Action | Permission | Notes |
|--------|-----------|-------|
| Read files | ✅ Auto | Always allowed |
| Edit code | ✅ Auto | Bug fixes and features |
| Create commits | ✅ Auto | After code changes |
| Push to master | ⚠️ Ask | Needs approval for production |
| Delete files | ⚠️ Ask | Destructive operation |
| Add dependencies | ⚠️ Ask | May affect compatibility |
| Force push | ❌ No | Never without explicit request |
| Commit secrets | ❌ No | Use .env instead |

---

## Last Updated
March 1, 2026

## Next Steps
- Set up CI/CD pipeline (GitHub Actions)
- Add Swagger/Springdoc documentation
- Implement response caching
- Add rate limiting
- Support additional GenAI models (Claude, Llama, etc.)