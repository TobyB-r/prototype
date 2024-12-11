/*
 * This source file was generated by the Gradle 'init' task
 */

package org.sepp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.apache.commons.cli.*;

public class Cli {

  private static Cli INSTANCE = null;

  public CommandLineParser parser;
  public Options options;
  public HelpFormatter hFormatter;

  public static synchronized Cli getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new Cli();
    }
    return INSTANCE;
  }

  public Cli() {
    this.parser = new DefaultParser();
    this.options = new Options();
    this.options.addOption("i", "interactive", false, "Run interactive mode");
    this.options.addOption("h", "help", false, "Prints this message");
    this.options.addOption("v", "test", false, "this is a test");
    this.options.addOption("t", "title", true, "Set title of configuration");
    this.options.addOption("r", "run", true, "Directory to run in");
    this.options.addOption("li", "list", false, "list config files");
    this.options.addOption(
        Option.builder("a")
            .longOpt("add-task")
            .hasArgs()
            .numberOfArgs(3)
            .desc(
                "Task to add <name> <type> <path-to-shell-script>. Shell script path is not store,"
                    + " only the contents of the file")
            .build());
    this.options.addOption("l", "load", true, "Configuration to load");
    this.options.addOption("c", "create", false, "Create configuration");
    this.options.addOption(
        Option.builder("d")
            .longOpt("diff")
            .numberOfArgs(2)
            .desc("Diff two files <skeleton-path> <submission-path>")
            .build());
    this.hFormatter = new HelpFormatter();
  }

  public CommandLine parse(String[] args) throws ParseException {
    return this.parser.parse(this.options, args);
  }

  public void printHelp() {
    hFormatter.printHelp("utility-name", options);
  }

  public static void runCli(String[] args) {
    // this is the config we are goin to work with
    Context context = new Context();
    // get our Cli singleton
    Cli cli = Cli.getInstance();
    // flag to check if we need to save our config
    Boolean saveConfig = false;

    try {
      CommandLine line = cli.parse(args);
      if (line.hasOption("interactive")) {
        InteractiveMode im = new InteractiveMode();
        im.start();
        return;
      }

      if (line.hasOption("help")) {
        cli.printHelp();
        return;
      }

      if (line.hasOption("li")) {
        list();
        return;
      }

      // 2. Diff option
      if (line.hasOption("diff")) {
        String[] diffArgs = line.getOptionValues("diff");
        diffHandler(diffArgs);
        return;
      }

      if (!line.hasOption("load") && !line.hasOption("create")) {
        System.err.println("No config provided, see --help");
        System.exit(1);
      }

      // attempt to get our config
      if (line.hasOption("load")) {
        context.config = loadConfig(line.getOptionValue("load"));
      } else if (line.hasOption("create")) {
        createConfig(context);
      } else {
        // we need a configuration, otherwise nothing can be done
        System.err.println("No config provided, see --help");
        System.exit(1);
      }

      // adding tasks
      if (line.hasOption("add-task")) {
        String[] taskstr = line.getOptionValues("add-task");
        addTask(context, taskstr);
      }

      // allow changing titles
      if (line.hasOption("title")) {
        String title = line.getOptionValue("title");
        setTitle(context, title);
      }

      if (line.hasOption("run")) {
        String directory = line.getOptionValue("run");
        runConfig(context, directory);
      }

    } catch (ParseException e) {
      System.err.println("Parsing failed.  Reason: " + e.getMessage());
      System.exit(1);
    }

    if (context.saveConfig) {
      System.out.println("Saved config");
      context.config.save(true);
    }

    System.out.println("Finish run");
  }

  public static void list() {
    String[] names = Config.configsPath.list();
    if (names.length > 0) {
      System.out.println("Configs:");
    } else {
      System.out.println("No configs");
    }
    var filtered = Arrays.stream(names).filter(str -> str.endsWith(".toml"));
    filtered.forEach(str -> System.out.println("  - " + str.substring(0, str.length() - 5)));
  }

  public static void diffHandler(String[] diffArgs) {
    if (diffArgs.length != 2) {
      System.err.println("Diff command requires two file paths");
      System.exit(1);
    }

    try {
      if (!DiffCommand.validDiffArgs(diffArgs[0], diffArgs[1])) {
        System.exit(1);
      }
      DiffCommand.printDiff(diffArgs[0], diffArgs[1]);
    } catch (IOException e) {
      System.err.println("Failed to perform diff: " + e.getMessage());
      System.exit(1);
    }
  }

  public static Config loadConfig(String filepath) {
    try {
      Config c= Config.load(filepath);
      System.out.println("Loaded config \""+c.name+"\"");
      return c;
    } catch (Exception e) {
      System.err.println("Could not load config.\n\tError: " + e.getMessage());
      return null;
    }
  }

  public static void addTask(Context context, String[] taskstr) {
    if(context.config == null){
      System.out.println("No config provided");
      return;
    }
    // task needs 3 arguments
    if (taskstr.length < 3) {
      System.err.println("Invalid task, see --help");
      System.out.println(
          "argument length: " + taskstr.length + "\nArguments: " + Arrays.toString(taskstr));
      System.exit(1);
    }

    // Verify that file exists and is a file, this is scoped to ensure that shfile
    // isn't
    // accessed anywhere else
    {
      File shfile = new File(taskstr[2]);
      if (!shfile.isFile()) {
        System.err.println("Invalid shell file");
        System.exit(1);
      }
    }

    // try to read contents of file at path
    String sh;

    try {
      Path path = Paths.get(taskstr[2]);
      sh = Files.readString(path);
    } catch (Exception e) {
      System.err.println("Failed to read " + taskstr[1] + " \nError: " + e.getMessage());
      System.exit(1);
      return; // this line is here because java compiler assumes sh may be unitiliazed
    }

    // determine type, will default to custom
    Task.TaskType type = Task.parseType(taskstr[1]);

    context.config.addTask(new Task(taskstr[0], type, sh));

    // ensure config is updated
    context.saveConfig = true;
    System.out.println("Created new task \"" + taskstr[0] + "\"");
  }

  public static void runConfig(Context context, String directory) {
    if (context.config == null) {
      System.out.println("No config provided");
      return;
    }
    try {
      context.config.run(directory);
      System.out.println("Finished running");
    } catch (Exception e) {
      System.err.println("Failed to run config, Error:\n\t" + e.getMessage());
    }
  }

  public static void createConfig(Context context) {
    context.config = new Config();
    System.out.println("Created new config \""+context.config.name +"\"");
    context.saveConfig = true;
  }

  public static void setTitle(Context context, String title) {
    if (context.config == null) {
      System.out.println("No config provided");
      return;
    }
    context.config.name = title;
    System.out.println("Set title to \"" +title + "\"");
    context.saveConfig = true;
  }
}
