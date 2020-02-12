package jooqcodegen;

import jooqcodegen.parsers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static bobthebuildtool.services.Log.logWarning;

public enum Functions {;

    public static Stream<String> readAllLines(final Path path) {
        try {
            return Files.readAllLines(path).stream();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static int toNumber(final String name) {
        final int periodIndex = name.indexOf('.');
        final int underscoreIndex = name.indexOf("__");
        return ( periodIndex == -1 || underscoreIndex == -1 )
            ? 0
            : parseInt(name.substring(periodIndex+1, underscoreIndex), 0);
    }
    private static int parseInt(final String value, final int _default) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return _default;
        }
    }

    private static final List<StatementParser> parsers = Arrays.asList(new ForeignKeyChecks()
            , new NameUTF8(), new CreateTable(), new AlterTable(), new DropTable());

    public static String toJooqSafeStatement(final String statement) {
        for (final StatementParser parser : parsers) {
            if (parser.matches(statement))
                return parser.convert(statement);
        }
        logWarning("No parser matched with statement: " + statement);
        return statement;
    }

}
