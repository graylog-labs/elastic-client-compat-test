package org.graylog.labs.esclient;


import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.CliBuilder;
import com.github.rvesse.airline.help.Help;
import com.github.rvesse.airline.parser.ParseResult;
import com.github.rvesse.airline.parser.errors.handlers.CollectAll;

@SuppressWarnings("unchecked")
public class Main {

  public static void main(String[] args) {
    final CliBuilder<Runnable> builder = Cli.<Runnable>builder("esclient-test")
        .withDefaultCommand(RequestRunner.class)
        .withCommands(Help.class, RequestRunner.class);
    builder.withParser()
        .withErrorHandler(new CollectAll());

    final Cli<Runnable> cli = builder.build();
    final ParseResult<Runnable> parseResult = cli.parseWithResult(args);
    if (parseResult.wasSuccessful()) {
      parseResult.getCommand().run();
    } else {
      parseResult.getErrors().forEach(e -> System.err.println(e.getMessage()));
      System.err.println();
      final String failedCommandName = parseResult.getState().getCommand().getName();
      // let's hope that help never fails
      cli.parse("help",failedCommandName).run();
      System.exit(1);
    }
  }

}
