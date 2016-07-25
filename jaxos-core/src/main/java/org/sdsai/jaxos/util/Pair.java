package org.sdsai.jaxos.util;

/**
 */
public class Pair<LEFT, RIGHT> {
    public final RIGHT right;
    public final LEFT left;

    public Pair(final LEFT left, final RIGHT right) {
        this.left = left;
        this.right = right;
    }


    public static <LEFT, RIGHT> Pair<LEFT, RIGHT> pair(final LEFT left, final RIGHT right) {
        return new Pair<LEFT, RIGHT>(left, right);
    }
}
