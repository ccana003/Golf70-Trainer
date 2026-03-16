# Golf70-Trainer

Golf training app to smash your goals.

## Running Android unit tests in GitHub Codespaces

This project currently does **not** include a Gradle wrapper, so a first-time Codespaces setup is required.

### 1) Install Java + Android SDK command line tools

```bash
sudo apt-get update && sudo apt-get install -y openjdk-17-jdk wget unzip
mkdir -p "$HOME/android-sdk/cmdline-tools"
cd /tmp
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip
mv cmdline-tools "$HOME/android-sdk/cmdline-tools/latest"
```

### 2) Configure environment variables

```bash
export ANDROID_SDK_ROOT="$HOME/android-sdk"
export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"
```

To persist this in Codespaces shells:

```bash
echo 'export ANDROID_SDK_ROOT="$HOME/android-sdk"' >> ~/.bashrc
echo 'export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"' >> ~/.bashrc
```

### 3) Accept licenses and install required SDK components

```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 4) Generate the Gradle wrapper (one time)

```bash
gradle wrapper
```

After this, use `./gradlew` commands.

### 5) Run tests

```bash
./gradlew testDebugUnitTest
# or all unit tests
./gradlew test
```

### Troubleshooting

If tests fail in Codespaces, try these common fixes:

1. **SDK path issue**: create `local.properties` with:
   ```properties
   sdk.dir=/home/codespace/android-sdk
   ```
2. **Missing SDK pieces**: rerun
   ```bash
   sdkmanager "platforms;android-34" "build-tools;34.0.0"
   ```
3. **Java version mismatch**:
   ```bash
   java -version
   ```
   Ensure Java 17 is active.

## Optional: automatic setup for Codespaces

A starter `.devcontainer/devcontainer.json` and bootstrap script are included in this repository so Android tooling can be installed automatically at container startup.
