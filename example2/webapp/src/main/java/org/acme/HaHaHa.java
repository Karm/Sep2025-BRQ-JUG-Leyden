package org.acme;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.RecomputeFieldValue;



import io.smallrye.common.constraint.Assert;

@TargetClass(className = "io.quarkus.runtime.configuration.NameIterator")
final class Target_io_quarkus_runtime_configuration_NameIterator{


    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    public static  int MAX_LENGTH = 2048;
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static  int POS_MASK = 0x0FFF;
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static  int POS_BITS = 12;
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static  int SE_SHIFT = 32 - POS_BITS;
    @Alias
    private  String name;
    @Alias
    private int pos;

    /*
     * next-iteration DFA
     * <any> → <end> ## on EOI
     * I → <end> ## on '.'
     * I → Q ## on '"'
     * Q → I ## on '"'
     * Q → QBS ## on '\'
     * QBS → Q ## on any
     * I → BS ## on '\'
     * BS → I ## on any
     */
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static  int FS_INITIAL = 0;
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static  int FS_QUOTE = 1;
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static  int FS_BACKSLASH = 2;
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static  int FS_QUOTE_BACKSLASH = 3;

    /*
     * Iteration cookie format
     *
     * Bit: 14...12 11 ... 0
     * ┌───────┬────────────┐
     * │ state │ position │
     * │ │ (signed) │
     * └───────┴────────────┘
     */



    public Target_io_quarkus_runtime_configuration_NameIterator(final String name) {
        this(name, false);
    }

    public Target_io_quarkus_runtime_configuration_NameIterator(final String name, final boolean startAtEnd) {
        this(name, startAtEnd ? name.length() : -1);
    }

    public Target_io_quarkus_runtime_configuration_NameIterator(final String name, final int pos) {
        Assert.checkNotNullParam("name", name);
        if (name.length() > MAX_LENGTH)
            throw new IllegalArgumentException("Name is too long");
        Assert.checkMinimumParameter("pos", -1, pos);
        Assert.checkMaximumParameter("pos", name.length(), pos);
        if (pos != -1 && pos != name.length() && name.charAt(pos) != '.')
            throw new IllegalArgumentException("Position is not located at a delimiter");
        this.name = name;
        this.pos = pos;
    }




    @Substitute
    public int getNextEnd() {
        System.out.print('.');
        int cookie = initIteration();
        do {
            cookie = nextPos(cookie);
        } while (!isSegmentDelimiter(cookie));
        return getPosition(cookie);
    }

    @Alias
    private boolean isEndOfString(int cookie) {
        return getPosition(cookie) == name.length();
    }

    @Alias
    private boolean isStartOfString(int cookie) {
        return getPosition(cookie) == -1;
    }

    @Alias
    private int charAt(int cookie) {
        return name.charAt(getPosition(cookie));
    }

    @Alias
    private boolean isSegmentDelimiter(int cookie) {
        return isStartOfString(cookie) || isEndOfString(cookie) || getState(cookie) == FS_INITIAL && charAt(cookie) == '.';
    }
    @Alias
    private int getPosition(int cookie) {
        return (cookie & POS_MASK) << SE_SHIFT >> SE_SHIFT;
    }
    @Alias
    private int initIteration() {
        return this.pos & POS_MASK;
    }
    @Alias
    private int getState(int cookie) {
        return cookie >> POS_BITS;
    }
    @Alias
    private int cookieOf(int state, int pos) {
        return state << POS_BITS | pos & POS_MASK;
    }
    @Alias
    private int nextPos(int cookie) {
        int pos = getPosition(cookie);
        if (isEndOfString(cookie)) {
            throw new NoSuchElementException();
        }
        int state = getState(cookie);
        int ch;
        for (;;) {
            pos++;
            if (pos == name.length()) {
                return cookieOf(state, pos);
            }
            ch = name.charAt(pos);
            if (state == FS_INITIAL) {
                if (ch == '.') {
                    return cookieOf(state, pos);
                } else if (ch == '"') {
                    state = FS_QUOTE;
                } else if (ch == '\\') {
                    state = FS_BACKSLASH;
                } else {
                    return cookieOf(state, pos);
                }
            } else if (state == FS_QUOTE) {
                if (ch == '"') {
                    state = FS_INITIAL;
                } else if (ch == '\\') {
                    state = FS_QUOTE_BACKSLASH;
                } else {
                    return cookieOf(state, pos);
                }
            } else if (state == FS_BACKSLASH) {
                state = FS_INITIAL;
                return cookieOf(state, pos);
            } else {
                assert state == FS_QUOTE_BACKSLASH;
                state = FS_QUOTE;
                return cookieOf(state, pos);
            }
        }
    }
}
public class HaHaHa {
}
