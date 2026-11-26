#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

##############################################################################
#
#   Gradle start up script for UN*X
#
#   Requires Java 8 or higher, and a POSIX-compliant shell.
#
##############################################################################

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no slash
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in
        /*)   app_path=$link ;;
        *)    app_path=$APP_HOME$link ;;
    esac
done

# This is normally a little awkward, but the shell expansion is quite portable.
APP_HOME=$( cd -P "${APP_HOME:-./}" && pwd -P ) || exit

APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

classpath=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    if ! command -v java >/dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
fi

# Increase the maximum file descriptors if possible.
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in
      maximum)
        # In POSIX sh, ulimit -n is undefined.
        MAX_FD=$( ulimit -H -n )
        if [ $? -eq 0 ] ; then
          if [ -z "$MAX_FD" ] || [ "$MAX_FD" = "unlimited" ] || [ "$MAX_FD" -gt 65536 ] ; then
            MAX_FD=65536
          fi
        else
          warn "Could not query maximum file descriptor limit (ulimit -H -n)"
        fi
        ;;
      *)
        warn "Maximum file descriptors limit: $MAX_FD"
        ;;
    esac
    if [ -n "$MAX_FD" ] ; then
        ulimit -n "$MAX_FD" 2>/dev/null || warn "Could not set maximum file descriptor limit to $MAX_FD"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if "$cygwin" || "$msys" ; then
    APP_HOME=$( cygpath --path --mixed "$APP_HOME" )
    classpath=$( cygpath --path --mixed "$classpath" )
    JAVACMD=$( cygpath --unix "$JAVACMD" )

    # We build the converted path by concatenating the converted elements
    # with the appropriate path separator.
    IFS=':'
    for path in $classpath ; do
        if [ -n "$path" ] ; then
            path=$( cygpath --path --mixed "$path" )
            if [ -z "$CP" ] ; then
                CP="$path"
            else
                CP="$CP;$path"
            fi
        fi
    done
    classpath=$CP
    unset IFS
fi

# Collect all arguments for the java command;
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, and GRADLE_OPTS are passed to the JVM
#   * CLI args are passed to Gradle
set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$classpath" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"

exec "$JAVACMD" "$@"
