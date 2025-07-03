# TUWEL Grade Monitor 📊

This Java program monitors your TUWEL courses and **notifies you by email** when any **grades change**.

It works by:
1. Logging in to TUWEL using the SAML2 login flow (like your browser does).
2. Visiting the **"Grades" tab** for each configured course.
3. Extracting the HTML content of the `<div class="user-report-container">` — this is the section TUWEL uses to show your grades.
4. Comparing the current version of that section to the previous one saved on disk.
5. If a change is detected, a **color-coded email** is sent to you showing the differences.

---

## ✉️ Email Notification

When a grade changes, you’ll receive an email that includes:

- A styled HTML page (using TUWEL's official CSS)
- All removed content marked in **red and strikethrough**
- All added content marked in **green**

### Example

If you had a grade change like this:

```diff
- <td>Homework 1</td><td>80%</td>
+ <td>Homework 1</td><td>95%</td>
```

You’ll get an email like:

```html
<span style="color:red;text-decoration:line-through;">Homework 1 80%</span>
<span style="color:green;">Homework 1 95%</span>
```

---

## 📧 Gmail Setup: App Password Required

To send email notifications through Gmail, you must use an **App Password** (not your normal Google password).

### ✅ How to get a Gmail App Password

1. Enable **2-Step Verification** on your Google account:  
   https://myaccount.google.com/security

2. Go to:  
   https://myaccount.google.com/apppasswords

3. Type something like `Tuwel Monitor`

4. Google will generate a **16-character password** like:  
   `abcd efgh ijkl mnop`

5. Copy and paste that into the `Constants.java` file (see below).

---

## ⚙️ Configuration – `Constants.java`

This file contains all key settings:

```java
package com.petkap.tuwelscraper;

public class Constants {

    // TUWEL Login Credentials
    public static final String TUWEL_USERNAME = "MATR.NR";                      // Your TU Wien username
    public static final String TUWEL_PASSWORD = "YOUR_TUWEL_PASSWORD";          // Your TUWEL password

    // Gmail Email Sending
    public static final String GMAIL_USERNAME = "YOUR_EMAIL@gmail.com";         // Sender Gmail address
    public static final String GMAIL_APP_PASSWORD = "GENERATED_APP_PASSWORD";   // App password (not Gmail password)

    // Courses to Monitor
    public static final int[] COURSE_IDS = new int[]{
        12345, 23456, 34567
    };
}
```

### Explanation:

- **TUWEL_USERNAME** and **TUWEL_PASSWORD**: used to log in through TU Wien’s SAML2 authentication system.
- **GMAIL_USERNAME** and **GMAIL_APP_PASSWORD**: used to send the email from your account.
- **COURSE_IDS**: numeric IDs of your TUWEL courses.

---

## 📘 About Course IDs

Each TUWEL course has a unique ID, visible in the course URL:

```
https://tuwel.tuwien.ac.at/course/view.php?id=72263
```

→ Here, `72263` is the **Course ID**.

### Important:
Courses must have a **Grades** tab (`/grade/report/user/index.php?id=...`) for the monitor to work. If a course doesn’t use TUWEL’s grading feature, the program won’t find the `<div class="user-report-container">`, and will print a warning.

---

## ✅ Running the App

1. Package with Maven:
   ```bash
   mvn clean package
   ```

2. Run the app:
   ```bash
   java -jar target/TuwelScraper-1.0-SNAPSHOT.jar
   ```

3. The app will:
   - Log in once
   - Monitor all courses every minute
   - Send an email when any course’s grade content changes

---

## 🛡️ Security Note

Your passwords are stored in plaintext for simplicity. For production use, consider moving them to a secure `.env` file or encrypted config store.
