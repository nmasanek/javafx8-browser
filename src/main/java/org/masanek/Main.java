package org.masanek;

import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {


    @Override
    public void start(final Stage stage) throws Exception {
        SvgImageLoaderFactory.install();
        final FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/browser.fxml"));
        loader.setResources(ResourceBundle.getBundle("labels.labels", Locale.GERMAN));
        final BorderPane page = loader.load();

        final Scene scene = new Scene(page);

        stage.setMaximized(true);
        stage.setTitle("Internot Explorer - Der einzig gute Brauser");
        stage.getIcons().add(new Image("http://icons.iconarchive.com/icons/dakirby309/simply-styled/128/Internet-Explorer-icon.png"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(final String[] args) {
        Main.launch();
    }
}
