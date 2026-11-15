#!/usr/bin/env bash
# Builds the native VPN engine (Xray-core) into app/libs/tulporcore.aar.
# Requires Go >= 1.24, Android SDK + NDK (ANDROID_HOME, ANDROID_NDK_HOME).
set -euo pipefail
cd "$(dirname "$0")/../core"
export PATH="$PATH:$(go env GOPATH)/bin"
command -v gomobile >/dev/null || go install golang.org/x/mobile/cmd/gomobile@latest golang.org/x/mobile/cmd/gobind@latest
mkdir -p ../app/libs
gomobile bind -target=android/arm64,android/arm,android/amd64 -androidapi 24 \
  -javapkg=uz.tulpor -ldflags="-s -w -buildid=" -trimpath -o ../app/libs/tulporcore.aar .
echo "Built app/libs/tulporcore.aar"
