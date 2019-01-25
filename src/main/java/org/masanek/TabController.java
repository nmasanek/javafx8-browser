package org.masanek;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import java.lang.reflect.Method;
import java.net.URL;

public class TabController {

    public static final String HOMEPAGE = "https://google.com";
    private static final String ERROR_PAGE = "/html/404.html";

    @FXML
    public CustomTextField urlTextField;

    @FXML
    public CustomTextField googleTextField;

    @FXML
    public ToolBar toolbar;

    @FXML
    public Tab tab;

    @FXML
    public WebView webView;
    @FXML
    public Button previousButton;
    @FXML
    public Button nextButton;
    public Button reloadButton;
    public Label statusLabel;
    public ProgressBar progressBar;

    AutoCompletionBinding<String> currentBinding;

    @FXML
    private void initialize() throws Exception {
        setupClearButtonField(urlTextField);
        setupClearButtonField(googleTextField);

        webView.setContextMenuEnabled(false);
        this.createContextMenu(webView);

        webView.getEngine().locationProperty().addListener((observable, oldValue, newValue) -> {
            if (!webView.getEngine().getHistory().getEntries().isEmpty()) {
                previousButton.setDisable(false);
            }
            urlTextField.setText(newValue);
        });

        webView.getEngine().getLoadWorker().exceptionProperty().addListener((obs, oldValue, newValue) -> {
            final URL page404 = MainPageController.class.getResource(ERROR_PAGE);
            webView.getEngine().load(page404.toExternalForm());
        });
    }

    private static void setupClearButtonField(final CustomTextField customTextField) throws Exception {
        final Method m = TextFields.class.getDeclaredMethod("setupClearButtonField", TextField.class, ObjectProperty.class);
        m.setAccessible(true);
        m.invoke(null, customTextField, customTextField.rightProperty());
    }

    @FXML
    public void reload() {
        webView.getEngine().reload();
    }

    @FXML
    public void loadPrevious() {
        final WebEngine engine = webView.getEngine();
        final WebHistory.Entry entry = engine.getHistory().getEntries().get(engine.getHistory().getCurrentIndex() - 1);
        engine.load(entry.getUrl());
    }

    @FXML
    public void loadNext() {

    }

    @FXML
    public void home() {
        webView.getEngine().load(HOMEPAGE);
    }

    @FXML
    public void onUrlField() {
        webView.getEngine().load(this.convertToValidUrl(urlTextField.getText()));
    }

    private String convertToValidUrl(final String url) {
        return url.startsWith("http://") || url.startsWith("https://") ? url : "https://" + url;
    }

    public void showProgressInfo() {
        final PopOver popOver = new PopOver();
        popOver.setContentNode(new Label("Hey"));
        popOver.show(progressBar);
    }

    private void createContextMenu(final WebView webView) {
        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem reload = new MenuItem("Reload");
        reload.setOnAction(e -> webView.getEngine().reload());
        final MenuItem savePage = new MenuItem("Save Page");
        savePage.setOnAction(e -> System.out.println("Save page..."));
        final MenuItem hideImages = new MenuItem("Hide Images");
        hideImages.setOnAction(e -> System.out.println("Hide Images..."));
        contextMenu.getItems().addAll(reload, savePage, hideImages);

        webView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(webView, e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }
}
