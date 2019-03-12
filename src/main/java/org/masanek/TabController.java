package org.masanek;

import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding;
import impl.org.controlsfx.autocompletion.SuggestionProvider;
import javafx.application.Platform;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.masanek.bookmarks.BookmarkWizardPane;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class TabController {

    private static final String GOOGLE_FAVICON_PROVIDER_URL = "https://www.google.com/s2/favicons?domain_url=%s";
    public static final String HOMEPAGE = "https://google.com";
    private static final String ERROR_PAGE = "/html/404.html";

    private final SuggestionProvider<String> urlSuggestionProvider = SuggestionProvider.create(Collections.emptyList());
    private final SuggestionProvider<String> googleSuggestionProvider = SuggestionProvider.create(Collections.emptyList());

    public CustomTextField urlTextField;
    public CustomTextField googleTextField;
    public ToolBar toolbar;
    public Tab tab;
    public WebView webView;
    public Button previousButton;
    public Button nextButton;
    public Label statusLabel;
    public ProgressBar progressBar;
    public ToolBar bookmarksBar;

    private BrowserController browserController;

    @FXML
    private void initialize() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // confirm dialog to close browser if the tab being closed is the last one
        tab.setOnCloseRequest(closed -> {
            if (tab.getTabPane().getTabs().size() - 2 <= 0) {
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

        final WebEngine webEngine = webView.getEngine();

        // connect status label
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldValue, newValue) -> {
            switch (newValue) {
                case SUCCEEDED:
                    statusLabel.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.NAVICON));
                    break;
                case CANCELLED:
                case FAILED:
                    statusLabel.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.WARNING));
                    break;
            }
        });

        // connect status progress bar
        progressBar.progressProperty().bind(webEngine.getLoadWorker().progressProperty());
        statusLabel.textProperty().bind(webEngine.getLoadWorker().messageProperty());

        final AutoCompletionTextFieldBinding<String> binding = new AutoCompletionTextFieldBinding<>(urlTextField, urlSuggestionProvider);
        binding.setPrefWidth(urlTextField.getPrefWidth());
        binding.setMinWidth(urlTextField.getMinWidth());
        binding.setMaxWidth(urlTextField.getMaxWidth());

        setupClearButtonField(urlTextField);
        setupClearButtonField(googleTextField);

        this.patchWebViewTrustManager();
        this.patchWebViewContextMenu();

        urlTextField.textProperty().addListener((observable, oldValue, newValue) -> this.suggestUrls());

        // set url field and enable/disable history buttons on page change
        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            final WebHistory history = webEngine.getHistory();
            final int currentIndex = history.getCurrentIndex();
            previousButton.setDisable(currentIndex == 1);
            urlTextField.setText(newValue);
        });

        // display error page on exception
        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldValue, newValue) -> {
            new ExceptionDialog(newValue).showAndWait();
            final URL page404 = BrowserController.class.getResource(ERROR_PAGE);
            webEngine.load(page404.toExternalForm());
        });

        // set favicon
        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            try {
                final String faviconUrl = String.format(GOOGLE_FAVICON_PROVIDER_URL, URLEncoder.encode(newValue, "UTF-8"));
                final Image favicon = new Image(faviconUrl, true);
                final ImageView iv = new ImageView(favicon);
                tab.setGraphic(iv);
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });

        // set tab title and mouseover tooltip
        tab.textProperty().bind(webEngine.titleProperty());
        tab.tooltipProperty().bind(new ObjectBinding<Tooltip>() {
            @Override
            protected Tooltip computeValue() {
                return new Tooltip(webEngine.getTitle());
            }
        });

        // history listener
        webEngine.getHistory().getEntries().addListener((ListChangeListener<WebHistory.Entry>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(a -> {
                    });
                } else if (c.wasRemoved()) {
                    c.getRemoved().forEach(r -> {
                    });
                }
            }
        });

        webView.setOnKeyPressed(this::setupKeybinds);
    }

    private static void setupClearButtonField(final CustomTextField customTextField) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method m = TextFields.class.getDeclaredMethod("setupClearButtonField", TextField.class, ObjectProperty.class);
        m.setAccessible(true);
        m.invoke(null, customTextField, customTextField.rightProperty());
    }

    private void suggestUrls() {
        // TODO: Provide suggestions from all tabs, not just current one
        urlSuggestionProvider.clearSuggestions();
        if (urlTextField.getText().isEmpty()) {
            final List<String> suggestions = webView.getEngine().getHistory().getEntries().stream()
                    .limit(10)
                    .sorted((o1, o2) -> o2.getLastVisitedDate().compareTo(o1.getLastVisitedDate()))
                    .map(WebHistory.Entry::getUrl)
                    .collect(Collectors.toList());
            urlSuggestionProvider.addPossibleSuggestions(suggestions);
        } else {
            urlSuggestionProvider.addPossibleSuggestions(webView.getEngine().getHistory().getEntries().stream()
                    .map(WebHistory.Entry::getUrl)
                    .filter(url -> url.toLowerCase().contains(urlTextField.getText().toLowerCase()))
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Disables SSL certificate validation.
     * <p/>
     * <a href="https://stackoverflow.com/questions/22605701/javafx-webview-not-working-using-a-untrusted-ssl-certificate">Stack Overflow</a>
     */
    private void patchWebViewTrustManager() {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
                    }

                    @Override
                    public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (final GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private void setupKeybinds(final KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case F5:
                this.reload();
                break;
            case F12:
                final String firebugJs = new Scanner(BrowserController.class.getResourceAsStream("/js/initFirebug.js")).useDelimiter("\\A").next();
                webView.getEngine().executeScript(firebugJs);
                break;
        }
    }

    @FXML
    public void reload() {
        webView.getEngine().reload();
    }

    @FXML
    public void loadPrevious() {
        nextButton.setDisable(false);
        final WebHistory history = webView.getEngine().getHistory();
        final ObservableList<WebHistory.Entry> entryList = history.getEntries();
        final int currentIndex = history.getCurrentIndex();

        Platform.runLater(() -> history.go(entryList.size() > 1 && currentIndex > 0 ? -1 : 0));
    }

    @FXML
    public void loadNext() {
        final WebHistory history = webView.getEngine().getHistory();
        final ObservableList<WebHistory.Entry> entryList = history.getEntries();
        final int currentIndex = history.getCurrentIndex();

        // TODO disable next button if there is no fullHistory
        Platform.runLater(() -> history.go(entryList.size() > 1 && currentIndex < entryList.size() - 1 ? 1 : 0));
    }

    @FXML
    public void home() {
        webView.getEngine().load(HOMEPAGE);
    }

    @FXML
    public void onUrlField() throws MalformedURLException {
        final URL url = this.convertToValidUrl(urlTextField.getText());
        webView.getEngine().load(url.toString());
    }

    private URL convertToValidUrl(final String url) throws MalformedURLException {
        final URI uri = URI.create(url);
        if (uri.getScheme() != null) {
            return uri.toURL();
        } else {
            return new URL("https://" + url);
        }
    }

    public void showProgressInfo() {
        final PopOver popOver = new PopOver();
        popOver.setContentNode(new Label("Hey"));
        popOver.show(progressBar);
    }

    private void patchWebViewContextMenu() {
        webView.setContextMenuEnabled(false);

        final MenuItem reload = new MenuItem("Reload");
        reload.setOnAction(e -> this.reload());
        final MenuItem goBack = new MenuItem("Go back");
        goBack.setOnAction(e -> this.loadPrevious());
        final MenuItem next = new MenuItem("Next");
        next.setOnAction(e -> this.loadNext());

        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(reload, goBack, next);

        webView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(webView, e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }

    @FXML
    public void closeTab() {
        tab.getTabPane().getTabs().remove(tab);
    }

    @FXML
    public void openCreateBookmarkDialog() {
        // Create pages. Here for simplicity we just create and instance of WizardPane.
        final BookmarkWizardPane defaultPage = new BookmarkWizardPane();
        final GridPane flowPane = new GridPane();
        final TextField nameField = new TextField();
        nameField.setId("name");
        flowPane.add(nameField, 0, 0);
        defaultPage.setContent(nameField);

        final BookmarkWizardPane advancedPage = new BookmarkWizardPane();
        final GridPane advancedPane = new GridPane();
        final ColorPicker colorPicker = new ColorPicker();
        colorPicker.setId("color");
        advancedPane.add(colorPicker, 0, 0);
        advancedPage.setContent(advancedPane);

        // create wizard
        final Wizard wizard = new Wizard();

        // create and assign the flow
        wizard.setFlow(new Wizard.LinearFlow(defaultPage, advancedPage, advancedPage));

        // show wizard and wait for response
        wizard.showAndWait().ifPresent(result -> {
            if (result == ButtonType.FINISH) {
                System.out.println("Wizard finished, settings: " + wizard.getSettings());
            }
        });

        System.out.println("Open dialog");
    }

    public void setBrowserController(final BrowserController browserController) {
        this.browserController = browserController;
    }
}
