# Recreation of [LogcatReader by darshanparajuli](https://github.com/darshanparajuli/LogcatReader).


## CPC200-Log-Capture
Android App that connects to exploited CPC200-CCPA TTY Log for capture

Hardcoded IP of 192.168.43.1 with a blank root password. CPC200-CCPA must be exploited with dropbear ssh installed, log still located at /tmp/ttyLog and vehicle or Android device connected to the adapters broadcasted AP.

- Connects to adapter
- Streams ttyLog
- Allows recording to file
- Export file
