#!/usr/bin/env bash
# One-time setup: creates the release signing key and stores it as GitHub
# repository secrets for .github/workflows/release.yml.
#
# Every published APK must be signed with this same key, or Android refuses to
# update an installed copy. Keep the backup this script writes somewhere safe
# (a password manager or encrypted drive): losing it means users must uninstall
# and reinstall to get any later version.
#
# Needs: keytool (comes with any JDK), openssl, and the GitHub CLI logged in
# with access to this repository.
set -euo pipefail

backup_dir="${HOME}/.android/roundthecorner-release"
keystore="${backup_dir}/release.jks"
alias_name="roundthecorner"

if [ -e "$keystore" ]; then
  echo "A keystore already exists at $keystore."
  echo "Reusing it would be fine, but this script won't overwrite it. Move it away first if you really want a new key."
  exit 1
fi

for tool in keytool openssl gh base64; do
  command -v "$tool" >/dev/null || { echo "Missing required tool: $tool"; exit 1; }
done
gh auth status >/dev/null

mkdir -p "$backup_dir"
chmod 700 "$backup_dir"

# PKCS12 keystores use one password for the store and the key.
password="$(openssl rand -base64 32 | tr -d '/+=' | cut -c1-32)"

keytool -genkeypair \
  -keystore "$keystore" -storetype PKCS12 \
  -alias "$alias_name" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$password" -keypass "$password" \
  -dname "CN=RoundTheCorner"
chmod 600 "$keystore"

# Local backup of the credentials next to the key.
printf 'alias=%s\npassword=%s\n' "$alias_name" "$password" > "${backup_dir}/credentials.txt"
chmod 600 "${backup_dir}/credentials.txt"

# Secrets are piped on stdin so they never appear in the process list or shell history.
base64 < "$keystore" | tr -d '\n' | gh secret set RTC_KEYSTORE_BASE64
printf '%s' "$password"   | gh secret set RTC_KEYSTORE_PASSWORD
printf '%s' "$password"   | gh secret set RTC_KEY_PASSWORD
printf '%s' "$alias_name" | gh secret set RTC_KEY_ALIAS

echo
echo "Done. The release key and its password are backed up in:"
echo "  $backup_dir"
echo "Copy that folder somewhere safe. Then publish a version with:"
echo "  git tag v0.1.0 && git push origin v0.1.0"
