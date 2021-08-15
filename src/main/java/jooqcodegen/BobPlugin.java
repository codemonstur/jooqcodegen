package jooqcodegen;

import bobthebuildtool.pojos.buildfile.Project;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static bobthebuildtool.services.Functions.isNullOrEmpty;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.joining;
import static jcli.CliParserBuilder.newCliParser;
import static jooqcodegen.CodegenArguments.DEFAULT_INPUT_LOCATION;
import static jooqcodegen.Functions.toNumber;

public enum BobPlugin {;

    public static void installPlugin(final Project project) {
        project.addCommand("jooq-codegen", "Generates JOOQ code", BobPlugin::generateJooqCode);
    }

    private static int generateJooqCode(final Project project, final Map<String, String> env, final String[] args) throws Exception {
        final CodegenArguments arguments = newCliParser(CodegenArguments::new)
            .onErrorPrintHelpAndExit()
            .onHelpPrintHelpAndExit()
            .parse(args);

        if (!isEnabled(project, arguments)) return 0;

        if (project.config.sourceDirectories.isEmpty()) {
            project.config.sourceDirectories.add(toGeneratedSourceDir(project, arguments));
            project.config.sourceDirectories.add("src" + File.separator + "main" + File.separator + "java");
        } else {
            project.config.sourceDirectories.add(0, arguments.outputDir);
        }

        String inputLocation = arguments.inputScript;
        if (!arguments.dontPreProcess) {
            final Path inputDir = toInputDir(project, arguments);
            final Path outputFile = toOutputSchemaFile(project, arguments);
            preProcessSql(inputDir, outputFile, arguments);
            inputLocation = outputFile.toAbsolutePath().toString();
        }

        final String packageName = toPackageName(project, arguments);
        final String outputDir = toGeneratedSourceDir(project, arguments);
        GenerationTool.generate(newConfiguration(packageName, inputLocation, outputDir, arguments));
        return 0;
    }

    private static boolean isEnabled(final Project project, final CodegenArguments arguments) {
        if (!DEFAULT_INPUT_LOCATION.equals(arguments.inputScript)) return true;
        return exists(project.parentDir.resolve(DEFAULT_INPUT_LOCATION));
    }

    private static Path toInputDir(final Project project, final CodegenArguments arguments) throws FileNotFoundException {
        if (isNullOrEmpty(arguments.inputScript)) throw new FileNotFoundException("Specified Migration SQL directory is empty");
        final Path inputDir = project.parentDir.resolve(arguments.inputScript);

        if (!exists(inputDir)) throw new FileNotFoundException("Specified Migration SQL directory does not exist");
        if (!isDirectory(inputDir)) throw new FileNotFoundException("Specified Migration SQL directory is not a directory");

        return inputDir;
    }
    private static Path toOutputSchemaFile(final Project project, final CodegenArguments arguments) throws IOException {
        final Path outputFile = project.getBuildTarget().resolve(arguments.generationSqlFile);
        if (exists(outputFile) && outputFile.toFile().delete())
            throw new IOException("Generation SQL file exists and cannot be deleted");
        if (!exists(outputFile.getParent()) && !outputFile.getParent().toFile().mkdirs())
            throw new IOException("Could not create parent directories for generation SQL file");
        return outputFile;
    }
    private static String toPackageName(final Project project, final CodegenArguments arguments) {
        return isNullOrEmpty(arguments.packageName) ? project.config.name + ".schema" : arguments.packageName;
    }
    private static String toGeneratedSourceDir(final Project project, final CodegenArguments arguments) {
        return project.parentDir.relativize(project.getBuildTarget().resolve(arguments.outputDir)).toString();
    }

    private static void preProcessSql(final Path inputDir, final Path outputFile, final CodegenArguments arguments) throws IOException {
        final String result = Arrays
                .stream(Files.list(inputDir)
                .filter(Files::isRegularFile)
                .sorted(comparingInt(o -> toNumber(o.getFileName().toString())))
                .flatMap(Functions::readAllLines)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.startsWith("--"))
                .map(s -> s+" ")
                .collect(joining())
                .split(";"))
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(trim -> !trim.isEmpty())
            .map(Functions::toJooqSafeStatement)
            .collect(joining());

        Files.writeString(outputFile, result, CREATE, TRUNCATE_EXISTING);
    }

    private static Configuration newConfiguration(final String packageName, final String inputLocation
            , final String outputDir, final CodegenArguments arguments) {

        return new Configuration()
            .withLogging(arguments.logging)
            .withGenerator(new Generator()
                .withGenerate(new Generate()
                    .withJavadoc(arguments.withJavadoc)
                    .withComments(arguments.withComments)
                    .withEmptySchemas(arguments.withEmptySchemas)
                    .withEmptyCatalogs(arguments.withEmptyCatalogs)
                    .withGeneratedAnnotation(arguments.withGeneratedAnnotation)
                    .withGlobalCatalogReferences(arguments.withGlobalCatalogReferences)
                    .withDaos(arguments.withDaos)
                    .withPojos(arguments.withPojos)
                    .withIndexes(arguments.withIndexes)
                    .withKeys(arguments.withKeys)
                    .withInstanceFields(arguments.withInstanceFields)
                    .withRecords(arguments.withRecords))
                .withDatabase(new Database()
                    .withName("org.jooq.meta.extensions.ddl.DDLDatabase")
                    .withProperties(new Property()
                        .withKey("scripts")
                        .withValue(inputLocation)))
                .withTarget(new Target()
                    .withPackageName(packageName)
                    .withDirectory(outputDir)));
    }

}
