package edu.psu.jjb24.firestoredemo;

import androidx.appcompat.app.AppCompatActivity;
import edu.psu.jjb24.firestoredemo.edu.psu.jjb24.firestoredemo.db.FirestoreESP;
import edu.psu.jjb24.firestoredemo.edu.psu.jjb24.firestoredemo.db.Game;
import edu.psu.jjb24.firestoredemo.edu.psu.jjb24.firestoredemo.db.GameUtils;
import edu.psu.jjb24.firestoredemo.edu.psu.jjb24.firestoredemo.db.Images;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements FirestoreESP.OnGameUpdateListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirestoreESP.getInstance().authenticate(this, (success, status) -> {
            if (success) showStatusUpdate(status);
            else showStatusUpdate("Authentication to firebase failed.");
        });
        FirestoreESP.getInstance().setOnGameUpdateListener(this);
    }

    public void startGame(View view) {
        FirestoreESP.getInstance().startGame();
    }

    public void joinGame(View view) {
        FirestoreESP.getInstance().findGame();
    }

    public void deleteGame(View view) {
        FirestoreESP.getInstance().quitCurrentGame();
    }

    public void guessWord(View view) {
        String guess = ((EditText) findViewById(R.id.edtGuess)).getText().toString();
        FirestoreESP.getInstance().guess(guess);
    }

    private void showStatusUpdate(String status) {
        Toast.makeText(this, status, Toast.LENGTH_LONG).show();
    }

    private void setupGame(Game game) {
        StringBuilder tabooWords = new StringBuilder("Taboo:");
        for (String taboo: game.getTaboo()) {
            tabooWords.append(" ");
            tabooWords.append(taboo);
        }

        ((TextView) findViewById(R.id.txtTaboo)).setText(tabooWords.toString());

        ((ImageView) findViewById(R.id.imgGame)).setImageResource(Images.ids[game.getImageIndex()]);
    }

    @Override
    public void onUpdate(FirestoreESP.RESULT_CODE resultCode, String status, Game game) {
        switch (resultCode) {
            case STARTED:
                showStatusUpdate("Started game.");
                setupGame(game);
                break;
            case PLAYER2_ACTION:
                String winner = GameUtils.getCommonGuess(game);
                if (winner != null) {
                    showStatusUpdate("Game won on common guess " + winner);
                }
                else {
                    showStatusUpdate("Game update received. Guess list sizes = (" + game.getUser1Guess().size() + "," +  game.getUser2Guess().size() + ")");
                }
                break;
            case ERROR:
                showStatusUpdate("Failure: " + status);
                break;
        }
    }

}
