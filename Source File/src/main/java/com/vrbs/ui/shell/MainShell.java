package com.vrbs.ui.shell;

import com.vrbs.config.AppConfig;
import com.vrbs.model.SessionUser;
import com.vrbs.model.UserRole;
import com.vrbs.service.AuthService;
import com.vrbs.service.CabLocationService;
import com.vrbs.session.SessionManager;
import com.vrbs.session.SessionWatchdog;
import com.vrbs.state.ApplicationState;
import com.vrbs.ui.animations.FxAnimations;
import com.vrbs.ui.dashboard.RoleDashboards;
import com.vrbs.ui.popout.ProfileWindow;
import com.vrbs.ui.popout.SettingsWindow;
import com.vrbs.ui.support.SupportWidget;
import com.vrbs.ui.theme.ThemeManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Optional;

public final class MainShell {

    private enum NavPage { HOME, ABOUT, FEATURES, BOOK, RENT }

    private final Stage stage;
    private final ThemeManager theme = new ThemeManager();
    private final AuthService authService;
    private final CabLocationService cabService = new CabLocationService();
    private final SessionWatchdog watchdog;
    private final TextField searchField = new TextField();
    private final StackPane rootStack = new StackPane();
    private final BorderPane main = new BorderPane();
    private final StackPane centerHolder = new StackPane();
    private final VBox supportColumn = new VBox(8);
    private final SupportWidget supportWidget;
    private NavPage navPage = NavPage.HOME;

    public MainShell(Stage stage) {
        this.stage = stage;
        this.authService = new AuthService(u -> SessionManager.get().login(u));
        this.watchdog = new SessionWatchdog(msg -> Platform.runLater(() -> {
            new Alert(Alert.AlertType.WARNING, msg).showAndWait();
            refreshContent();
        }));
        SessionManager.get().setOnLogout(this::refreshContent);
        SessionManager.get().setOnLogin(u -> refreshContent());

        supportWidget = new SupportWidget(this::layoutSupport);
        buildLayout();
    }

    private void buildLayout() {
        main.setTop(buildNavBar());
        main.setCenter(centerHolder);
        supportColumn.getChildren().addAll(supportWidget.buildModeToolbar(), supportWidget.dockedPane());
        supportColumn.setPadding(new Insets(0, 16, 16, 16));
        main.setBottom(supportColumn);

        Region overlay = buildMaintenanceOverlay();
        overlay.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> ApplicationState.get().isMaintenanceMode()
                        && SessionManager.get().currentUser().map(u -> u.getRole() != UserRole.ADMIN).orElse(true),
                ApplicationState.get().maintenanceModeProperty(),
                SessionManager.get().userProperty()
        ));
        overlay.managedProperty().bind(overlay.visibleProperty());

        supportWidget.attachFloatingToStack(rootStack);
        rootStack.getChildren().addAll(main, overlay);
        refreshContent();
    }

    private Region buildMaintenanceOverlay() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("overlay-blocker");
        Label t = new Label("VRBS is under maintenance");
        t.getStyleClass().add("nav-brand");
        Label s = new Label("Please try again later. Administrators can sign in to restore service.");
        s.getStyleClass().add("text-muted");
        s.setWrapText(true);
        box.getChildren().addAll(t, s);
        return box;
    }

    private HBox buildNavBar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("nav-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Region logo = new Region();
        logo.getStyleClass().add("logo-placeholder");
        logo.setMinSize(44, 44);
        Label logoHint = new Label("LOGO");
        logoHint.setMouseTransparent(true);
        logoHint.setStyle("-fx-font-size: 9px;");
        StackPane logoStack = new StackPane(logo, logoHint);
        Runnable goHome = () -> {
            navPage = NavPage.HOME;
            refreshContent();
        };
        logoStack.setOnMouseClicked(e -> goHome.run());
        logoStack.setCursor(javafx.scene.Cursor.HAND);

        Label brand = new Label("VRBS");
        brand.getStyleClass().add("nav-brand");
        brand.setOnMouseClicked(e -> goHome.run());
        brand.setCursor(javafx.scene.Cursor.HAND);

        Button about = navButton("About", () -> showNav(NavPage.ABOUT));
        Button features = navButton("Features", () -> showNav(NavPage.FEATURES));
        Button book = navButton("Book", () -> showNav(NavPage.BOOK));
        Button rent = navButton("Rent", () -> showNav(NavPage.RENT));

        searchField.setPromptText("Search…");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(220);
        HBox.setHgrow(searchField, Priority.NEVER);
        searchField.setOnAction(e -> refreshContent());

        MenuItem profileItem = new MenuItem("Profile");
        profileItem.setOnAction(e -> SessionManager.get().currentUser().ifPresentOrElse(
                u -> ProfileWindow.open(stage, u, theme),
                () -> new Alert(Alert.AlertType.INFORMATION, "Please sign in to view your profile.").showAndWait()
        ));
        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setOnAction(e -> {
            Scene s = stage.getScene();
            if (s != null) {
                SettingsWindow.open(stage, authService, theme, s, this::refreshContent);
            }
        });
        MenuButton profileBtn = new MenuButton("Profile ▾");
        profileBtn.getItems().addAll(profileItem, settingsItem);
        profileBtn.getStyleClass().add("nav-profile");

        bar.getChildren().addAll(logoStack, brand, new Separator(), about, features, book, rent, searchField, profileBtn);
        return bar;
    }

    private Button navButton(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    private void showNav(NavPage page) {
        navPage = page;
        if (page == NavPage.BOOK || page == NavPage.RENT) {
            if (!SessionManager.get().isLoggedIn()) {
                showLoginDialog();
                return;
            }
            if (SessionManager.get().currentRoleOrGuest() != UserRole.USER) {
                new Alert(Alert.AlertType.INFORMATION, "Book and Rent are available to customer accounts.").showAndWait();
                return;
            }
        }
        refreshContent();
    }

    public Scene createScene() {
        Scene scene = new Scene(rootStack, AppConfig.windowDefaultWidth(), AppConfig.windowDefaultHeight());
        theme.tryLoadBundledFonts();
        theme.applyTo(scene);
        scene.getAccelerators().put(
                javafx.scene.input.KeyCombination.valueOf("SHORTCUT+L"),
                this::showLoginDialog
        );
        watchdog.bind(scene);
        return scene;
    }

    public void refreshContent() {
        centerHolder.getChildren().clear();
        SessionUser u = SessionManager.get().userProperty().get();
        if (u == null) {
            centerHolder.getChildren().add(switch (navPage) {
                case ABOUT -> buildAboutPage();
                case FEATURES -> featuresPage();
                default -> guestHome();
            });
        } else {
            String filter = searchField.getText() != null ? searchField.getText().trim() : "";
            if (u.getRole() == UserRole.USER && (navPage == NavPage.BOOK || navPage == NavPage.RENT)) {
                filter = navPage == NavPage.BOOK ? "book cab" : "rent vehicle";
            }
            centerHolder.getChildren().add(RoleDashboards.build(u, u.getRole(), cabService, stage, filter));
        }
        var node = centerHolder.getChildren().isEmpty() ? null : centerHolder.getChildren().get(0);
        if (node != null) {
            FxAnimations.fadeIn(node, Duration.millis(200)).play();
        }
        watchdog.touch();
        layoutSupport();
    }

    private void layoutSupport() {
        // Floating panel visibility handled inside SupportWidget; bottom dock shows when not floating
    }

    private ScrollPane simpleTextPage(String title, String text) {
        Label t = new Label(title);
        t.getStyleClass().add("nav-brand");
        Label body = new Label(text);
        body.setWrapText(true);
        body.getStyleClass().add("text-muted");
        VBox v = new VBox(12, t, body);
        v.setPadding(new Insets(24));
        ScrollPane sp = new ScrollPane(v);
        sp.setFitToWidth(true);
        return sp;
    }

    private ScrollPane buildAboutPage() {
        return simpleTextPage("About",
                "VRBS (Vehicle Rental and Booking System) is a desktop application for a small fleet operator. "
                        + "Customers rent vehicles by the hour or book cabs with assigned drivers. "
                        + "Suppliers manage fleet and settlements; admins oversee the whole network.");
    }

    private ScrollPane featuresPage() {
        return simpleTextPage("Features",
                "• Secure authentication with BCrypt\n"
                        + "• Hourly vehicle rental with cart (max 5) and peak pricing rules\n"
                        + "• Cab booking by fixed service areas and available drivers\n"
                        + "• Wallets, receipts (PDF), penalties, and refunds per policy\n"
                        + "• Role dashboards: User, Driver, Supplier, Admin\n"
                        + "• Customer care with instant answers and docked/floating support panel\n"
                        + "• Session timeout, maintenance mode, and suspension controls");
    }

    private VBox guestHome() {
        Label hero = new Label("Move your fleet with confidence");
        hero.getStyleClass().add("nav-brand");
        Label sub = new Label("Rent by the hour or book a cab — one desktop app for your company.");
        sub.getStyleClass().add("text-muted");
        sub.setWrapText(true);

        Button signIn = new Button("Sign in");
        signIn.getStyleClass().add("primary-button");
        signIn.setOnAction(e -> showLoginDialog());

        Button reg = new Button("Create account");
        reg.getStyleClass().add("secondary-button");
        reg.setOnAction(e -> showRegisterDialog());

        HBox actions = new HBox(12, signIn, reg);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(20, hero, sub, actions);
        box.setPadding(new Insets(48));
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private void showLoginDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sign in");
        dialog.initOwner(stage);

        TextField user = new TextField();
        user.setPromptText("Username");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Password");
        VBox form = new VBox(10, user, pass);
        form.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setOnShown(e -> Platform.runLater(user::requestFocus));

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            authService.login(user.getText(), pass.getText()).ifPresentOrElse(
                    err -> new Alert(Alert.AlertType.ERROR, err).showAndWait(),
                    this::refreshContent
            );
        }
    }

    private void showRegisterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create account");
        dialog.initOwner(stage);

        TextField user = new TextField();
        user.setPromptText("Username");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Password (min 6)");
        TextField email = new TextField();
        email.setPromptText("Email");
        TextField display = new TextField();
        display.setPromptText("Display name");
        ComboBox<UserRole> role = new ComboBox<>();
        role.getItems().addAll(UserRole.USER, UserRole.DRIVER, UserRole.SUPPLIER);
        role.getSelectionModel().selectFirst();

        VBox form = new VBox(10, user, pass, display, email, role);
        form.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Register", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            authService.register(user.getText(), pass.getText(), display.getText(), role.getValue(), email.getText())
                    .ifPresentOrElse(
                            err -> new Alert(Alert.AlertType.ERROR, err).showAndWait(),
                            () -> new Alert(Alert.AlertType.INFORMATION, "Account created — you can sign in.").showAndWait()
                    );
        }
    }
}
