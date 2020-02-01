package bobjooqcodegen;

import bobthebuilder.pojos.buildfile.Project;
import bobthebuilder.pojos.error.InvalidInput;
import bobthebuilder.pojos.internal.DescriptionAndInterface;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import static bobjooqcodegen.Functions.*;
import static bobthebuilder.services.Functions.isNullOrEmpty;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.StandardOpenOption.*;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.joining;
import static jcli.CliParserBuilder.newCliParser;

public enum Main {;

    public static void installPlugin(final Project project) {
        project.commands.put("jooq-codegen", new DescriptionAndInterface<>("Generates JOOQ code", (project1, environment, args) -> {
            final CodegenArguments arguments = newCliParser(CodegenArguments::new)
                .onErrorPrintHelpAndExit()
                .onHelpPrintHelpAndExit()
                .parse(args);

            if (project1.config.sourceDirectories.isEmpty()) {
                project1.config.sourceDirectories.add(arguments.outputDir);
                project1.config.sourceDirectories.add("src/main/java");
            } else {
                project1.config.sourceDirectories.add(0, arguments.outputDir);
            }

            if (!arguments.dontPreProcess) preProcessSql(project1, arguments);
            GenerationTool.generate(newConfiguration(project1, arguments));
        }));
    }

    private static void preProcessSql(final Project project, final CodegenArguments arguments) throws IOException, InvalidInput {
        final Path outputFile = checkValidOutputFile(project.getBuildTarget().resolve(arguments.generationSqlFile));

        if (!isDirectory(Paths.get(arguments.inputScript)))
            throw new InvalidInput("Migration input directory " + arguments.inputScript + " does not exist");

        final String result = Arrays
            .stream(listFiles(arguments.inputScript)
                .filter(File::isFile)
                .sorted(comparingInt(o -> toNumber(o.getName())))
                .flatMap(Functions::readAllLines)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.startsWith("--"))
                .map(s -> s+" ")
                .collect(joining())
                .split(";"))
            .filter(Objects::nonNull)
            .filter(s -> !s.trim().isEmpty())
            .map(String::trim)
            .map(Functions::toJooqSafeStatement)
            .collect(joining());

        Files.writeString(outputFile, result, CREATE, TRUNCATE_EXISTING);
    }

    private static Configuration newConfiguration(final Project project, final CodegenArguments arguments) {
        final String packageName
            = isNullOrEmpty(arguments.packageName)
            ? project.config.name + ".schema"
            : arguments.packageName;

        final String inputScript
            = !arguments.dontPreProcess
            ? project.getBuildTarget().resolve(arguments.generationSqlFile).toAbsolutePath().toString()
            : arguments.inputScript;

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
                    .withRecords(arguments.withRecords))
                .withDatabase(new Database()
                    .withName("org.jooq.meta.extensions.ddl.DDLDatabase")
                    .withProperties(new Property()
                        .withKey("scripts")
                        .withValue(inputScript)))
                .withTarget(new Target()
                    .withPackageName(packageName)
                    .withDirectory(arguments.outputDir)));
    }

}
