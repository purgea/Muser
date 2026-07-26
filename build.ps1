param(
    [ValidateSet("release", "compile", "run", "clean")]
    [string]$Task = "release"
)

$ErrorActionPreference = "Stop"

$projectDir = $PSScriptRoot
$sourceDir = Join-Path $projectDir "src"
$iconsDir = Join-Path $projectDir "icons"
$buildDir = Join-Path $projectDir "build"
$classesDir = Join-Path $buildDir "classes"
$stagingDir = Join-Path $buildDir "jar"
$releaseDir = Join-Path $projectDir "release"
$releaseJar = Join-Path $releaseDir "muser.jar"
$manifestFile = Join-Path $buildDir "MANIFEST.MF"

$dependencyNames = @(
    "jgoodies-forms-1.8.0.jar"
    "miglayout15-swing.jar"
    "jgoodies-common-1.8.0.jar"
    "jfugue-5.0.9.jar"
)
$dependencies = $dependencyNames | ForEach-Object { Join-Path $projectDir $_ }

function Assert-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "'$Name' was not found. Install a Java 17 or newer JDK and add its bin directory to PATH."
    }
}

function Remove-GeneratedFiles {
    if (Test-Path -LiteralPath $buildDir) {
        Remove-Item -LiteralPath $buildDir -Recurse -Force
    }
    if (Test-Path -LiteralPath $releaseDir) {
        Remove-Item -LiteralPath $releaseDir -Recurse -Force
    }
}

function Compile-Project {
    Assert-Command "javac"

    foreach ($dependency in $dependencies) {
        if (-not (Test-Path -LiteralPath $dependency -PathType Leaf)) {
            throw "Missing dependency: $dependency"
        }
    }

    New-Item -ItemType Directory -Path $classesDir -Force | Out-Null

    $sources = @(Get-ChildItem -LiteralPath $sourceDir -Recurse -Filter "*.java" |
        ForEach-Object { $_.FullName })
    if ($sources.Count -eq 0) {
        throw "No Java source files were found in $sourceDir"
    }

    $classpath = $dependencies -join [IO.Path]::PathSeparator
    & javac --release 17 -encoding UTF-8 -cp $classpath -d $classesDir $sources
    if ($LASTEXITCODE -ne 0) {
        throw "Java compilation failed."
    }

    Copy-Item -Path (Join-Path $iconsDir "*") -Destination $classesDir -Force
    Write-Host "Compiled $($sources.Count) source files."
}

function Expand-Jar {
    param(
        [string]$JarPath,
        [string]$Destination
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        foreach ($entry in $archive.Entries) {
            if ([string]::IsNullOrEmpty($entry.Name)) {
                continue
            }

            $relativePath = $entry.FullName.Replace("/", [IO.Path]::DirectorySeparatorChar)
            $destinationPath = [IO.Path]::GetFullPath((Join-Path $Destination $relativePath))
            $destinationRoot = [IO.Path]::GetFullPath($Destination) + [IO.Path]::DirectorySeparatorChar
            if (-not $destinationPath.StartsWith($destinationRoot, [StringComparison]::OrdinalIgnoreCase)) {
                throw "Unsafe path in dependency '$JarPath': $($entry.FullName)"
            }

            $parent = Split-Path $destinationPath
            New-Item -ItemType Directory -Path $parent -Force | Out-Null
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destinationPath, $true)
        }
    }
    finally {
        $archive.Dispose()
    }
}

function New-Release {
    Compile-Project

    if (Test-Path -LiteralPath $stagingDir) {
        Remove-Item -LiteralPath $stagingDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null
    Copy-Item -Path (Join-Path $classesDir "*") -Destination $stagingDir -Recurse -Force

    foreach ($dependency in $dependencies) {
        Expand-Jar -JarPath $dependency -Destination $stagingDir
    }

    $metaInfDir = Join-Path $stagingDir "META-INF"
    if (Test-Path -LiteralPath $metaInfDir) {
        Get-ChildItem -LiteralPath $metaInfDir -File |
            Where-Object { $_.Extension -in ".SF", ".RSA", ".DSA" } |
            Remove-Item -Force
        $dependencyManifest = Join-Path $metaInfDir "MANIFEST.MF"
        if (Test-Path -LiteralPath $dependencyManifest) {
            Remove-Item -LiteralPath $dependencyManifest -Force
        }
    }

    New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null
    @(
        "Manifest-Version: 1.0"
        "Main-Class: jarsick.muser.gui.MuserGUI"
        "Add-Exports: java.desktop/com.sun.media.sound"
        ""
    ) | Set-Content -LiteralPath $manifestFile -Encoding ASCII

    $jarManifestDir = Join-Path $stagingDir "META-INF"
    New-Item -ItemType Directory -Path $jarManifestDir -Force | Out-Null
    Copy-Item -LiteralPath $manifestFile -Destination (Join-Path $jarManifestDir "MANIFEST.MF") -Force

    if (Test-Path -LiteralPath $releaseJar) {
        Remove-Item -LiteralPath $releaseJar -Force
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $outputStream = [IO.File]::Open($releaseJar, [IO.FileMode]::CreateNew)
    $archive = New-Object IO.Compression.ZipArchive(
        $outputStream,
        [IO.Compression.ZipArchiveMode]::Create
    )
    try {
        $stagingRoot = [IO.Path]::GetFullPath($stagingDir).TrimEnd("\", "/") +
            [IO.Path]::DirectorySeparatorChar
        foreach ($file in Get-ChildItem -LiteralPath $stagingDir -Recurse -File) {
            $entryName = $file.FullName.Substring($stagingRoot.Length).Replace("\", "/")
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                $archive,
                $file.FullName,
                $entryName,
                [IO.Compression.CompressionLevel]::Optimal
            ) | Out-Null
        }
    }
    finally {
        $archive.Dispose()
        $outputStream.Dispose()
    }

    Write-Host "Release created: $releaseJar"
}

switch ($Task) {
    "clean" {
        Remove-GeneratedFiles
        Write-Host "Removed build and release directories."
    }
    "compile" {
        Compile-Project
    }
    "release" {
        New-Release
    }
    "run" {
        Assert-Command "java"
        New-Release
        & java -jar $releaseJar
        if ($LASTEXITCODE -ne 0) {
            throw "Muser exited with code $LASTEXITCODE."
        }
    }
}
