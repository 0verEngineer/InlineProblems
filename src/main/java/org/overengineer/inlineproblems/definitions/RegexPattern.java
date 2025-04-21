package org.overengineer.inlineproblems.definitions;

import java.util.regex.Pattern;

public class RegexPattern {
    public static final Pattern HTML_TAG_PATTERN = Pattern.compile("</?(b|i|u|html|body|div|span|br|p|code|pre|strong|em)(\\s+[^>]*)?>");
}
