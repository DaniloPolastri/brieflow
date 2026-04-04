# Technical Setup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Scaffold the BriefFlow monorepo with a working Spring Boot backend, Angular 20 frontend, and Docker Compose — ready to receive features.

**Architecture:** Monorepo with `backend/` (Spring Boot 3 + Java 21 + Maven) and `frontend/` (Angular 20 + Standalone + Zoneless + CSR). PostgreSQL via Docker Compose for dev. Nginx reverse proxy for prod.

**Tech Stack:** Java 21, Spring Boot 3, Maven, Angular 20, PrimeNG 19+ (Aura), Tailwind CSS v4, PostgreSQL 16, Docker Compose, Nginx

**Design Doc:** `docs/spec/2026-04-03-technical-setup-design.md`

---

### Task 1: Initialize Git Repo + .gitignore

**Files:**
- Create: `.gitignore`

**Step 1: Initialize git repo**

Run: `git init`

**Step 2: Create .gitignore**

```gitignore
# Java / Maven
backend/target/
*.class
*.jar
*.war
*.log
.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo

# Angular / Node
frontend/node_modules/
frontend/dist/
frontend/.angular/
frontend/.nx/

# Environment
.env
*.env.local

# Docker
postgres_data/

# OS
.DS_Store
Thumbs.db

# Uploads
uploads/
```

**Step 3: Commit**

```bash
git add .gitignore CLAUDE.md docs/
git commit -m "chore: initialize repo with gitignore, docs, and CLAUDE.md"
```

---

### Task 2: Docker Compose Dev (PostgreSQL)

**Files:**
- Create: `docker-compose.yml`

**Step 1: Create docker-compose.yml**

```yaml
services:
  postgres:
    image: postgres:16
    container_name: briefflow-postgres
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: briefflow
      POSTGRES_USER: briefflow
      POSTGRES_PASSWORD: briefflow
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U briefflow"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

**Step 2: Verify PostgreSQL starts**

Run: `docker-compose up -d`
Run: `docker-compose ps`
Expected: postgres container running, healthy

Run: `docker-compose down`

**Step 3: Commit**

```bash
git add docker-compose.yml
git commit -m "chore: add docker-compose for dev PostgreSQL"
```

---

### Task 3: Scaffold Spring Boot Backend

**Files:**
- Create: `backend/` (entire scaffold from Spring Initializr)

**Step 1: Generate project with Spring Initializr**

Use Spring Initializr (https://start.spring.io) or `curl` to generate:

```bash
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.4.4 \
  -d baseDir=backend \
  -d groupId=com.briefflow \
  -d artifactId=briefflow \
  -d name=briefflow \
  -d packageName=com.briefflow \
  -d javaVersion=21 \
  -d dependencies=web,security,data-jpa,postgresql,flyway,validation,mail,lombok \
  -o backend.zip
```

Unzip to project root so `backend/` contains `pom.xml`, `src/`, `.mvn/`, `mvnw`, etc.

**Step 2: Add MapStruct to pom.xml**

Add to `<dependencies>`:

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>
```

Add to `<build><plugins>` inside `maven-compiler-plugin` configuration:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.6.3</version>
            </path>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>0.2.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

Note: Lombok + MapStruct require `lombok-mapstruct-binding` to work together. The order in `annotationProcessorPaths` matters — MapStruct processor must be listed.

**Step 3: Verify it compiles**

Run: `cd backend && ./mvnw compile`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add backend/
git commit -m "chore: scaffold Spring Boot backend with Maven"
```

---

### Task 4: Backend Folder Structure

**Files:**
- Create: multiple `.gitkeep` files and `application*.yml`

**Step 1: Create package directories with .gitkeep**

Create all empty packages under `backend/src/main/java/com/briefflow/`:

```
config/.gitkeep
controller/.gitkeep
service/.gitkeep
service/impl/.gitkeep
repository/.gitkeep
entity/.gitkeep
dto/.gitkeep
dto/auth/.gitkeep
dto/workspace/.gitkeep
dto/member/.gitkeep
dto/client/.gitkeep
dto/job/.gitkeep
dto/kanban/.gitkeep
dto/approval/.gitkeep
dto/dashboard/.gitkeep
dto/common/.gitkeep
mapper/.gitkeep
enums/.gitkeep
exception/.gitkeep
security/.gitkeep
validation/.gitkeep
util/.gitkeep
```

Create resource directories:

```
backend/src/main/resources/db/migration/.gitkeep
backend/src/main/resources/templates/email/.gitkeep
```

**Step 2: Create application.yml**

Replace auto-generated `application.properties` with `backend/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: briefflow
  profiles:
    active: dev

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

  flyway:
    enabled: true
    baseline-on-migrate: true

  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

server:
  port: 8080

app:
  jwt:
    secret: ${JWT_SECRET:default-dev-secret-key-that-is-at-least-256-bits-long-for-hmac}
    access-expiration: 900000
    refresh-expiration: 604800000
  file:
    upload-dir: ./uploads
    max-size: 52428800
```

**Step 3: Create application-dev.yml**

File: `backend/src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/briefflow
    username: briefflow
    password: briefflow

  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
    hibernate:
      ddl-auto: update

  flyway:
    enabled: false

app:
  cors:
    allowed-origins: http://localhost:4200
```

Note: In dev profile, `ddl-auto: update` and `flyway.enabled: false` so Hibernate auto-creates tables during early development. Switch to `validate` + Flyway once migrations are written.

**Step 4: Create application-prod.yml**

File: `backend/src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}

  jpa:
    show-sql: false

app:
  cors:
    allowed-origins: ${CORS_ORIGINS}
```

**Step 5: Commit**

```bash
git add backend/
git commit -m "chore: add backend folder structure and config profiles"
```

---

### Task 5: Backend Exception Handling Skeleton

**Files:**
- Create: `backend/src/main/java/com/briefflow/exception/BusinessException.java`
- Create: `backend/src/main/java/com/briefflow/exception/ResourceNotFoundException.java`
- Create: `backend/src/main/java/com/briefflow/exception/UnauthorizedException.java`
- Create: `backend/src/main/java/com/briefflow/exception/ForbiddenException.java`
- Create: `backend/src/main/java/com/briefflow/exception/FileStorageException.java`
- Create: `backend/src/main/java/com/briefflow/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/briefflow/dto/common/ErrorResponseDTO.java`

**Step 1: Create ErrorResponseDTO**

```java
package com.briefflow.dto.common;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
    LocalDateTime timestamp,
    int status,
    String error,
    String message
) {}
```

**Step 2: Create custom exceptions**

`BusinessException.java`:
```java
package com.briefflow.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

`ResourceNotFoundException.java`:
```java
package com.briefflow.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

`UnauthorizedException.java`:
```java
package com.briefflow.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
```

`ForbiddenException.java`:
```java
package com.briefflow.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
```

`FileStorageException.java`:
```java
package com.briefflow.exception;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Step 3: Create GlobalExceptionHandler**

```java
package com.briefflow.exception;

import com.briefflow.dto.common.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusiness(BusinessException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Business Error",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorized(UnauthorizedException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponseDTO> handleForbidden(ForbiddenException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponseDTO> handleFileStorage(FileStorageException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "File Storage Error",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation error");

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                message
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

**Step 4: Verify it compiles**

Run: `cd backend && ./mvnw compile`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add backend/src/main/java/com/briefflow/exception/ backend/src/main/java/com/briefflow/dto/common/
git commit -m "chore: add global exception handler and custom exceptions"
```

---

### Task 6: Backend Security Skeleton

**Files:**
- Create: `backend/src/main/java/com/briefflow/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/briefflow/config/CorsConfig.java`

**Step 1: Create SecurityConfig**

```java
package com.briefflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/approval/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Step 2: Create CorsConfig**

```java
package com.briefflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

**Step 3: Update SecurityConfig to use CORS**

Add `.cors(cors -> cors.configurationSource(corsConfigurationSource))` to the filter chain. Inject `CorsConfigurationSource`:

```java
package com.briefflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/approval/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Step 4: Verify it compiles**

Run: `cd backend && ./mvnw compile`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add backend/src/main/java/com/briefflow/config/
git commit -m "chore: add Security and CORS skeleton config"
```

---

### Task 7: Verify Backend Starts

**Step 1: Start PostgreSQL**

Run: `docker-compose up -d`
Wait for healthy status.

**Step 2: Start backend**

Run: `cd backend && ./mvnw spring-boot:run`
Expected: Application starts on port 8080 without errors.

Note: With `ddl-auto: update` and Flyway disabled in dev, it should start even with no entities/migrations yet.

**Step 3: Test a request**

Run: `curl -s http://localhost:8080/api/v1/auth/test`
Expected: 404 (no controller yet, but the app is running — not 500 or connection refused)

Stop the backend with Ctrl+C.

**Step 4: No commit needed** (verification only)

---

### Task 8: Scaffold Angular Frontend

**Files:**
- Create: `frontend/` (entire scaffold from Angular CLI)

**Step 1: Check Angular CLI is installed**

Run: `ng version`
If not installed: `npm install -g @angular/cli`

Verify Angular CLI version supports Angular 20. If `ng version` shows Angular CLI < 20, update: `npm install -g @angular/cli@latest`

**Step 2: Generate Angular project**

Run from project root:

```bash
ng new frontend --zoneless --style=css --skip-git
```

Flags:
- `--zoneless` — signals-based change detection, no zone.js
- `--style=css` — Tailwind handles styling
- `--skip-git` — we already have a git repo at root

Note: `--standalone` is the default in Angular 20. `--ssr` is off by default.

**Step 3: Verify it runs**

Run: `cd frontend && ng serve`
Expected: App compiles and runs on http://localhost:4200

Stop with Ctrl+C.

**Step 4: Commit**

```bash
git add frontend/
git commit -m "chore: scaffold Angular 20 frontend (zoneless, standalone, CSR)"
```

---

### Task 9: Install Tailwind CSS v4

**Files:**
- Modify: `frontend/package.json` (via npm install)
- Create: `frontend/.postcssrc.json`
- Modify: `frontend/src/styles.css`

**Step 1: Install Tailwind CSS v4**

Run:

```bash
cd frontend && npm install tailwindcss @tailwindcss/postcss postcss --force
```

Note: `--force` may be needed for peer dependency conflicts with Angular 20.

**Step 2: Create PostCSS config**

File: `frontend/.postcssrc.json`

```json
{
  "plugins": {
    "@tailwindcss/postcss": {}
  }
}
```

**Step 3: Configure styles.css**

Replace contents of `frontend/src/styles.css` with:

```css
@import "tailwindcss";
```

**Step 4: Verify Tailwind works**

Add a Tailwind class to `frontend/src/app/app.component.html` (replace default content):

```html
<div class="flex items-center justify-center min-h-screen bg-gray-100">
  <h1 class="text-3xl font-bold text-indigo-500">BriefFlow</h1>
</div>
```

Run: `cd frontend && ng serve`
Expected: Page shows "BriefFlow" in indigo, centered, gray background.

Stop with Ctrl+C.

**Step 5: Commit**

```bash
git add frontend/
git commit -m "chore: configure Tailwind CSS v4"
```

---

### Task 10: Install and Configure PrimeNG

**Files:**
- Modify: `frontend/package.json` (via npm install)
- Modify: `frontend/src/styles.css`
- Modify: `frontend/src/app/app.config.ts`

**Step 1: Install PrimeNG**

Run:

```bash
cd frontend && npm install primeng @primeng/themes primeicons
```

**Step 2: Update styles.css**

File: `frontend/src/styles.css`

```css
@import "tailwindcss";
@import "primeicons/primeicons.css";

@layer reset, primeng;
```

**Step 3: Update app.config.ts**

Add PrimeNG provider. The file should look like:

```typescript
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: false
        }
      }
    })
  ]
};
```

Note: `provideZoneChangeDetection` may or may not be present depending on Angular 20 zoneless scaffold. If the scaffold already omits it (since `--zoneless`), don't add it back. If it's present with `{eventCoalescing: true}`, leave it.

Note: `darkModeSelector: false` disables dark mode (design spec says light mode only).

**Step 4: Verify PrimeNG works**

Update `frontend/src/app/app.component.ts` to test a PrimeNG component:

```typescript
import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-root',
  imports: [ButtonModule],
  template: `
    <div class="flex items-center justify-center min-h-screen bg-gray-100 gap-4">
      <h1 class="text-3xl font-bold text-indigo-500">BriefFlow</h1>
      <p-button label="Test PrimeNG" />
    </div>
  `
})
export class AppComponent {}
```

Run: `cd frontend && ng serve`
Expected: Page shows "BriefFlow" title + a styled PrimeNG button.

Stop with Ctrl+C.

**Step 5: Commit**

```bash
git add frontend/
git commit -m "chore: configure PrimeNG 19 with Aura theme"
```

---

### Task 11: Frontend Folder Structure + Environments

**Files:**
- Create: multiple `.gitkeep` files
- Create: `frontend/src/environments/environment.ts`
- Create: `frontend/src/environments/environment.prod.ts`

**Step 1: Create folder structure with .gitkeep**

Under `frontend/src/`:

```
core/services/.gitkeep
core/guards/.gitkeep
core/interceptors/.gitkeep
core/models/.gitkeep
shared/components/.gitkeep
shared/directives/.gitkeep
shared/pipes/.gitkeep
shared/utils/.gitkeep
features/auth/.gitkeep
features/clients/.gitkeep
features/jobs/.gitkeep
features/kanban/.gitkeep
features/dashboard/.gitkeep
features/members/.gitkeep
features/approval/.gitkeep
features/settings/.gitkeep
layout/.gitkeep
assets/images/.gitkeep
assets/icons/.gitkeep
```

**Step 2: Create environment files**

File: `frontend/src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

File: `frontend/src/environments/environment.prod.ts`

```typescript
export const environment = {
  production: true,
  apiUrl: ''
};
```

**Step 3: Commit**

```bash
git add frontend/src/
git commit -m "chore: add frontend folder structure and environment configs"
```

---

### Task 12: Frontend App Routes Skeleton

**Files:**
- Modify: `frontend/src/app/app.routes.ts`
- Modify: `frontend/src/app/app.component.ts`

**Step 1: Create route skeleton**

File: `frontend/src/app/app.routes.ts`

```typescript
import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
```

Note: Lazy-loaded feature routes (auth, kanban, jobs, etc.) will be added as each feature is implemented. This is just the skeleton.

**Step 2: Reset app.component.ts to minimal root**

```typescript
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `<router-outlet />`
})
export class AppComponent {}
```

**Step 3: Verify it compiles**

Run: `cd frontend && ng serve`
Expected: App starts, redirects to /dashboard (blank page since no dashboard component yet — no errors in console).

Stop with Ctrl+C.

**Step 4: Commit**

```bash
git add frontend/src/app/
git commit -m "chore: add app routes skeleton and minimal root component"
```

---

### Task 13: Docker Compose Prod + Dockerfiles + Nginx

**Files:**
- Create: `backend/Dockerfile`
- Create: `frontend/Dockerfile`
- Create: `nginx/nginx.conf`
- Create: `docker-compose.prod.yml`

**Step 1: Create backend Dockerfile**

File: `backend/Dockerfile`

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Step 2: Create frontend Dockerfile**

File: `frontend/Dockerfile`

```dockerfile
# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration=production

# Stage 2: Serve
FROM nginx:alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

Note: Angular 20 outputs to `dist/<project>/browser`. Adjust the path if different after the first build.

**Step 3: Create nginx.conf**

File: `nginx/nginx.conf`

```nginx
upstream backend {
    server backend:8080;
}

server {
    listen 80;
    server_name _;

    # Frontend static files
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # Backend API proxy
    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 50M;
    }
}
```

**Step 4: Create docker-compose.prod.yml**

```yaml
services:
  postgres:
    image: postgres:16
    container_name: briefflow-postgres-prod
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DATABASE_NAME:-briefflow}
      POSTGRES_USER: ${DATABASE_USER:-briefflow}
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD:-briefflow}
    volumes:
      - postgres_data_prod:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DATABASE_USER:-briefflow}"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build: ./backend
    container_name: briefflow-backend
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:postgresql://postgres:5432/${DATABASE_NAME:-briefflow}
      DATABASE_USER: ${DATABASE_USER:-briefflow}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD:-briefflow}
      JWT_SECRET: ${JWT_SECRET}
      CORS_ORIGINS: ${CORS_ORIGINS:-http://localhost}
    expose:
      - "8080"

  frontend:
    build: ./frontend
    container_name: briefflow-frontend
    restart: unless-stopped
    expose:
      - "80"

  nginx:
    image: nginx:alpine
    container_name: briefflow-nginx
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on:
      - backend
      - frontend

volumes:
  postgres_data_prod:
```

**Step 5: Commit**

```bash
git add backend/Dockerfile frontend/Dockerfile nginx/ docker-compose.prod.yml
git commit -m "chore: add production Docker Compose, Dockerfiles, and Nginx config"
```

---

### Task 14: README.md

**Files:**
- Create: `README.md`

**Step 1: Create README.md**

```markdown
# BriefFlow

Plataforma de gestao de producao criativa para agencias de marketing. Centraliza briefings, kanban de producao e aprovacao do cliente.

## Pre-requisitos

- Java 21+
- Node.js 22+
- Docker e Docker Compose
- Angular CLI (`npm install -g @angular/cli`)

## Desenvolvimento

### 1. Subir o banco de dados

```bash
docker-compose up -d
```

### 2. Rodar o backend

```bash
cd backend
./mvnw spring-boot:run
```

API disponivel em http://localhost:8080

### 3. Rodar o frontend

```bash
cd frontend
ng serve
```

App disponivel em http://localhost:4200

## Producao

```bash
docker-compose -f docker-compose.prod.yml up -d --build
```

App disponivel em http://localhost
```

**Step 2: Commit**

```bash
git add README.md
git commit -m "chore: add README with dev and prod instructions"
```

---

### Task 15: Final Verification

**Step 1: Start PostgreSQL**

Run: `docker-compose up -d`

**Step 2: Start backend**

Run: `cd backend && ./mvnw spring-boot:run`
Expected: Starts on :8080, no errors.

**Step 3: Start frontend (in another terminal)**

Run: `cd frontend && ng serve`
Expected: Compiles and serves on :4200, no errors.

**Step 4: Open browser**

Open http://localhost:4200
Expected: Blank page (router-outlet with no routes matched yet), no console errors.

**Step 5: Stop everything**

Stop backend (Ctrl+C), stop frontend (Ctrl+C), stop Docker (`docker-compose down`).

**Step 6: Final commit if any cleanup needed**

If no changes needed, no commit.

---

## Task Summary

| Task | Description |
|------|-------------|
| 1 | Initialize Git Repo + .gitignore |
| 2 | Docker Compose Dev (PostgreSQL) |
| 3 | Scaffold Spring Boot Backend |
| 4 | Backend Folder Structure + Config |
| 5 | Backend Exception Handling Skeleton |
| 6 | Backend Security Skeleton |
| 7 | Verify Backend Starts |
| 8 | Scaffold Angular Frontend |
| 9 | Install Tailwind CSS v4 |
| 10 | Install and Configure PrimeNG |
| 11 | Frontend Folder Structure + Environments |
| 12 | Frontend App Routes Skeleton |
| 13 | Docker Compose Prod + Dockerfiles + Nginx |
| 14 | README.md |
| 15 | Final Verification |
