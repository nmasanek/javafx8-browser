package org.masanek;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.File;

public class Main extends Application {

    public static final String DARK_THEME_LOCATION= "/css/modena_dark.css";

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/browser.fxml"));
        final BorderPane page = loader.load();

        final Scene scene = new Scene(page);
        scene.getStylesheets().add("/css/custom.css");
        scene.getStylesheets().add(DARK_THEME_LOCATION);

        final File dir = new File(new JFileChooser().getFileSystemView().getDefaultDirectory() + "/Internot Explorer");
        dir.mkdirs();
        final File file = new File(dir.toPath().toAbsolutePath() + "/test.txt");
        file.createNewFile();

        primaryStage.setTitle("Internot Explorer - Der einzig gute Brauser");
        primaryStage.getIcons().add(new Image("http://icons.iconarchive.com/icons/dakirby309/simply-styled/128/Internet-Explorer-icon.png"));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(final String[] args) {
        Main.launch();
    }
}
