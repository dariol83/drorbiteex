package eu.dariolucia.drorbiteex.fxml;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.LinkedList;

public class LogViewer {

    private static final int MAX_NB_MESSAGES = 500;

    private final ObservableList<LogMessage> messages = FXCollections.observableList(new LinkedList<>());
    private Stage stage;
    private TableView<LogMessage> table;
    private TableColumn<LogMessage, String> timeColumn;
    private TableColumn<LogMessage, String> messageColumn;

    public LogViewer() {
        table = new TableView<>();
        timeColumn = new TableColumn<>("Time");
        timeColumn.setMinWidth(120);
        timeColumn.setReorderable(false);
        table.getColumns().add(timeColumn);
        messageColumn = new TableColumn<>("Message");
        messageColumn.setMinWidth(450);
        messageColumn.setReorderable(false);
        table.getColumns().add(messageColumn);
        table.setItems(messages);
        timeColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().time));
        messageColumn.setCellValueFactory(o -> new ReadOnlyStringWrapper(o.getValue().message));
        CssHolder.applyTo(table);
    }

    public void open(Window w) {
        if(stage == null) {
            stage = new Stage();
            stage.setTitle("Log messages");
            stage.setAlwaysOnTop(false);
            stage.setResizable(true);
            stage.setMinHeight(300);
            stage.setMinWidth(600);
            Image icon = new Image(LogViewer.class.getResourceAsStream("/satellite-uplink_24.png"));
            stage.getIcons().add(icon);
            stage.setScene(new Scene(table));
            stage.setOnCloseRequest(event -> {
                stage.getScene().setRoot(new VBox());
                stage.close();
                stage = null;
            });
            stage.show();
            table.getSelectionModel().selectLast();
        }
    }

    public void addMessage(String time, String message) {
        messages.add(new LogMessage(time, message));
        while(messages.size() > MAX_NB_MESSAGES) {
            messages.remove(0);
        }
    }

    private static class LogMessage {
        public final String time;
        public final String message;

        public LogMessage(String time, String message) {
            this.time = time;
            this.message = message;
        }
    }
}
