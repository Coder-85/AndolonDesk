package org.amjonota;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
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
import javafx.scene.control.Label;
import java.security.Key;

public class IndexController {
    @FXML private ImageView slideImg;
    @FXML private VBox root;
    @FXML private VBox slideTxt;
    @FXML private VBox slideTxtParent;
    @FXML private HBox navBarBox;
    @FXML private HBox slider;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox content;
    @FXML private HBox featureHBox;
    private SequentialTransition seqFeature;


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

    @FXML private HBox statBox;
    @FXML private ImageView statImg;
    @FXML private HBox stat1;
    @FXML private HBox stat2;
    @FXML private HBox stat3;

    @FXML private Label statUserNum;
    @FXML private Label statPostNum;
    @FXML private Label statViewNum;
    private int statUserCount = 217;
    private int statPostCount = 35;
    private int statViewCount = 4324;
    private final int statAnimTime = 1000; // in ms
    private final int frameChangingTime = 20;
    private int startingUserCount = 0;
    private int startingPostCount = 0;
    private int startingViewCount = 0;
    private Timeline statTimeline;


    private TranslateTransition tt;
    private TranslateTransition tt2;
    private SequentialTransition sequence;

    private TranslateTransition newstt;
    private TranslateTransition newstt2;
    static boolean isFeatureBtnClicked = false;

    private double rootWidth;
    private double rootHeight;
    private double newsPicWidth;
    private double newsCardWidth;
    private boolean isFeatureTriggeredOnce = false;
    private boolean isNewsTriggeredOnce = false;
    private boolean isStatTriggeredOnce = false;
    private void initializeFeatureNode(){
        numberOfNodes = 5;
        featureNodes = new Node[numberOfNodes];
        featureNodes[0] = featureImg;
        featureNodes[1] = feature1;
        featureNodes[2] = feature2;
        featureNodes[3] = feature3;
        featureNodes[4] = feature4;
    }

    private void animateStat(Label label, int target) {

        int frames = statAnimTime / frameChangingTime;
        double increment = (double) target / frames;

        final double[] value = {0};

        Timeline timeline = new Timeline();

        KeyFrame keyFrame = new KeyFrame(Duration.millis(frameChangingTime), e -> {
            value[0] += increment;

            if (value[0] >= target) {
                label.setText(String.valueOf(target));
                timeline.stop();
            } else {
                label.setText(String.valueOf((int) value[0]));
            }
        });

        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
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
            newsCardWidth = root.getWidth()-newsPicWidth;

            newsCard.setTranslateX(newsCardWidth);
            newsPicContent.setTranslateX(-newsPicWidth);

            newstt = new TranslateTransition();
            newstt.setNode(newsPicContent);
            newstt.setDuration(Duration.seconds(0.5));
            newstt.setFromX(-newsPicWidth);
            newstt.setToX(0);

            newstt2 = new TranslateTransition();
            newstt2.setNode(newsCard);
            newstt2.setDuration(Duration.seconds(0.5));
            newstt2.setFromX(newsCardWidth);
            newstt2.setToX(0);

            if(isFeatureBtnClicked){
                gotoFeature();
            }

        });

        root.layoutBoundsProperty().addListener((obs, oldValue, newValue) -> {
            slider.setMinHeight(newValue.getHeight() - navBarBox.getHeight());

            newsPicWidth = newsPicContentParent.getWidth();
            newsCardWidth = root.getWidth()-newsPicWidth;

            if(!isNewsTriggeredOnce){
                newsCard.setTranslateX(newsCardWidth);
                newsPicContent.setTranslateX(-newsPicWidth);
            }

            if(newstt != null && newstt2 != null){
                newstt.setFromX(-newsPicWidth);
                newstt2.setFromX(newsCardWidth);
            }
        });


        // Preparing Feature Nodes;
        initializeFeatureNode();
        FadeTransition[] ftArray = new FadeTransition[numberOfNodes];
        seqFeature = new SequentialTransition();
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

            if (visibleHeight > featureHBox.getHeight() * 0.6 && !isFeatureTriggeredOnce) {
                seqFeature.play();
                isFeatureTriggeredOnce = true;
            }


            Bounds newshboxBounds = newsHBox.localToScene(newsHBox.getBoundsInLocal());

            double visibleHeightNews = Math.min(newshboxBounds.getMaxY(), viewportBounds.getHeight())
                    - Math.max(newshboxBounds.getMinY(), 0);
            visibleHeightNews = Math.max(0, visibleHeightNews);

            if (visibleHeightNews > newsHBox.getHeight() * 1 && !isNewsTriggeredOnce) {
                newstt.play();
                newstt2.play();
                isNewsTriggeredOnce = true;
            }

            Bounds statHboxBounds = statBox.localToScene(statBox.getBoundsInLocal());

            double visibleHeightStat = Math.min(statHboxBounds.getMaxY(), viewportBounds.getHeight())
                    - Math.max(statHboxBounds.getMinY(), 0);
            visibleHeightStat = Math.max(0, visibleHeightStat);

            if (visibleHeightStat > statBox.getHeight() * 0.7 && !isStatTriggeredOnce) {
                animateStat(statUserNum, statUserCount);
                animateStat(statPostNum, statPostCount);
                animateStat(statViewNum, statViewCount);
                isStatTriggeredOnce = true;
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

    public void setAboutPage() {
        try {
            App.setRoot("about_us");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void gotoFeature() {
        double height = content.getHeight();
        double y = featureHBox.getLayoutY();
        scrollPane.setVvalue(y / height);
        if(!isFeatureTriggeredOnce){
            seqFeature.play();
            isFeatureTriggeredOnce = true;
        }
        isFeatureBtnClicked = false;
    }
}
