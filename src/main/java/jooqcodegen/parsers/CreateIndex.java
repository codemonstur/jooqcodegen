package jooqcodegen.parsers;

public final class CreateIndex implements StatementParser {

    @Override public boolean matches(final String statement) {
        return statement.startsWith("CREATE INDEX ");
    }

    @Override public String convert(final String statement) {
        return "";
    }

}
