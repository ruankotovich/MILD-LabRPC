/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labrpc.secondquestion.model;

/**
 *
 * @author dmitry
 */
class SizeHandle {

    private static final long GBYTE_SIZE = 1024 * 1024 * 1024;
    private static final long MBYTE_SIZE = 1024 * 1024;
    private static final long KBYTE_SIZE = 1024;

    static String convertUnity(long size) {
        String sz;
        if (size >= GBYTE_SIZE) {
            sz = String.valueOf((size / GBYTE_SIZE)) + "Gb";
        } else if (size >= MBYTE_SIZE) {
            sz = String.valueOf((size / MBYTE_SIZE)) + "Mb";
        } else if (size >= KBYTE_SIZE) {
            sz = String.valueOf((size / KBYTE_SIZE)) + "Kb";
        } else {
            sz = String.valueOf((size)) + "B";
        }

        return sz;
    }
}

public class AbstractFolder {

    private String canonicalName;
    private String canonicalPath;

    public AbstractFolder(String canonicalPath, String canonicalName) {
        this.canonicalPath = canonicalPath;
        this.canonicalName = canonicalName;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public AbstractFolder setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
        return this;
    }

    public String getCanonicalPath() {
        return canonicalPath;
    }

    public AbstractFolder setCanonicalPath(String canonicalPath) {
        this.canonicalPath = canonicalPath;
        return this;
    }

    @Override
    public String toString() {
        return canonicalName;
    }
}
