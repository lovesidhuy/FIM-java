A Java-Based File Integrity Monitoring Tool
monitors a specific directory and detects file changes (creation, modification, deletion) in real-time. It tracks changes using SHA-256 hashing and stores known-good values in a persistent `hashes.json` file. Logs are also sent to the system's log service (syslog) and printed to the console.

![image](https://github.com/user-attachments/assets/f1ba0a1f-3aea-4650-9d30-bf84e496628b)


When you start the tool, it follows this route:

![image](https://github.com/user-attachments/assets/857c3048-50a5-42a8-bfa3-ea58d02d19e0)


Each change is logged to:
   - Console output
   - System log via `logger` (can be viewed in Console app or `/var/log/syslog`)

<img width="468" alt="image" src="https://github.com/user-attachments/assets/ecae74c6-3412-40a1-b47c-b712249a48eb" />

