#!/usr/bin/env bash

set -Eeuo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
source_dir="$project_dir/src"
icons_dir="$project_dir/icons"
build_dir="$project_dir/build"
classes_dir="$build_dir/classes"
staging_dir="$build_dir/jar"
release_dir="$project_dir/release"
release_jar="$release_dir/muser.jar"
manifest_file="$build_dir/MANIFEST.MF"

dependencies=(
    "$project_dir/jgoodies-forms-1.8.0.jar"
    "$project_dir/miglayout15-swing.jar"
    "$project_dir/jgoodies-common-1.8.0.jar"
    "$project_dir/jfugue-5.0.9.jar"
)

usage() {
    cat <<'EOF'
Usage: ./build.sh [release|compile|run|clean]

Commands:
  release  Compile the project and create release/muser.jar (default)
  compile  Compile Java sources into build/classes
  run      Build the release JAR and launch it
  clean    Remove the generated build and release directories
EOF
}

assert_command() {
    local command_name="$1"
    if ! command -v "$command_name" >/dev/null 2>&1; then
        printf "Error: '%s' was not found. Install a Java 17 or newer JDK and add it to PATH.\n" \
            "$command_name" >&2
        exit 1
    fi
}

check_dependencies() {
    local dependency
    for dependency in "${dependencies[@]}"; do
        if [[ ! -f "$dependency" ]]; then
            printf 'Error: missing dependency: %s\n' "$dependency" >&2
            exit 1
        fi
    done
}

compile_project() {
    assert_command javac
    check_dependencies

    if [[ ! -d "$source_dir" ]]; then
        printf 'Error: source directory was not found: %s\n' "$source_dir" >&2
        exit 1
    fi

    local -a sources=()
    while IFS= read -r -d '' source_file; do
        sources+=("$source_file")
    done < <(find "$source_dir" -type f -name '*.java' -print0)

    if ((${#sources[@]} == 0)); then
        printf 'Error: no Java source files were found in %s\n' "$source_dir" >&2
        exit 1
    fi

    mkdir -p "$classes_dir"

    local classpath
    classpath="$(IFS=:; printf '%s' "${dependencies[*]}")"
    javac --release 17 -encoding UTF-8 -cp "$classpath" -d "$classes_dir" "${sources[@]}"

    cp -a "$icons_dir"/. "$classes_dir"/
    printf 'Compiled %d source files.\n' "${#sources[@]}"
}

create_release() {
    assert_command jar
    compile_project

    rm -rf -- "$staging_dir"
    mkdir -p "$staging_dir"
    cp -a "$classes_dir"/. "$staging_dir"/

    local dependency
    for dependency in "${dependencies[@]}"; do
        (cd "$staging_dir" && jar xf "$dependency")
    done

    if [[ -d "$staging_dir/META-INF" ]]; then
        find "$staging_dir/META-INF" -type f \
            \( -name '*.SF' -o -name '*.RSA' -o -name '*.DSA' -o -name 'MANIFEST.MF' \) \
            -delete
    fi

    mkdir -p "$release_dir" "$staging_dir/META-INF"
    printf '%s\n' \
        'Manifest-Version: 1.0' \
        'Main-Class: jarsick.muser.gui.MuserGUI' \
        'Add-Exports: java.desktop/com.sun.media.sound' \
        > "$manifest_file"

    rm -f -- "$release_jar"
    jar --create --file "$release_jar" --manifest "$manifest_file" \
        -C "$staging_dir" .

    printf 'Release created: %s\n' "$release_jar"
}

clean_generated_files() {
    rm -rf -- "$build_dir" "$release_dir"
    printf 'Removed build and release directories.\n'
}

task="${1:-release}"
if (($# > 1)); then
    printf 'Error: expected at most one command.\n\n' >&2
    usage >&2
    exit 2
fi

case "$task" in
    clean)
        clean_generated_files
        ;;
    compile)
        compile_project
        ;;
    release)
        create_release
        ;;
    run)
        assert_command java
        create_release
        java -jar "$release_jar"
        ;;
    -h|--help|help)
        usage
        ;;
    *)
        printf "Error: unknown command '%s'.\n\n" "$task" >&2
        usage >&2
        exit 2
        ;;
esac
