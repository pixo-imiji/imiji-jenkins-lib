package de.imiji.jenkins

class CIPreconditions {

    private Object pipeline;

    CIPreconditions(Object pipeline) {
        this.pipeline = pipeline;
    }

    void check() {
//        this.pipeline.cleanWs(disableDeferredWipeout: true, deleteDirs: true)
//        this.sh("find " + this.pipeline.env.WORKSPACE + "/ -name " + "dist -type f delete")
    }
}
