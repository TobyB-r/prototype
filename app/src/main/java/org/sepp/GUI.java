package org.sepp;

import java.io.File;
import java.io.IOException;
import javafx.application.Application;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class GUI extends Application {

  Context context = new Context();

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    MenuBar menuBar = new MenuBar();

    // Created menus at the top of GUI
    Menu fileMenu = new Menu("File");
    Menu configMenu = new Menu("Config");
    Menu tasksMenu = new Menu("Tasks");
    Menu helpMenu = new Menu("Help");

    // Menu items for file Menu
    MenuItem run = new MenuItem("Run...");
    MenuItem directory = new MenuItem("Set directory");

    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setInitialDirectory(new File("src"));

    directory.setOnAction(
        e -> {
          File selectedDirectory = directoryChooser.showDialog(primaryStage);
          context.runDirectory = selectedDirectory;
          // System.out.println(selectedDirectory.getAbsolutePath());
        });

    // Retrieves all file menu items into the fileMenu
    fileMenu.getItems().addAll(run, directory);

    // Menu items for configMenu
    MenuItem newConfig = new MenuItem("Create new config");
    newConfig.setOnAction(e -> createConfigPopup());
    Menu loadConfig = new Menu("Load config");
    MenuItem save = new MenuItem("Save");
    MenuItem saveAs = new MenuItem("Save As...");
    MenuItem close = new MenuItem("Close");
    MenuItem pref = new MenuItem("Preferences...");
    MenuItem quit = new MenuItem("Quit");

    loadConfig.getItems().addAll(context.getConfigFileNames().stream().map(str ->{
      MenuItem citem = new MenuItem(str);
      citem.setOnAction(e->{
        Config c;
        try{
          context.config = Config.load(str);
        } catch (IOException ex) {
          Alert alert = new Alert(Alert.AlertType.ERROR);
          alert.setTitle("Failed to load config \""+str+"\"");
          alert.setHeaderText(null);
          alert.setContentText(ex.getMessage());
          alert.showAndWait();
        }
      });
      return citem;
    }).toList());
    loadConfig.getItems().add(new MenuItem("Browse..."));

    // Retrieves all config menu items into the configMenu
    configMenu
        .getItems()
        .addAll(newConfig, loadConfig, save, saveAs, new SeparatorMenuItem(), pref, quit);

    // Menu items for taskMenu
    MenuItem newTask = new MenuItem("New");
    newTask.setOnAction(e -> {
      if (context.config == null){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("No config selected!");
        alert.setHeaderText(null);
        alert.setContentText("Please select a config with: Config -> Load or create a new one");
        alert.showAndWait();
      } else {
      Task t = newTaskPopup();
      if (t != null) {
        System.out.println(t.toString());
      }else {
        System.out.println("we got null");
      }}
    });
    MenuItem delete = new MenuItem("Delete");

    // Retrieves all task items into taskMenu
    tasksMenu.getItems().addAll(newTask, delete);

    // Gathers all the menus to the menuBar
    menuBar.getMenus().addAll(fileMenu, configMenu, tasksMenu, helpMenu);

    // Creates the original Layout of GUI
    BorderPane layout = new BorderPane();

    // root acts as the main background for list view and the output
    AnchorPane root = new AnchorPane();

    // Splits the listView and VBox
    HBox hBox = new HBox();

    // All items in the listView
    ListView<String> listView = new ListView<>();

    // Where the output will be (text and project name)
    VBox vBox = new VBox();
    vBox.setAlignment(Pos.TOP_CENTER);

    // Current project name
    Label projectName = new Label("Project name");

    // Where the text will be displayed
    AnchorPane miniPane = new AnchorPane();

    // output text
    Text output = new Text("1. Task name");
    AnchorPane.setTopAnchor(output, 0.0);
    AnchorPane.setLeftAnchor(output, 0.0);

    // Puts the text into the Anchor pane
    miniPane.getChildren().add(output);

    // Puts the project name and anchor pane (which holds the text) into VBOX
    vBox.getChildren().addAll(projectName, miniPane);

    // Separates the listView and VBOX
    hBox.getChildren().addAll(listView, vBox);

    // Finally puts everything into a single anchorpane
    root.getChildren().add(hBox);

    AnchorPane.setTopAnchor(hBox, 0.0);
    AnchorPane.setBottomAnchor(hBox, 0.0);
    AnchorPane.setRightAnchor(hBox, 0.0);
    AnchorPane.setLeftAnchor(hBox, 0.0);

    // Formats where the menu bar and split should go inside GUI
    layout.setCenter(root);
    layout.setTop(menuBar);

    // Builds scene
    Scene scene = new Scene(layout, 1024, 768);
    primaryStage.setTitle("Prototype");
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  private Task newTaskPopup() {
    Stage newTaskPopup = new Stage();
    newTaskPopup.setTitle("New task");
    Insets padding = new Insets(10,10,10,10);

    TextField taskNameField = new TextField();
    taskNameField.setPromptText("Enter the task name");

    ComboBox<String> options = new ComboBox<>();
    options.getItems().addAll("Compile", "Custom");
    options.setValue("Custom");

    TextArea input = new TextArea();
    input.setPromptText("Enter shell script");
    input.setFont(Font.font("Monospaced"));
    input.setMaxHeight(Double.MAX_VALUE);
    input.setMaxWidth(Double.MAX_VALUE);
    GridPane.setVgrow(input,Priority.ALWAYS);


    Button okButton = new Button("Ok");
    GridPane.setMargin(okButton,padding);
    Task[] task = {null};
    okButton.setOnAction(e -> {
      String taskName = taskNameField.getText();
      if(taskName.isEmpty()){
        taskName = "Untitled Task";
      }
      String type = options.getValue();
      String shellScript = input.getText();

      task[0] = new Task(taskName, Task.TaskType.fromString(type),shellScript);
      newTaskPopup.close();
    });



    GridPane layout = new GridPane();
    Label tlabel = new Label("Task name:");
    tlabel.setPadding(padding);
    Label typelabel = new Label("Task Type:");
    typelabel.setPadding(padding);
    Label shLabel = new Label("sh script:");
    shLabel.setPadding(padding);


    layout.add(tlabel, 0,0);
    layout.add(taskNameField,1,0);
    layout.add(typelabel, 0,1);
    layout.add(options, 1,1);
    layout.add(shLabel, 0,2);
    layout.add(input,1,2);
    layout.add(okButton,0,3);

    Scene taskPopupScene = new Scene(layout,420,200);
    newTaskPopup.setScene(taskPopupScene);
    newTaskPopup.setMinWidth(420);
    newTaskPopup.setWidth(420);
    newTaskPopup.setMinHeight(200);
    newTaskPopup.setHeight(200);
    newTaskPopup.showAndWait();

    return task[0];
  }

  private void createConfigPopup() {
    Stage createConfigPopup = new Stage();
    createConfigPopup.setTitle("Create config");

    TextField configNameField = new TextField();
    configNameField.setPromptText("Enter config name");

    TextArea compileScriptField = new TextArea();
    compileScriptField.setPromptText("Enter compile script");

    Button okButton = new Button("Ok");

    Label nameLabel = new Label("Config Name");
    nameLabel.setPadding(new Insets(10, 10, 10, 10));

    Label compileScriptLabel = new Label("Compile Script");
    compileScriptLabel.setPadding(new Insets(10, 10, 10, 10));

    GridPane layout = new GridPane();
    layout.add(nameLabel, 0, 0);
    layout.add(configNameField, 1, 0);
    layout.add(compileScriptLabel, 0, 1);
    layout.add(compileScriptField, 1, 1);
    layout.add(okButton, 1, 2);
    GridPane.setValignment(compileScriptLabel, VPos.TOP);
    GridPane.setValignment(okButton, VPos.CENTER);
    GridPane.setHalignment(okButton, HPos.RIGHT);

    Scene configPopupScene = new Scene(layout, 600, 250);
    createConfigPopup.setScene(configPopupScene);
    createConfigPopup.show();
  }
}
