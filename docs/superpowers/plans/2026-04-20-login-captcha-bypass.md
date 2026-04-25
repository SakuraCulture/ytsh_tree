# Login Captcha Bypass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an explicit frontend-only environment switch that bypasses the login slider captcha in test automation while preserving the normal login flow by default.

**Architecture:** Keep the change scoped to the login form. Introduce a Vite environment variable that the login submit entrypoint checks before deciding whether to open the captcha widget or submit directly. Update the relevant environment file so automation can opt in without affecting other environments.

**Tech Stack:** Vue 3, Vite env variables, TypeScript, Element Plus, pnpm, vue-tsc

---

## File Structure

- Modify: `ytsh-ui-vue3/src/views/login/components/LoginForm.vue`
  - Responsibility: decide whether clicking the login button triggers the captcha widget or directly calls the existing login handler.
- Modify: `ytsh-ui-vue3/.env.test`
  - Responsibility: enable the new captcha-bypass switch for the dedicated test build mode.
- Optional verify-only read: `ytsh-ui-vue3/.env.local`
  - Responsibility: confirm local dev keeps the default behavior and does not enable the bypass.

### Task 1: Add the explicit captcha bypass switch in the login form

**Files:**
- Modify: `ytsh-ui-vue3/src/views/login/components/LoginForm.vue`
- Test: `ytsh-ui-vue3/src/views/login/components/LoginForm.vue` (manual behavior verification via app runtime and type-check)

- [ ] **Step 1: Write the failing expectation as an executable checklist**

```md
Expected behavior before code change:
1. When VITE_APP_CAPTCHA_ENABLE === 'true', clicking the login button always calls verify.value.show().
2. There is no VITE_DISABLE_LOGIN_CAPTCHA branch in getCode().
3. In test automation mode we cannot bypass the slider captcha from the frontend.
```

- [ ] **Step 2: Confirm the current code lacks the bypass branch**

Run:
```bash
grep -n "VITE_DISABLE_LOGIN_CAPTCHA\|const getCode" "C:/Users/ytsh01/Desktop/ant_dev/ytsh-ui-vue3/src/views/login/components/LoginForm.vue"
```

Expected:
```text
The output shows const getCode, but no VITE_DISABLE_LOGIN_CAPTCHA match.
```

- [ ] **Step 3: Add the minimal implementation in the login form**

Update `ytsh-ui-vue3/src/views/login/components/LoginForm.vue` so the component reads the new env variable and `getCode()` checks it first.

```ts
const loginData = reactive({
  isShowPassword: false,
  captchaEnable: import.meta.env.VITE_APP_CAPTCHA_ENABLE,
  disableLoginCaptcha: import.meta.env.VITE_DISABLE_LOGIN_CAPTCHA === 'true',
  tenantEnable: import.meta.env.VITE_APP_TENANT_ENABLE,
  loginForm: {
    tenantName: import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '',
    username: import.meta.env.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
    password: import.meta.env.VITE_APP_DEFAULT_LOGIN_PASSWORD || '',
    captchaVerification: '',
    rememberMe: true
  }
})

const getCode = async () => {
  if (loginData.disableLoginCaptcha || loginData.captchaEnable === 'false') {
    await handleLogin({})
    return
  }

  verify.value.show()
}
```

Implementation notes:
- Do not change `handleLogin()` behavior.
- Do not remove the `Verify` component from the template.
- Keep the bypass explicit and local to the login page.

- [ ] **Step 4: Review the edited file for exact behavior**

Run:
```bash
grep -n "disableLoginCaptcha\|const getCode" "C:/Users/ytsh01/Desktop/ant_dev/ytsh-ui-vue3/src/views/login/components/LoginForm.vue"
```

Expected:
```text
The output includes disableLoginCaptcha in loginData and a getCode() branch that directly calls handleLogin({}) before verify.value.show().
```

- [ ] **Step 5: Commit the focused code change**

```bash
git add "ytsh-ui-vue3/src/views/login/components/LoginForm.vue"
git commit -m "feat: add login captcha bypass switch"
```

### Task 2: Enable the bypass in the dedicated test environment

**Files:**
- Modify: `ytsh-ui-vue3/.env.test`
- Test: `ytsh-ui-vue3/.env.test` (config verification by build-time env consumption)

- [ ] **Step 1: Write the failing expectation as an executable checklist**

```md
Expected behavior before config change:
1. .env.test does not define VITE_DISABLE_LOGIN_CAPTCHA.
2. The test build mode cannot enable the frontend captcha bypass explicitly.
```

- [ ] **Step 2: Confirm the test env file does not already define the switch**

Run:
```bash
grep -n "VITE_DISABLE_LOGIN_CAPTCHA" "C:/Users/ytsh01/Desktop/ant_dev/ytsh-ui-vue3/.env.test"
```

Expected:
```text
No output.
```

- [ ] **Step 3: Add the new env variable to the test mode file**

Append the explicit switch to `ytsh-ui-vue3/.env.test` near the other frontend runtime flags.

```env
# 登录滑块验证开关：仅测试环境用于自动化登录
VITE_DISABLE_LOGIN_CAPTCHA=true
```

Implementation notes:
- Do not change `VITE_APP_CAPTCHA_ENABLE` semantics.
- Keep the variable name identical to the one read in `LoginForm.vue`.

- [ ] **Step 4: Confirm the test env file now enables the bypass**

Run:
```bash
grep -n "VITE_DISABLE_LOGIN_CAPTCHA" "C:/Users/ytsh01/Desktop/ant_dev/ytsh-ui-vue3/.env.test"
```

Expected:
```text
A single line showing VITE_DISABLE_LOGIN_CAPTCHA=true.
```

- [ ] **Step 5: Commit the config change**

```bash
git add "ytsh-ui-vue3/.env.test"
git commit -m "chore: enable login captcha bypass in test mode"
```

### Task 3: Verify default behavior and test-mode behavior

**Files:**
- Verify: `ytsh-ui-vue3/src/views/login/components/LoginForm.vue`
- Verify: `ytsh-ui-vue3/.env.local`
- Verify: `ytsh-ui-vue3/.env.test`

- [ ] **Step 1: Confirm local development does not enable the bypass**

Run:
```bash
grep -n "VITE_DISABLE_LOGIN_CAPTCHA" "C:/Users/ytsh01/Desktop/ant_dev/ytsh-ui-vue3/.env.local"
```

Expected:
```text
No output, which means local development keeps the normal captcha behavior.
```

- [ ] **Step 2: Run type-check to catch template/script mistakes**

Run:
```bash
pnpm --dir "C:/Users/ytsh01/Desktop/ant_dev/ytsh-ui-vue3" ts:check
```

Expected:
```text
Type-check completes successfully with exit code 0.
```

- [ ] **Step 3: Start the frontend in test mode and manually verify the login page path**

Run:
```bash
pnpm --dir "C:/Users/ytsh01/Desktop/ant_dev/ytsh-ui-vue3" vite --mode test
```

Expected:
```text
Vite starts successfully and serves the frontend with .env.test loaded.
```

Manual verification checklist:
```md
1. Open the login page in the browser.
2. Enter valid username and password.
3. Click the login button.
4. Verify the slider captcha does not appear.
5. Verify the page sends the login request directly.
6. Verify successful login still stores the token and redirects.
7. Clear one required field and click login again.
8. Verify form validation still blocks submission.
```

- [ ] **Step 4: Stop the dev server and inspect the final diff**

Run:
```bash
git diff -- "C:/Users/ytsh01/Desktop/ant_dev/ytsh-ui-vue3/src/views/login/components/LoginForm.vue" "C:/Users/ytsh01/Desktop/ant_dev/ytsh-ui-vue3/.env.test"
```

Expected:
```text
The diff only shows the new env switch and the minimal login-branch change.
```

- [ ] **Step 5: Create the final verification commit**

```bash
git add "ytsh-ui-vue3/src/views/login/components/LoginForm.vue" "ytsh-ui-vue3/.env.test"
git commit -m "test: verify login captcha bypass flow"
```

## Self-Review

### Spec coverage
- Explicit frontend-only switch: covered in Task 1 and Task 2.
- Default behavior unchanged: covered in Task 3 Step 1 and the local branch design in Task 1.
- No backend protocol changes: enforced by Task 1 implementation notes.
- Automation can log in without solving slider: covered in Task 3 manual verification checklist.

### Placeholder scan
- No TODO/TBD placeholders remain.
- All code/config steps include exact snippets or exact commands.

### Type consistency
- The env variable name is consistently `VITE_DISABLE_LOGIN_CAPTCHA` across all tasks.
- The login entrypoint is consistently `getCode()` in `LoginForm.vue`.
