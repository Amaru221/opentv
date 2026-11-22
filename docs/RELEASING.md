# Releasing

Releases are cut by pushing a tag. GitHub Actions does the rest.

```bash
git tag v0.1.0
git push origin v0.1.0
```

`.github/workflows/release.yml` runs the tests, assembles the release APK and attaches it to a
GitHub release.

## Signing

Releases use the project's **upload keystore**, supplied to CI through four repository secrets:
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. With those present,
`release.yml` decodes the keystore and `app/build.gradle.kts` signs the release with it.

If the secrets are absent — a fork without them, or a local build — Gradle falls back to the
Android debug key so an installable APK is still produced. That fallback is **not
upgrade-compatible**: a different debug key must never be used for two public releases. Releases
0.12.7 and 0.12.8 were built that way, which is why installing one over the other fails. 0.12.9
onwards use the stable key; users coming from 0.12.8 must uninstall once.

An *unsigned* APK cannot be installed on Android at all — the installer rejects it before the
user sees anything, with an error that explains nothing. That is why the debug fallback exists.

### Setting up real signing

1. Generate a keystore (use PKCS12; its key password is the store password):

   ```bash
   keytool -genkeypair -v -keystore upload.jks -keyalg RSA -keysize 4096 \
     -validity 10000 -alias opentv
   ```

2. Add these repository secrets under **Settings → Secrets and variables → Actions**:
   `KEYSTORE_BASE64` (the output of `base64 -i upload.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
   `KEY_PASSWORD`.

3. That is all — `release.yml` and `build.gradle.kts` already pick them up. The release notes
   will report `signed: yes` instead of `debug-key`.

**The keystore must belong to the project, not to one person.** A single maintainer holding
the only copy is the same single point of failure this project exists to avoid — if they go
quiet, nobody can ship an upgrade, and every user is stranded on whatever version they have.
Share it among at least two maintainers, and back it up somewhere that is not one laptop.

Losing the keystore is unrecoverable. There is no reset.

## Version numbers

Bump `versionCode` and `versionName` in `app/build.gradle.kts`. `versionCode` must increase on
every release; Android uses it, not `versionName`, to decide what counts as an upgrade.
