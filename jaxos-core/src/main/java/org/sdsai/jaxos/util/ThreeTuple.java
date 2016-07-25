package org.sdsai.jaxos.util;

/**
 */
public class ThreeTuple<LEFT, MIDDLE, RIGHT> extends Pair<LEFT, RIGHT> {
    public final MIDDLE middle;
    public ThreeTuple(final LEFT left, final MIDDLE middle, final RIGHT right) {
        super(left, right);
        this.middle = middle;
    }

    public static <LEFT, MIDDLE, RIGHT> ThreeTuple<LEFT, MIDDLE, RIGHT> threeTuple(final LEFT left, final MIDDLE middle, final RIGHT right) {
        return new ThreeTuple<LEFT, MIDDLE, RIGHT>(left, middle, right);
    }
}
