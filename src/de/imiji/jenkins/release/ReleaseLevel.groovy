package de.imiji.jenkins.release

enum ReleaseLevel {
    major, minor, patch, premajor, preminor,
    prepatch, prerelease

    static ReleaseLevel valueOfName(String name) {
        values().find { it.name == name }
    }
}