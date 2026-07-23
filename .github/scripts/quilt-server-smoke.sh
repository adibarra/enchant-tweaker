#!/usr/bin/env bash
set -euo pipefail

project_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
temp_root=${RUNNER_TEMP:-${TMPDIR:-/tmp}}
work_dir=$(mktemp -d "$temp_root/enchant-tweaker-quilt.XXXXXX")
server_dir="$work_dir/server"
server_pid=""
console_fd_open=false
server_shutdown_timeout=15

server_has_exited() {
    ! kill -0 "$server_pid" 2>/dev/null \
        || [[ "$(ps -o stat= -p "$server_pid" 2>/dev/null)" == Z* ]]
}

reap_server() {
    local status
    if wait "$server_pid" 2>/dev/null; then
        status=0
    else
        status=$?
    fi
    server_pid=""
    return "$status"
}

wait_for_server() {
    local attempts=$1
    for ((attempt = 1; attempt <= attempts; attempt++)); do
        if server_has_exited; then
            if reap_server; then
                return 0
            fi
            return 1
        fi
        sleep 1
    done
    return 1
}

stop_server() {
    if [[ -z "$server_pid" ]]; then
        return 0
    fi
    if wait_for_server "$server_shutdown_timeout"; then
        return 0
    fi
    if [[ -z "$server_pid" ]]; then
        return 1
    fi
    echo "Quilt server did not stop after ${server_shutdown_timeout} seconds" >&2
    kill -KILL "$server_pid" 2>/dev/null || true
    reap_server || true
    return 1
}

cleanup() {
    if [[ "$console_fd_open" == true ]]; then
        exec 3>&-
        console_fd_open=false
    fi
    stop_server || true
    if [[ -n "$work_dir" && -d "$work_dir" && "$work_dir" == "$temp_root"/enchant-tweaker-quilt.* ]]; then
        rm -rf -- "$work_dir"
    fi
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

read_property() {
    local key=$1
    local -a values
    mapfile -t values < <(sed -n "s/^${key}=//p" "$project_root/gradle.properties")
    if (( ${#values[@]} != 1 )); then
        echo "Expected exactly one $key property in gradle.properties" >&2
        exit 1
    fi
    printf '%s' "${values[0]}"
}

minecraft_version=$(read_property minecraft_version)
mod_version=$(read_property mod_version)
fabric_version=$(read_property fabric_version)
quilt_loader_version=$(read_property quilt_loader_version)
quilt_installer_version=$(read_property quilt_installer_version)
mod_jar="$project_root/build/libs/enchanttweaker-$mod_version+mc$minecraft_version.jar"
# pinned to versions in gradle.properties
quilt_installer_sha256=f0c6e04e7f3b932d801b9e783ae17c960ff3cadc0f0109d6cc9be5240e99d455
fabric_api_sha256=52f024d19b17e87abea24b8568efea4fcfc2ad96ded16d65c70044924c29e29e


for value in minecraft_version mod_version fabric_version quilt_loader_version quilt_installer_version; do
    if [[ -z "${!value}" ]]; then
        echo "Missing $value in gradle.properties" >&2
        exit 1
    fi
done
if [[ ! -s "$mod_jar" ]]; then
    echo "Missing built mod jar: $mod_jar" >&2
    exit 1
fi
if ! command -v jq >/dev/null 2>&1; then
    echo "jq is required to read Mojang metadata" >&2
    exit 1
fi

installer="$work_dir/quilt-installer.jar"
version_manifest="$work_dir/version-manifest.json"
version_metadata="$work_dir/version.json"
server_jar="$server_dir/server.jar"
fabric_api="$work_dir/fabric-api.jar"
download_connect_timeout=15
download_timeout=120
installer_timeout=300

echo "Downloading Quilt installer $quilt_installer_version"
curl --fail --location --progress-bar \
    --connect-timeout "$download_connect_timeout" --max-time "$download_timeout" \
    "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/$quilt_installer_version/quilt-installer-$quilt_installer_version.jar" \
    --output "$installer"
if ! printf '%s  %s\n' "$quilt_installer_sha256" "$installer" | sha256sum --check --status; then
    echo "Quilt installer checksum mismatch" >&2
    exit 1
fi
echo "Downloading Mojang version metadata for $minecraft_version"
curl --fail --silent --show-error \
    --connect-timeout "$download_connect_timeout" --max-time "$download_timeout" \
    "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json" \
    --output "$version_manifest"
version_metadata_url=$(jq -er --arg version "$minecraft_version" \
    'first(.versions[] | select(.id == $version) | .url)' "$version_manifest")
case "$version_metadata_url" in
    https://piston-meta.mojang.com/*) ;;
    *)
        echo "Unexpected Mojang metadata URL" >&2
        exit 1
        ;;
esac
curl --fail --silent --show-error \
    --connect-timeout "$download_connect_timeout" --max-time "$download_timeout" \
    "$version_metadata_url" --output "$version_metadata"
server_download_url=$(jq -er '.downloads.server.url' "$version_metadata")
server_sha1=$(jq -er '.downloads.server.sha1' "$version_metadata")
case "$server_download_url" in
    https://piston-data.mojang.com/*) ;;
    *)
        echo "Unexpected Minecraft server URL" >&2
        exit 1
        ;;
esac
if [[ ! "$server_sha1" =~ ^[[:xdigit:]]{40}$ ]]; then
    echo "Invalid Minecraft server digest" >&2
    exit 1
fi

echo "Installing Quilt Loader $quilt_loader_version for Minecraft $minecraft_version"

timeout "${installer_timeout}s" java -jar "$installer" install server "$minecraft_version" "$quilt_loader_version" \
    --install-dir="$server_dir" --download-server
if [[ ! -s "$server_jar" ]]; then
    echo "Missing downloaded Minecraft server: $server_jar" >&2
    exit 1
fi
if ! printf '%s  %s\n' "$server_sha1" "$server_jar" | sha1sum --check --status; then
    echo "Minecraft server checksum mismatch" >&2
    exit 1
fi

mkdir -p "$server_dir/mods"
cp "$mod_jar" "$server_dir/mods/"
echo "Downloading Fabric API $fabric_version"
curl --fail --location --progress-bar \
    --connect-timeout "$download_connect_timeout" --max-time "$download_timeout" \
    "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$fabric_version/fabric-api-$fabric_version.jar" \
    --output "$fabric_api"
if ! printf '%s  %s\n' "$fabric_api_sha256" "$fabric_api" | sha256sum --check --status; then
    echo "Fabric API checksum mismatch" >&2
    exit 1
fi

cp "$fabric_api" "$server_dir/mods/"
printf 'eula=true\n' > "$server_dir/eula.txt"

console_fifo="$work_dir/console"
server_log="$work_dir/server.log"
mkfifo "$console_fifo"
(
    cd "$server_dir"
    exec java -Xmx1G -jar quilt-server-launch.jar nogui
) < "$console_fifo" > "$server_log" 2>&1 &
server_pid=$!
exec 3> "$console_fifo"
console_fd_open=true

ready=false
for ((attempt = 1; attempt <= 120; attempt++)); do
    if grep -Fq 'Done (' "$server_log"; then
        ready=true
        break
    fi
    if server_has_exited; then
        echo "Quilt server exited before becoming ready" >&2
        cat "$server_log" >&2
        exit 1
    fi
    printf '\rQuilt startup: %3d%%' $((attempt * 100 / 120))
    sleep 1
done
printf '\rQuilt startup: 100%%\n'
if [[ "$ready" != true ]]; then
    echo "Quilt server did not become ready within 120 seconds" >&2
    cat "$server_log" >&2
    exit 1
fi

grep -Fq "Loading Minecraft $minecraft_version with Quilt Loader $quilt_loader_version" "$server_log"
grep -Fq '[EnchantTweaker] Ready to go! Applied ' "$server_log"
printf 'et diagnose\n' >&3

healthy=false
for ((attempt = 1; attempt <= 30; attempt++)); do
    if grep -Fq '== Mod Status ==' "$server_log"; then
        healthy=true
        break
    fi
    if server_has_exited; then
        echo "Quilt server exited before the diagnose command responded" >&2
        cat "$server_log" >&2
        exit 1
    fi
    sleep 1
done
if [[ "$healthy" != true ]]; then
    echo "Enchant Tweaker command did not respond under Quilt" >&2
    cat "$server_log" >&2
    exit 1
fi

printf 'stop\n' >&3
exec 3>&-
console_fd_open=false
if ! stop_server; then
    echo "Quilt server failed to stop cleanly" >&2
    exit 1
fi
grep -E 'Loading Minecraft .* with Quilt Loader|\[EnchantTweaker\] Ready to go! Applied|== Mod Status ==' "$server_log"
echo "Quilt compatibility smoke test passed"
