package org.masanek;

import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding;
import impl.org.controlsfx.autocompletion.SuggestionProvider;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.stream.Collectors;

public class TabController {

    public static final String HOMEPAGE = "https://google.com";
    private static final String ERROR_PAGE = "/html/404.html";

    private final SuggestionProvider<String> urlSuggestionProvider = SuggestionProvider.create(Collections.emptyList());

    public CustomTextField urlTextField;
    public CustomTextField googleTextField;
    public ToolBar toolbar;
    public Tab tab;
    public WebView webView;
    public Button previousButton;
    public Button nextButton;
    public Label statusLabel;
    public ProgressBar progressBar;

    @FXML
    private void initialize() throws Exception {
        final AutoCompletionTextFieldBinding<String> binding = new AutoCompletionTextFieldBinding<>(urlTextField, urlSuggestionProvider);
        binding.setPrefWidth(urlTextField.getPrefWidth());
        binding.setMinWidth(urlTextField.getMinWidth());
        binding.setMaxWidth(urlTextField.getMaxWidth());

        setupClearButtonField(urlTextField);
        setupClearButtonField(googleTextField);

        this.patchWebViewTrustManager();
        this.patchWebViewContextMenu();

        urlTextField.textProperty().addListener((observable, oldValue, newValue) -> this.suggestUrls());

        webView.getEngine().locationProperty().addListener((observable, oldValue, newValue) -> {
            final WebHistory history = webView.getEngine().getHistory();
            final int currentIndex = history.getCurrentIndex();
            previousButton.setDisable(currentIndex == 1);

            urlTextField.setText(newValue);
        });

        webView.getEngine().getLoadWorker().exceptionProperty().addListener((obs, oldValue, newValue) -> {
            final URL page404 = BrowserController.class.getResource(ERROR_PAGE);
            webView.getEngine().load(page404.toExternalForm());
        });
    }

    private void suggestUrls() {
        // TODO: Provide suggestions from all tabs, not just current one
        urlSuggestionProvider.clearSuggestions();
        if (urlTextField.getText().isEmpty()) {
            urlSuggestionProvider.addPossibleSuggestions(webView.getEngine().getHistory().getEntries().stream().limit(10).map(WebHistory.Entry::getUrl).collect(Collectors.toList()));
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
}
