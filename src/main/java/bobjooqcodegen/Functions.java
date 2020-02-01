package bobjooqcodegen;

import bobjooqcodegen.parsers.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static bobthebuilder.services.Log.logWarning;

public enum Functions {;

    public static Path checkValidOutputFile(final Path filename) throws IOException {
        if (filename == null) throw new FileNotFoundException("Generation SQL file is not specified");
        final File file = filename.toFile();
        if (file.exists() && !file.delete()) throw new IOException("Generation SQL file exists and cannot be deleted");
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) throw new IOException("Could not create parent directories for generation SQL file");
        return filename;
    }

    public static Stream<File> listFiles(final String dirname) throws FileNotFoundException {
        if (dirname == null || dirname.isEmpty()) throw new FileNotFoundException("Specified Migration SQL directory is empty");
        final File dir = new File(dirname);
        if (!dir.exists()) throw new FileNotFoundException("Specified Migration SQL directory does not exist");
        if (!dir.isDirectory()) throw new FileNotFoundException("Specified Migration SQL directory is not a directory");
        return Arrays.stream(dir.listFiles());
    }

    public static Stream<String> readAllLines(final File path) {
        try {
            return Files.readAllLines(path.toPath()).stream();
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
