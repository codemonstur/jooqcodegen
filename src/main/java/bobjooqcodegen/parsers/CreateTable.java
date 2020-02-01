package bobjooqcodegen.parsers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class CreateTable implements StatementParser {

    @Override
    public boolean matches(final String statement) {
        return statement.startsWith("CREATE TABLE ");
    }

    @Override
    public String convert(final String statement) {
        final int startIndex = statement.indexOf('(');
        final int endIndex = statement.lastIndexOf(')');

        final List<String> middle = new LinkedList<>();
        final Iterator<String> middlePart = Arrays
            .stream(statement.substring(startIndex+1, endIndex).split(","))
            .iterator();
        while (middlePart.hasNext()) {
            String part = middlePart.next().trim();
            if (part.startsWith("KEY ") || part.startsWith("CONSTRAINT ") || part.startsWith("UNIQUE KEY ")) continue;
            if (part.endsWith(" DEFAULT b'0'")) {
                part = part.replaceAll("DEFAULT b'0'", "DEFAULT 0");
            }
            if (part.endsWith(" DEFAULT b'1'")) {
                part = part.replaceAll("DEFAULT b'1'", "DEFAULT 1");
            }
            middle.add(part);
        }

        return statement.substring(0, startIndex)
            + "("
            + String.join(",", middle)
            + ");\n";
    }

}
