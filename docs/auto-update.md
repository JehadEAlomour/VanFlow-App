# Self-update — how a FlowVan handset replaces its own build

The people carrying these handsets are salesmen, not operators. They will not
notice a banner, cannot judge whether an update matters, and have no idea what
an APK is. So FlowVan does not ask them. On a correctly provisioned handset the
app checks, downloads, verifies and installs its own replacement with nothing on
screen but a progress bar, and comes back by itself afterwards.

This document is the three things that are **not** in the app: the signing key,
the server endpoint, and the one-time provisioning step per device.

---

## 0. Before anything: the signing key

Android replaces an installed app only when the new APK is signed with the **same
key** as the one already on the device. Nothing else in this document works
without that.

FlowVan used to sign `release` with the **debug** key. That key is generated per
developer machine and is not in the repo — so any handset that installed such an
APK becomes permanently un-updatable the day that machine is reimaged. The only
way forward from there is uninstall + reinstall, which takes the local Room
database and any un-synced vouchers with it.

Generate the real key once:

```bash
keytool -genkeypair -v -keystore flowvan-release.jks -alias flowvan -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=FlowVan, O=7Software, C=JO"
```

Then copy `keystore.properties.example` to `keystore.properties` and fill it in.
Both the `.jks` and the `.properties` are gitignored.

**Back up the `.jks` file and its passwords somewhere that outlives this laptop.**
Losing it is not an inconvenience; it is the end of updates for every handset in
the field.

A release built without `keystore.properties` still succeeds — a developer has to
be able to build one — but prints a loud warning. An APK signed that way must
never reach a van.

---

## 1. Versioning

`gradle.properties` is the only place the shipped version is set:

```properties
flowvan.versionCode=7
flowvan.versionName=1.4.2
```

`versionCode` is the **only** number the updater compares. It must increase by 1
on every build you hand out. Ship two different builds under the same code and
the second one is invisible to every handset already carrying the first.

---

## 2. Build variants — one APK per customer

Three customers, one source tree. The `customer` flavour dimension is declared in
`build-logic/.../CustomerFlavors.kt`, and it is the **only** place a customer's
URLs are written down.

| Flavour | applicationId | API base URL | Update manifest |
|---|---|---|---|
| `ferdous` | `com.jehadalomour.flowvan.ferdous` | `http://94.142.51.91:3100/api/v1` | `…/updates/ferdous/android.json` |
| `tal3at` | `com.jehadalomour.flowvan.tal3at` | `http://77.245.5.113:3002/api/v1` | `…/updates/tal3at/android.json` |
| `dev` | `com.jehadalomour.flowvan.dev` | `https://app-dev.7softwarejo.com/api/v1` | `…/updates/dev/android.json` |

It crosses with the existing `gms`/`nogms` dimension, so variants read
`gmsTal3atRelease`, `nogmsFerdousRelease`, and so on:

```bash
./gradlew :composeApp:assembleGmsTal3atRelease
```

**Each customer is a separate package.** That is what makes it impossible — not
merely unlikely — for a Ferdous APK to install over a Tal3at handset and silently
repoint a van full of stock at another company's data. It also means dev and live
builds coexist on your test phone.

The flavour name is appended to the version too (`1.4.2-tal3at`), so a photo of
the settings screen answers "which build is this?" on a support call.

**These values reach the app as BuildConfig fields**, bound into Koin in
`FlowVanApp` and carried around as `AppBuildConfig`. They are not constants in
shared code — that is what they replaced, and the old arrangement made switching
customer an edit to `ApiConfig` that compiled cleanly whichever way round it was.

---

## 3. The update manifest

A **static JSON file** on the update host, sitting next to the APK it describes.
No backend code: releasing is uploading two files.

```
https://7softwarejo.com/flowvan/updates/tal3at/android.json
```

```json
{
  "versionCode": 7,
  "versionName": "1.4.2",
  "apkUrl": "https://7softwarejo.com/flowvan/updates/tal3at/flowvan-7.apk",
  "sha256": "9f2c…",
  "sizeBytes": 23037007,
  "minVersionCode": 5,
  "notes": "إصلاح احتساب ضريبة الدخان"
}
```

Plain JSON — **no `{success, data}` envelope**, no auth. Requested with a
cache-busting `?t=` because Cloudflare sits in front of the host, and a cached
manifest is a fleet that keeps being walled off by a floor that has since been
lowered.

### Why it is not on the API server

Three reasons, each a way the obvious version fails:

- The API lives on **each customer's own box**. A manifest served from there means
  a customer whose server is down cannot be repaired by an update — the one
  moment an update matters most.
- The check runs **in front of sign-in**, because the build that most needs
  replacing is the one whose tokens the server has already stopped accepting. An
  update path that only works for a signed-in rep can repair every build except
  the broken ones.
- It is a file. Nothing to deploy per customer.

| Field | What it does |
|---|---|
| `versionCode` | Newest build. Compared against the running one. |
| `apkUrl` | Absolute URL. Blank means "no update" — the app refuses to wall anyone off behind a download that cannot happen. |
| `sha256` | Lowercase hex of the APK bytes. Checked before install. |
| `sizeBytes` | Progress bar only. `0` gives an indeterminate bar. |
| `minVersionCode` | **The floor.** Running below it → the app is walled off. `0` or absent blocks nobody. |

Get the checksum with:

```bash
shasum -a 256 composeApp/build/outputs/apk/gmsTal3at/release/composeApp-gms-tal3at-release.apk
```

### `versionCode` and `minVersionCode` are separate on purpose

Most releases should not stop anyone mid-round. Leave `minVersionCode` where it
is and the new build is simply available — the fleet takes it the next time the
floor moves past it. Raise it only for a build that genuinely must not be
skipped: a pricing fix, a tax change, a server contract that has already changed
underneath the old app.

**Each customer has its own manifest**, so you can move Tal3at's floor without
touching Ferdous.

---

## 4. Provisioning a handset for silent updates

**This is what makes it zero-tap, and it can only be done on a handset with no
accounts on it yet** — straight out of the box, or straight after a factory
reset, before it is handed to anyone. There is no way to grant it later.

On the handset: finish setup **without adding any Google account**, then enable
developer options and USB debugging.

```bash
adb install composeApp/build/outputs/apk/gmsTal3at/release/composeApp-gms-tal3at-release.apk
```

**The component name carries the customer's applicationId**, so the command
differs per customer. Get it wrong and `dpm` reports the component does not
exist:

```bash
adb shell dpm set-device-owner com.jehadalomour.flowvan.tal3at/com.jehadalomour.flowvan.admin.FlowVanDeviceAdminReceiver
```

| Customer | Device-owner component |
|---|---|
| Ferdous | `com.jehadalomour.flowvan.ferdous/com.jehadalomour.flowvan.admin.FlowVanDeviceAdminReceiver` |
| Tal3at | `com.jehadalomour.flowvan.tal3at/com.jehadalomour.flowvan.admin.FlowVanDeviceAdminReceiver` |
| Dev | `com.jehadalomour.flowvan.dev/com.jehadalomour.flowvan.admin.FlowVanDeviceAdminReceiver` |

The receiver's **class** stays in the `com.jehadalomour.flowvan` package for every
variant — only the package the app is installed under changes, and `dpm` wants
both. The short `.admin.…` form will not work here.

Success prints `Success: Device owner set to package com.jehadalomour.flowvan.tal3at`.

If it fails with *"Not allowed to set the device owner because there are already
some accounts on the device"*, the handset has an account on it. Factory reset
and start again — there is no other route.

Verify:

```bash
adb shell dumpsys device_policy | grep -i "device owner"
```

### What you get from it

- **Silent install.** `PackageInstaller` commits with no dialog.
- **Self-relaunch.** A device owner is exempt from the background-activity-launch
  restriction, so `UpdateRelaunchReceiver` can put the app back on screen after
  the swap. Without it the app just vanishes mid-screen, which gets reported as
  a crash.

### A handset that was never provisioned

Same APK, same flow, one difference: the install ends at the system confirm
dialog, and the update screen changes its wording from *"leave the device on"* to
*"tap Install on the message that appeared"* — telling someone to wait in front
of a dialog that is waiting for *them* is how a van sits still for an hour.

Those handsets also need **Settings → Apps → FlowVan → Install unknown apps →
Allow**, once.

---

## 5. Releasing

`versionCode` is shared across customers — one number for the whole product, so
"everyone is on 7" means something.

1. Bump `flowvan.versionCode` (and `versionName`) in `gradle.properties`.
2. Build that customer's variant — confirm **no** signing warning:
   ```bash
   ./gradlew :composeApp:assembleGmsTal3atRelease
   ```
3. `shasum -a 256` the APK.
4. Upload **two files** to that customer's folder on the update host: the APK, and
   an `android.json` naming it.
5. Raise `minVersionCode` **only** if this build must not be skipped.

A provisioned handset takes it at its next app resume.

**One customer at a time.** Each has its own folder and its own manifest, so
Ferdous can sit on versionCode 6 while Tal3at moves to 7 — which is how you roll
a risky change out to one company first.

---

## 6. Where the code is

| | |
|---|---|
| `build-logic/.../CustomerFlavors.kt` | **The only place a customer's URLs are written down.** |
| `core/common/.../config/AppBuildConfig.kt` | Those values, as something the shared code can be handed. |
| `core/network/.../api/AppVersionApi.kt` | Fetches the static manifest from the update host. |
| `core/network/.../dto/AppVersionDto.kt` | Its payload — every field defaulted, so an old build can always parse it. |
| `core/domain/.../update/CheckForUpdateUseCase.kt` | Decides whether to wall off. **Fails open on every error path** — read the comment there before changing anything. |
| `core/data/.../update/AppInstaller.kt` (+ `.android` / `.ios`) | Download, verify, install. |
| `core/data/.../update/UpdateInstallReceiver.kt` | The OS's answer; opens the confirm dialog on unprovisioned handsets. |
| `composeApp/.../UpdateGate.kt` | The wall. Wraps everything, outside `LocationLock`. |
| `composeApp/.../admin/FlowVanDeviceAdminReceiver.kt` | Exists so `dpm set-device-owner` has a component to name. |
| `composeApp/.../admin/UpdateRelaunchReceiver.kt` | Puts the app back after it replaces itself. |

### The load-bearing decision: it fails open

Every error in the update check — no network, no base URL, a 500, a payload this
build predates — lets the app through. The wall is unconditional and has no way
past it, so a bug that raises it wrongly does not inconvenience a salesman, it
ends their working day in a van with no way to sell, return or collect. A
salesman wrongly running an old build for another hour costs nothing by
comparison.

**iOS does nothing here.** Apple has no route for an app to replace itself. The
check still runs and still reports being behind; only the button is missing.
