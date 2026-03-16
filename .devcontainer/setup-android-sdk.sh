#!/usr/bin/env bash
set -euo pipefail

ANDROID_SDK_ROOT="${HOME}/android-sdk"
CMDLINE_TOOLS_DIR="${ANDROID_SDK_ROOT}/cmdline-tools"

sudo apt-get update
sudo apt-get install -y wget unzip

mkdir -p "${CMDLINE_TOOLS_DIR}"
cd /tmp
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
unzip -q -o cmdline-tools.zip
rm -rf "${CMDLINE_TOOLS_DIR}/latest"
mv cmdline-tools "${CMDLINE_TOOLS_DIR}/latest"

# shellcheck disable=SC2016
grep -qxF 'export ANDROID_SDK_ROOT="$HOME/android-sdk"' ~/.bashrc || \
  echo 'export ANDROID_SDK_ROOT="$HOME/android-sdk"' >> ~/.bashrc
# shellcheck disable=SC2016
grep -qxF 'export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"' ~/.bashrc || \
  echo 'export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"' >> ~/.bashrc

export ANDROID_SDK_ROOT
export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"

yes | sdkmanager --licenses >/dev/null
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "Android SDK setup complete."
