# Runtime install note

The shared UID socket fix is merged into workbench-api-updates.

Phone install test failed with:

INSTALL_FAILED_SHARED_USER_INCOMPATIBLE:
Package com.termux.api.debug has no signatures that match those in shared user com.termux

Conclusion:
A debug Termux:API APK with android:sharedUserId="com.termux" cannot be installed beside the current F-Droid/installed Termux app unless both are signed with the same certificate.

Next runtime lane:
Build/install a matched Termux app + Termux:API app stack signed by the same key, or do not use sharedUserId for side-by-side debug.
