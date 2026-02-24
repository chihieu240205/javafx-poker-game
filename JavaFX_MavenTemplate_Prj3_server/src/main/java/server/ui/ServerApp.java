package server.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server.net.PokerServer;

public class ServerApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("/server/ui/server_intro.fxml"));

        stage.setScene(new Scene(loader.load()));
        stage.setTitle("3-Card Poker Server");
        stage.show();
    }

    public static void showMonitorScene(Stage stage, PokerServer server) throws Exception {
        FXMLLoader loader =
                new FXMLLoader(ServerApp.class.getResource("/server/ui/server_monitor.fxml"));

        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.setTitle("Server Monitor");

        ServerMonitorController controller = loader.getController();
        controller.setServer(server);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
