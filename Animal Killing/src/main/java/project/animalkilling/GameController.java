package project.animalkilling;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import project.animalkilling.entities.Animal;
import project.animalkilling.entities.Player;
import project.animalkilling.entities.Bullet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class GameController extends SceneController{
    Stage stage = MainStage.getInstance().loadStage();
    private final Random RAND = new Random();
    public static int score, playerScore, animalScore;
    public static GraphicsContext gc;
    int liveTicks;
    int maxAnimal = 5, maxShots = maxAnimal * 2;
    int playerSize = 100;
    boolean gamePause;
    double mouseX;
    Player player;
    List<Bullet> bulletContainer;
    List<Animal> AnimalContainer;
    Image playerImg = new Image(GameController.class.getResource("img/other/Player.png").toString());
    Image[] AnimalImg = {
            new Image(GameController.class.getResource("img/Animal/purple.png").toString()),
            new Image(GameController.class.getResource("img/Animal/green.png").toString()),
            new Image(GameController.class.getResource("img/Animal/red.png").toString()),
            new Image(GameController.class.getResource("img/Animal/blue.png").toString()),
            new Image(GameController.class.getResource("img/Animal/yellow.png").toString()),
            new Image(GameController.class.getResource("img/Animal/xanh.png").toString()),
    };
    Image backgroundImg = new Image(GameController.class.getResource("img/other/background3.png").toString());


    //--Game Start--
    public void play() {
        Canvas canvas = new Canvas(MainScene.width, MainScene.height);
        gc = canvas.getGraphicsContext2D();
        Timeline timeline = new Timeline();
        KeyFrame frame = new KeyFrame(Duration.millis(50), e -> {
            try {
                if (run(gc)) {
                    timeline.stop();
                    OverController oc = new OverController();
                    oc.showScore();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        timeline.getKeyFrames().add(frame);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Scene ingame = new Scene(new StackPane(canvas));

        //--Ship shoot via key pressed--
        ingame.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                switch (keyEvent.getCode()) {
                    case A, S:
                        if (bulletContainer.size() < maxShots)
                            //add bullet if current shots array size does not exceed maxShots
                            bulletContainer.add(player.shoot());
                        break;
                    case ESCAPE:
                        if (!gamePause) {
                            gamePause = true;
                            timeline.pause();
                            gc.setFont(Font.loadFont(getClass().getResource("font/upheavtt.ttf").toExternalForm(), 50));
                            gc.setTextAlign(TextAlignment.CENTER);
                            gc.setFill(Color.WHITE);
                            gc.fillText("PAUSE GAME", MainScene.width / 2, MainScene.height / 2);
                        } else {
                            gamePause = false;
                            gc.setFill(Color.TRANSPARENT);
                            timeline.play();
                        }
                }
            }
        });

        //--Check if stage is focus or not--
        stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                gc.setFill(Color.TRANSPARENT);
                timeline.play();
            } else {
                timeline.pause();
                gc.setFont(Font.loadFont(getClass().getResource("font/upheavtt.ttf").toExternalForm(), 50));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFill(Color.WHITE);
                gc.fillText("PAUSE GAME", MainScene.width / 2, MainScene.height / 2);
            }
        });

        //--Ship movement via mouse--
        ingame.setCursor(Cursor.MOVE);
        ingame.setOnMouseMoved(e -> mouseX = e.getX());
        ingame.setOnMouseClicked(e -> {
            if (bulletContainer.size() < maxShots)
                bulletContainer.add(player.shoot());
        });

        setup();
        stage.setScene(ingame);
        stage.show();
    }

    //--Game setup--
    private Animal newAnimal() { //function to create a new Animal object
        int animalSize = playerSize /3;
        return new Animal(50 + RAND.nextInt(MainScene.width - 100), 0, animalSize,
                AnimalImg[RAND.nextInt(AnimalImg.length)]);
    }

    public void setup() {
        bulletContainer = new ArrayList<>();
        AnimalContainer = new ArrayList<>();
        player = new Player(MainScene.width / 2, MainScene.height - playerSize - 39, playerSize, playerImg);
        liveTicks = 5;
        playerScore = 0;
        animalScore = 0;
        IntStream.range(0, maxAnimal).mapToObj(i -> this.newAnimal()).forEach(AnimalContainer::add);
        //The IntStream.range() method is used to generate a sequence of integers from 0 to maxAnimal - 1.
        //For each integer in the sequence, a new animal object is created using the newAnimal() method.
        //Then each get added to the animal ArrayList using the forEach() method.
    }

    //--Run Graphics
    public boolean run(GraphicsContext gc) throws IOException {
        // setup background
        gc.drawImage(backgroundImg, 0, 0, MainScene.width, MainScene.height);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.loadFont(getClass().getResource("font/upheavtt.ttf").toExternalForm(), 30));
        gc.setFill(Color.WHITE);
        gc.fillText("Score: " + score, 50, 20);
        gc.fillText("Lives: " + liveTicks / 2, 50, 40);

        // draw player
        player.update();
        player.draw();
        player.setX((int) mouseX);

        AnimalContainer.stream().peek(Animal::update).peek(Animal::draw).forEach(e -> {
            if (player.collide(e) && !player.exploding) {
                e.explode();
                liveTicks--;
            }
            if (liveTicks == 1) {
                //sfx play here
                player.explode();
            }
        });

        for (int i = bulletContainer.size() - 1; i >= 0; i--) {
            Bullet shot = bulletContainer.get(i);
            if (shot.getY() < 0 || shot.getStatus()) {
                bulletContainer.remove(i);
                continue;
            }
            shot.update();
            shot.draw();
            for (Animal animal : AnimalContainer) {
                if (shot.collide(animal) && !animal.exploding) {
                    playerScore += 2;
                    animal.explode();
                    shot.setStatus(true);
                }
            }
        }

        for (Animal animal : AnimalContainer) {
            if (animal.getY() == MainScene.height) {
                animalScore += 4;
            }
        }

        for (int i = AnimalContainer.size() - 1; i >= 0; i--) {
            if (AnimalContainer.get(i).destroyed) {
                AnimalContainer.set(i, newAnimal());
            }
        }

        // assign total score
        score = playerScore - animalScore;

        return player.destroyed || score < 0;
    }
}
