package org.masanek;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class BrowserController {

    // TODO: Settings
    private static final String START_PAGE = "https://google.com";
    private static final String NEW_TAB_PAGE = "https://google.com";

    public TabPane tabPane;
    public BorderPane window;
    public ToolBar sideBar;
    public MenuButton globalHistoryButton;

    @FXML
    private Button addTabButton;

    @FXML
    private void initialize() {
        this.newTab(START_PAGE); // create initial tab when browser first gets opened
        addTabButton.setOnAction(action -> this.newTab(NEW_TAB_PAGE));
    }

    public void newTab(final String url) {
        final TabController tabController = createTabController();
        tabController.webView.getEngine().load(url);
        tabController.setBrowserController(this);
        tabPane.getTabs().add(tabPane.getTabs().size() - 1, tabController.tab); // add before add tab "button"
        tabPane.getSelectionModel().select(tabController.tab);
    }

    public static TabController createTabController() {
        final FXMLLoader loader = new FXMLLoader(TabController.class.getResource("/fxml/tab.fxml"));
        try {
            loader.load();
            return loader.getController();
        } catch (final IOException e) {
            throw new AssertionError("Why should this ever happen?");
        }
    }

    @FXML
    public void openSettingsPopup() {

    }
}