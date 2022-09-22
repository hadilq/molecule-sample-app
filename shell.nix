#
#  Copyright 2022 Hadi Lashkari Ghouchani
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# This is a file to define general dependencies of this app, which we usually don't mention
# in the repositories of our Android apps, but here it's!
# You can use nix package manager in any Linux machine to gather the dependencies of this app
# and have the environment ready for starting the development! But if you don't have that
# no worried! Just use your ordinary setup. By spending a little time and some trial and errors
# you'll get there as usual ;)

let
  pkgs = import (
      fetchTarball https://github.com/nixos/nixpkgs/tarball/dbc00be4985fdef654656092feb35ca2412f2a2b
    ) {
      config.android_sdk.accept_license = true;
      config.allowUnfree = true;
    };

  android = {
    versions = {
      platformTools = "33.0.2";
      buildTools = [
        "30.0.3"
        "31.0.0"
      ];
      ndk = "24.0.8215888"
      ;
      cmake = "3.10.2";
      emulator = "31.3.9";
    };

    platforms = [ "27" "28" "29" "30" "31" "32" ];
    abis = [ "x86" "x86_64" ]; # "armeabi-v7a" "arm64-v8a"
    extras = [ "extras;google;gcm" ];
  };

  sdkArgs = {
    platformToolsVersion = android.versions.platformTools;
    buildToolsVersions = android.versions.buildTools;
    includeEmulator = true;
    emulatorVersion = android.versions.emulator;
    platformVersions = android.platforms;
    includeSources = true;
    includeSystemImages = true;
    systemImageTypes = [ "google_apis_playstore" ];
    abiVersions = android.abis;
    cmakeVersions = [ android.versions.cmake ];
    includeNDK = true;
    ndkVersions = [ android.versions.ndk ];
    useGoogleAPIs = false;
  };

  androidComposition = pkgs.androidenv.composeAndroidPackages sdkArgs;

  androidEmulator = pkgs.androidenv.emulateApp {
    name = "emulate-android-nix";
    platformVersion = "31";
    abiVersion = "x86";
    systemImageType = "google_apis_playstore";
    sdkExtraArgs = sdkArgs;
  };
  
  androidSdk = androidComposition.androidsdk;
  androidSdkHome = "${androidSdk}/libexec/android-sdk";
  platformTools = androidComposition.platform-tools;
  # jdk = "${pkgs.android-studio.unwrapped}/jre";
  aapt2 = "${androidSdkHome}/build-tools/${builtins.toString (builtins.tail android.versions.buildTools)}/aapt2";
  userHome = "${builtins.toString ./.user-home}";
  androidUserHome = "${userHome}/.android";
  androidAvdHome = "${androidUserHome}/avd";

in
pkgs.mkShell {
  name = "android-nix-playground";
  packages = [
    androidSdk
    androidEmulator
    platformTools
  ] ++ (with pkgs; [
    git
    android-studio
    jetbrains.jdk
    gradle
  ]);

  LANG = "C.UTF-8";
  LC_ALL = "C.UTF-8";

  # JAVA_HOME = pkgs.jetbrains.jdk;
  # Note: ANDROID_HOME is deprecated. Use ANDROID_SDK_ROOT.
  ANDROID_SDK_ROOT = androidSdkHome;
  ANDROID_HOME = androidSdkHome;
  ANDROID_NDK_ROOT = "${androidSdkHome}/ndk-bundle";

  # Ensures that we don't have to use a FHS env by using the nix store's aapt2.
  GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${aapt2}";

  HOME = userHome;
  USER_HOME = userHome;
  GRADLE_USER_HOME = "${userHome}/.gradle";
  IDEA_VM_OPTIONS = "${userHome}/.idea-bin/idea64.vmoptions";
  IDEA_PROPERTIES = "${userHome}/.idea-bin/idea.properties";
  ANDROID_USER_HOME = "${androidUserHome}";
  ANDROID_AVD_HOME = "${androidAvdHome}";

  shellHook = ''
    mkdir -p ${androidAvdHome}
    # Add cmake to the path.
    cmake_root="$(echo "$ANDROID_HOME/cmake/${android.versions.cmake}"*/)"
    export PATH="$cmake_root/bin:$PATH"
    # Write out local.properties for Android Studio.
    cat <<EOF > local.properties
      # This file was automatically generated by nix-shell.
      sdk.dir=$ANDROID_SDK_ROOT
      ndk.dir=$ANDROID_NDK_ROOT
      cmake.dir=$cmake_root
    EOF
  '';
}

