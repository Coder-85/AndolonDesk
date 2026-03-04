package org.amjonota;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.io.IOException;

public class IndexController {
    @FXML private ImageView slideImg;
    @FXML private VBox root;
    @FXML private VBox slideTxt;
    @FXML private VBox slideTxtParent;
    @FXML private HBox navBarBox;
    @FXML private HBox slider;
    @FXML private ScrollPane scrollPane;
    @FXML private HBox featureHBox;


    @FXML private ImageView featureImg;
    @FXML private HBox feature1;
    @FXML private HBox feature2;
    @FXML private HBox feature3;
    @FXML private HBox feature4;
    private Node[] featureNodes;
    private int numberOfNodes;

    @FXML private HBox newsHBox;
    @FXML private VBox newsPicContentParent;
    @FXML private VBox newsPicContent;
    @FXML private VBox newsCardParent;
    @FXML private VBox newsCard;


    private TranslateTransition tt;
    private TranslateTransition tt2;
    private SequentialTransition sequence;

    private double rootWidth;
    private double rootHeight;
    private double newsPicWidth;
    private double newsCardWidth;
    private boolean isFeatureTriggeredOnce = false;
    private boolean isNewsTriggeredOnce = false;
    private void initializeFeatureNode(){
        numberOfNodes = 5;
        featureNodes = new Node[numberOfNodes];
        featureNodes[0] = featureImg;
        featureNodes[1] = feature1;
        featureNodes[2] = feature2;
        featureNodes[3] = feature3;
        featureNodes[4] = feature4;
    }



    public void initialize() {
        featureImg.setOpacity(0);

        Platform.runLater(() -> {

            double width = root.getWidth();
            double height = root.getHeight();

            double slideTxtWidth = slideTxtParent.getWidth();
            double sildeImgParentWidth = width - slideTxtWidth;

            slider.setMinHeight(root.getHeight() - navBarBox.getHeight());

            tt = new TranslateTransition();
            tt.setNode(slideImg);
            tt.setDuration(Duration.seconds(0.5));
            tt.setFromX(sildeImgParentWidth);
            tt.setToX(0);

            tt2 = new TranslateTransition();
            tt2.setNode(slideTxt);
            tt2.setDuration(Duration.seconds(0.5));
            tt2.setFromX(-slideTxtWidth);
            tt2.setToX(0);

            sequence = new SequentialTransition();
            sequence.getChildren().add(tt);
            sequence.getChildren().add(tt2);

            sequence.play();

            newsPicWidth = newsPicContentParent.getWidth();
            newsCardWidth = newsCardParent.getWidth();

        });

        root.layoutBoundsProperty().addListener((obs, oldValue, newValue) -> {
            slider.setMinHeight(newValue.getHeight() - navBarBox.getHeight());

            newsPicWidth = newsPicContentParent.getWidth();
            newsCardWidth = newsCardParent.getWidth();
        });


        // Preparing Feature Nodes;
        initializeFeatureNode();
        FadeTransition[] ftArray = new FadeTransition[numberOfNodes];
        SequentialTransition seqFeature = new SequentialTransition();
        for(int i = 0; i<numberOfNodes; i++){
            featureNodes[i].setOpacity(0);

            ftArray[i] = new FadeTransition(Duration.seconds(0.4), featureNodes[i]);

            ftArray[i].setFromValue(0);
            ftArray[i].setToValue(1);
            ftArray[i].setCycleCount(1);
            ftArray[i].setAutoReverse(false);

            seqFeature.getChildren().add(ftArray[i]);
        }

        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            Bounds hboxBounds = featureHBox.localToScene(featureHBox.getBoundsInLocal());
            Bounds viewportBounds = scrollPane.getViewportBounds();

            double visibleHeight = Math.min(hboxBounds.getMaxY(), viewportBounds.getHeight())
                    - Math.max(hboxBounds.getMinY(), 0);
            visibleHeight = Math.max(0, visibleHeight);

            if (visibleHeight > featureHBox.getHeight() * 0.5 && !isFeatureTriggeredOnce) {
                seqFeature.play();
                isFeatureTriggeredOnce = true;
            }

        });

        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            Bounds hboxBounds = newsHBox.localToScene(newsHBox.getBoundsInLocal());
            Bounds viewportBounds = scrollPane.getViewportBounds();

            double visibleHeight = Math.min(hboxBounds.getMaxY(), viewportBounds.getHeight())
                    - Math.max(hboxBounds.getMinY(), 0);
            visibleHeight = Math.max(0, visibleHeight);

            if (visibleHeight > newsHBox.getHeight() * 0.5 && !isNewsTriggeredOnce) {

                System.out.println(50);
                isNewsTriggeredOnce = true;
            }

        });

    }

    public void setLoginPage() {
        try {
            App.setRoot("Login");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRegisterPage() {
        try {
            App.setRoot("Register");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
