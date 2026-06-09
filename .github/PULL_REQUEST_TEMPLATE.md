## Summary

Describe the user-visible behavior change and why it belongs in Termux:API.

## Scope

- [ ] Android app-side implementation
- [ ] `TermuxApiReceiver` dispatch
- [ ] Manifest permissions / activities / services
- [ ] Debug-only behavior
- [ ] Companion `termux-api-package` wrapper/helper change
- [ ] Documentation only

## Runtime proof

Paste the exact commands and results used to validate the change.

```text
Device:
Android version:
Installed app package:
Installed app version:
Command helper target package/component/socket:
Commands run:
Result:
```

## CI

- [ ] GitHub Actions debug APK build passes
- [ ] Artifact SHA / run ID recorded when runtime testing uses a CI APK

## Package / signing compatibility

- [ ] This change does not require package-side updates
- [ ] Package-side update is included or linked
- [ ] Debug APK package identity is called out when relevant
- [ ] Release/F-Droid replacement risk is called out when relevant

## Android permissions and privacy

- [ ] No new Android permissions
- [ ] New or changed permissions are explained
- [ ] Logs and screenshots are redacted
- [ ] Security-sensitive behavior is documented

## Notes for maintainers

List anything reviewers should pay extra attention to, such as Android-version differences, OEM behavior, known limitations, or follow-up work.
