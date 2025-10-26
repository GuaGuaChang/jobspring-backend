# Format: <pluginId>    <rule>    <action>
# Action options: IGNORE | WARN | FAIL

# Informational alerts — set to WARN to avoid false positives
10021    Content-Type missing    WARN
10035    Strict-Transport-Security Header Not Set    WARN
10054    X-Content-Type-Options Header Missing       WARN
10055    X-Frame-Options Header Not Set              WARN
10020    X-Content-Security-Policy                   WARN
10038    Content Security Policy (CSP) Header Not Set    WARN
10036    Cache-control and Pragma HTTP Header Missing    WARN
10016    Web Browser XSS Protection Not Enabled       WARN
10023    Information Disclosure - Debug Error Messages  WARN

# Authentication or authorization issues — keep as FAIL
40012    Cross Site Scripting (XSS)    FAIL
40018    SQL Injection    FAIL
40019    Server-Side Code Injection    FAIL
40020    Path Traversal    FAIL
40032    Cross Site Request Forgery (CSRF)    FAIL
40025    Remote OS Command Injection    FAIL