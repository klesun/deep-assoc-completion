package org.klesun.lang.iterators;

import org.klesun.lang.L;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.klesun.lang.Lang.list;
import static org.klesun.lang.Lang.opt;

public class RegexIterator implements Iterator<L<String>>
{
    private boolean hasNext;
    private final Matcher matcher;

    public RegexIterator(String pattern, String text)
    {
        this.matcher = Pattern.compile(pattern)
            .matcher(opt(text).def(""));
        this.hasNext = matcher.find();
    }

    public boolean hasNext()
    {
        return hasNext;
    }

    public L<String> next()
    {
        L<String> groups = list();
        for (int i = 0; i < matcher.groupCount() + 1; ++i) {
            groups.add(matcher.group(i));
        }
        hasNext = matcher.find();
        return groups;
    }
}
