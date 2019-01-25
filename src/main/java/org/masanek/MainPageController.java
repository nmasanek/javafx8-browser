package org.masanek;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Scanner;

public class MainPageController {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/538.19 (KHTML, like Gecko) Safari/538.19";


    public TabPane tabPane;
    public BorderPane window;
    @FXML
    private Button newTabButton;

    @FXML
    private void initialize() {

        newTabButton.setOnAction(action -> {
            this.createTab("https://google.com");
        });
    }

    private void createTab(final String url) {
        final TabController tabController = createTabController(url);

        // connect status bar
        tabController.progressBar.progressProperty().bind(tabController.webView.getEngine().getLoadWorker().progressProperty());
        tabController.statusLabel.textProperty().bind(tabController.webView.getEngine().getLoadWorker().messageProperty());

        // confirm dialog to close browser if the tab being closed is the last one
        tabController.tab.setOnCloseRequest(closed -> {
            if (tabPane.getTabs().size() - 1 <= 0) {
                final Alert alert = new Alert(
                        Alert.AlertType.WARNING,
                        "Closing the last tab will shut down the browser.",
                        ButtonType.OK, ButtonType.CANCEL
                );
                alert.showAndWait();
                if (alert.getResult() == ButtonType.OK) {
                    Platform.exit();
                } else {
                    closed.consume();
                    alert.close();
                }
            }
        });

        tabController.webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldValue, newValue) -> {
            switch (newValue) {
                case SUCCEEDED:
                    tabController.statusLabel.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.NAVICON));
                    break;
                case CANCELLED:
                case FAILED:
                    tabController.statusLabel.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.WARNING));
                    break;
            }
        });

        tabController.tab.textProperty().bind(tabController.webView.getEngine().titleProperty());

        tabController.webView.getEngine().locationProperty().addListener((observable, oldValue, newValue) -> {
            try {
                final String faviconUrl = String.format("http://www.google.com/s2/favicons?domain_url=%s", URLEncoder.encode(newValue, "UTF-8"));
                final Image favicon = new Image(faviconUrl, true);
                final ImageView iv = new ImageView(favicon);
                tabController.tab.setGraphic(iv);
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
        tabPane.getTabs().add(tabController.tab);

        tabController.webView.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case F5:
                    tabController.reload();
                    break;
                case F12:
                    final String firebugJs = new Scanner(MainPageController.class.getResourceAsStream("/js/initFirebug.js")).useDelimiter("\\A").next();
                    tabController.webView.getEngine().executeScript(firebugJs);
                    break;
            }
        });

        final SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
        selectionModel.select(tabController.tab);
    }

    public static TabController createTabController(final String url) {
        final FXMLLoader loader = new FXMLLoader(TabController.class.getResource("/fxml/tab.fxml"));
        try {
            loader.load();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        final TabController controller = loader.getController();
        controller.webView.getEngine().setUserAgent(USER_AGENT);
        controller.webView.getEngine().load(url);
        return controller;
    }
}