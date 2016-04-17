package org.e5200256.filetransfer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Arguments {
    // TO!DO: not necessary to use List<> at this moment, ro at most

    List<String> content;

//    private Arguments(List<String> content) {
//        this.content = content;
//    }

    Arguments(String[] content) {
        this(Arrays.asList(content));
    }

    Arguments(List<String> content) {
        this.content = content;
    }

    public static Arguments CreateWithSpaceSpliting(String concatd) {
        return new Arguments(parse(concatd));
    }

    static List<String> parse(String concatd) {
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(concatd);

        List<String> found = new LinkedList<>();
        while (regexMatcher.find()) {
            found.add(regexMatcher.group());
        }

        return found;
    }

    public String get(int index) {
        if (index > -1 && index < content.size())
            return this.content.get(index);
        return "";
    }

    public int length() {
        return this.content.size();
    }

    public String[] toStringArray() {
        if (this.content.size() == 0) return new String[0];
        return this.content.toArray(new String[this.content.size()]);
    }

    public int[] parseToIntegerArray() throws NumberFormatException {
        if (this.content.size() == 0) return new int[0];
        // JAVA 8
        return this.content.stream().flatMapToInt(e -> IntStream.of(Integer.valueOf(e))).toArray();
    }
}