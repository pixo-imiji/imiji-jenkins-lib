package de.imiji.jenkins.constants

enum Stage {
    DEV("", "", "", "", "", "e"),
    PROD("", "", "", "", "", "p")

    private String host
    private String user
    private String password
    private String nodeHost
    private String nodeUser
    private String env

    private Stage(String host, String user, String password, String nodeHost, String nodeUser, String env) {
        this.host = host
        this.user = user
        this.password = password
        this.nodeHost = nodeHost
        this.nodeUser = nodeUser
        this.env = env
    }

    String getHost() {
        return host
    }

    String getUser() {
        return user
    }

    String getPassword() {
        return password
    }

    String getNodeHost() {
        return nodeHost
    }

    String getNodeUser() {
        return nodeUser
    }

    String getEnv() {
        return env
    }
}