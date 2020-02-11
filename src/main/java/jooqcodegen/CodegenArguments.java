package jooqcodegen;

import jcli.annotations.CliCommand;
import jcli.annotations.CliOption;
import org.jooq.meta.jaxb.Logging;

@CliCommand(name = "jooqcodegen", description = "A bob plugin that generates JOOQ code")
public class CodegenArguments {

    @CliOption(name = 'i', longName = "input-scripts", defaultValue = "src/main/resources/db/migration")
    public String inputScript;
    @CliOption(name = 'o', longName = "output-dir", defaultValue = "target/generate-sources/jooq")
    public String outputDir;

    @CliOption(longName = "wj")
    public boolean withJavadoc;
    @CliOption(longName = "wc")
    public boolean withComments;
    @CliOption(longName = "wes")
    public boolean withEmptySchemas;
    @CliOption(longName = "wec")
    public boolean withEmptyCatalogs;
    @CliOption(longName = "wga")
    public boolean withGeneratedAnnotation;
    @CliOption(longName = "wgcr")
    public boolean withGlobalCatalogReferences;
    @CliOption(longName = "wd")
    public boolean withDaos;
    @CliOption(longName = "wp")
    public boolean withPojos;
    @CliOption(longName = "wi")
    public boolean withIndexes;
    @CliOption(longName = "wk")
    public boolean withKeys;
    @CliOption(longName = "wr")
    public boolean withRecords;

    @CliOption(name = 'l', longName = "logging", defaultValue = "WARN")
    public Logging logging;
    @CliOption(name = 'n', longName = "package-name")
    public String packageName;
    @CliOption(name = 'd', longName = "dont-pre-process")
    public boolean dontPreProcess;

    @CliOption(longName = "pre-process-file", defaultValue = "instructions/schema.sql")
    public String generationSqlFile;

}
