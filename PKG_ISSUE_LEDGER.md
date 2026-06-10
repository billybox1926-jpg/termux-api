# Termux:API-Package Issue Ledger

Branch: workbench-package-updates
Last Updated: 2026-06-10

## SUMMARY

Total upstream open issues: 38
Fixed in this fork: 1
Remaining actionable: ~37

## FIXED ISSUES (1 total)

### Fixed in this session - 1 issue
- #224 run_api_command leaks file descriptors → Fixed (close input_server_socket and output_server_socket before return in termux-api.c)

## REMAINING UPSTREAM OPEN ISSUES

### Package repo (termux-api-package) - 38 open issues

Key issues that may be fixable without phone testing:
- #221 Termux-api feature request (vague)
- #215 Cannot start in Android 16 as new intent
- #200 termux-api command should detect if Termux:API plugin is not installed
- #197 termux-telephony-call
- #193 Brightness: logarithmic or exponential
- #169 termux-camera-photo Directly return image data?
- #166 Background jobs: Occasional hang
- #165 termux-microphone-record foreground argument
- #164 termux-job-scheduler: New jobs default to job ID 0
- #158 sms-list doesnt return images/pictures
- #138 termux-api never returns if Android app is not installed
- #137 termux-speech-to-text buffers progressive output
