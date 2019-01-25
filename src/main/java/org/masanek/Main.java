package org.masanek;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;

import javax.swing.*;
import java.io.File;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/MainPageController.fxml"));
        BorderPane page = loader.load();

        Scene scene = new Scene(page);
        scene.getStylesheets().add("/css/custom.css");

        File dir = new File(new JFileChooser().getFileSystemView().getDefaultDirectory() + "/Internot Explorer");
        dir.mkdirs();
        File file = new File(dir.toPath().toAbsolutePath() + "/test.txt");
        file.createNewFile();

        primaryStage.setTitle("Internot Explorer - Der einzig gute Brauser");
        primaryStage.getIcons().add(new Image("http://icons.iconarchive.com/icons/dakirby309/simply-styled/128/Internet-Explorer-icon.png"));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Main.launch();
    }
}
