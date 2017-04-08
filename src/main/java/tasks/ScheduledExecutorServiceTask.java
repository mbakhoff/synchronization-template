package tasks;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ScheduledExecutorServiceTask extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // TODO:
        // create a ScheduledExecutorService with exactly 1 thread
        // schedule annoyWithClippy(primaryStage) to repeat every 2sec
        // schedule annoyWithWindowsUpdate(primaryStage) to repeat every 3sec

        primaryStage.setOnCloseRequest(e -> {
            // TODO shut down the executor
            Platform.exit();
        });

        Button stopClippy = new Button("Stop clippy!");
        stopClippy.setOnAction(e -> {
            // TODO cancel the clippy spam
        });

        Button stopShutdown = new Button("Stop update!");
        stopShutdown.setOnAction(e -> {
            // TODO cancel the windows update spam
        });

        primaryStage.setScene(new Scene(new Group(new FlowPane(stopClippy, stopShutdown))));
        primaryStage.show();
    }

    private void annoyWithClippy(Stage primaryStage) {
        annoyWithPopup(primaryStage, "clippy.jpg");
    }

    private void annoyWithWindowsUpdate(Stage primaryStage) {
        annoyWithPopup(primaryStage, "restart.jpg");
    }

    private void annoyWithPopup(Window window, String image) {
        Platform.runLater(() -> {
            Popup popup = new Popup();
            ImageView imageView = new ImageView(image);
            imageView.setOnMouseClicked(e -> popup.hide());
            popup.getContent().add(imageView);
            popup.setX(Math.random() * 1000);
            popup.setY(Math.random() * 1000);
            popup.show(window);
        });
    }
}
