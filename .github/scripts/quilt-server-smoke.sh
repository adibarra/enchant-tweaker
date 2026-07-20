#!/usr/bin/env bash
set -euo pipefail

project_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
temp_root=${RUNNER_TEMP:-${TMPDIR:-/tmp}}
work_dir=$(mktemp -d "$temp_root/enchant-tweaker-quilt.XXXXXX")
server_dir="$work_dir/server"
server_pid=""
console_fd_open=false

cleanup() {
    if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
        kill "$server_pid" 2>/dev/null || true
        wait "$server_pid" 2>/dev/null || true
    fi
    if [[ "$console_fd_open" == true ]]; then
        exec 3>&-
    fi
    if [[ -n "$work_dir" && -d "$work_dir" && "$work_dir" == "$temp_root"/enchant-tweaker-quilt.* ]]; then
        rm -rf -- "$work_dir"
    fi
}
trap cleanup EXIT

read_property() {
    sed -n "s/^$1=//p" "$project_root/gradle.properties"
}

minecraft_version=$(read_property minecraft_version)
mod_version=$(read_property mod_version)
fabric_version=$(read_property fabric_version)
quilt_loader_version=$(read_property quilt_loader_version)
quilt_installer_version=$(read_property quilt_installer_version)
mod_jar="$project_root/build/libs/enchanttweaker-$mod_version+mc$minecraft_version.jar"

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

installer="$work_dir/quilt-installer.jar"
fabric_api="$work_dir/fabric-api.jar"
echo "Downloading Quilt installer $quilt_installer_version"
curl --fail --location --progress-bar \
    "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/$quilt_installer_version/quilt-installer-$quilt_installer_version.jar" \
    --output "$installer"
echo "Installing Quilt Loader $quilt_loader_version for Minecraft $minecraft_version"
java -jar "$installer" install server "$minecraft_version" "$quilt_loader_version" \
    --install-dir="$server_dir" --download-server

mkdir -p "$server_dir/mods"
cp "$mod_jar" "$server_dir/mods/"
echo "Downloading Fabric API $fabric_version"
curl --fail --location --progress-bar \
    "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$fabric_version/fabric-api-$fabric_version.jar" \
    --output "$fabric_api"
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
    if ! kill -0 "$server_pid" 2>/dev/null; then
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
wait "$server_pid"
server_pid=""
grep -E 'Loading Minecraft .* with Quilt Loader|\[EnchantTweaker\] Ready to go! Applied|== Mod Status ==' "$server_log"
echo "Quilt compatibility smoke test passed"
