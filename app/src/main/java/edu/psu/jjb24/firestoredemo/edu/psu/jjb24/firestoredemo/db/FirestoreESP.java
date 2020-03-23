package edu.psu.jjb24.firestoredemo.edu.psu.jjb24.firestoredemo.db;

import android.app.Activity;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// See added dependencies in app/build.grade
public class FirestoreESP {
    private static final String TAG = "FirestoreESP";

    public enum RESULT_CODE {
        STARTED, PLAYER2_ACTION, ERROR
    }
    public interface OnAuthenticatedListener {
        void onAuthenticated(boolean success, String status);
    }

    public interface OnGameUpdateListener {
        void onUpdate(RESULT_CODE resultCode, String status, Game game);
    }

    private static Random prng = new Random();


    private static FirestoreESP INSTANCE;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private DocumentReference currentGame;
    private ListenerRegistration currentGameListener;

    private OnGameUpdateListener listener;

    private FirestoreESP() {}

    public static synchronized FirestoreESP getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FirestoreESP();
        }
        return INSTANCE;
    }

    public void authenticate(Activity activity, final OnAuthenticatedListener listener) {
        if (user == null) {
            db = FirebaseFirestore.getInstance();

            final FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.signInAnonymously()
                    .addOnCompleteListener(activity, task -> {
                        if (task.isSuccessful()) {
                            user = auth.getCurrentUser();
                            listener.onAuthenticated(true, "Logged in with id: " + user.getUid());
                        } else {
                            listener.onAuthenticated(false, null);
                        }
                    });
        }
        else {
            listener.onAuthenticated(true, "Already logged in with id: " + user.getUid());
        }
    }

    public void setOnGameUpdateListener(OnGameUpdateListener listener) {
        this.listener = listener;
    }

    // Create a new game
    public void startGame() {
        final int pictureIndex = prng.nextInt(Images.ids.length);

        Log.d(TAG, "Starting a new game.  Picked image image" + (pictureIndex+1) + ".jpg.  Now getting taboo list.");
        DocumentReference tabooListRef = db.collection("taboo").document("" + (pictureIndex + 1));
        tabooListRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Log.d(TAG, "Retrieved taboo list: " + document.getData());
                    ArrayList<String> taboo = (ArrayList<String>) document.get("taboo");
                    Game game = new Game(pictureIndex,user.getUid(),"", taboo, new ArrayList<String>(), new ArrayList<String>());
                    addGameToDB(game);
                } else {
                    Log.d(TAG, "No taboo list found for this image.");
                    listener.onUpdate(RESULT_CODE.ERROR, "No taboo list exists for this image", null);
                }
            } else {
                Log.d(TAG, "Taboo list retrieval cause exception: " + task.getException());
                listener.onUpdate(RESULT_CODE.ERROR, "No getTaboo list get failed: " + task.getException(), null);
            }
        });
    }


    private void addGameToDB(Game game) {
        Log.d(TAG, "Adding a new game to the firestore database");
        final Map<String, Object> document = new HashMap<>();
        document.put("status", "open");
        document.put("game", game);
        document.put("start_timestamp", FieldValue.serverTimestamp());
        db.collection("games")
                .add(document)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "New game to the firestore database with ID: " + documentReference.getId());
                    setCurrentGame(documentReference);
                    listener.onUpdate(RESULT_CODE.STARTED, "", game);
                })
                .addOnFailureListener( (@NonNull Exception e) -> {
                    Log.d(TAG, "Adding a new game to the firestore database cause exception: " + e.getMessage());
                    listener.onUpdate(RESULT_CODE.ERROR, "Error adding game to firestore: " + e.getMessage(), null);
                });
    }

    private void setCurrentGame(DocumentReference document) {
        currentGame = document;
        if (currentGameListener != null) currentGameListener.remove();
        currentGameListener =
                document.addSnapshotListener((@Nullable DocumentSnapshot snapshot,
                                              @Nullable FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Log.d(TAG, "Listening for updates to game cause an exception: " + e.getMessage());
                        listener.onUpdate(RESULT_CODE.ERROR, "Failed to register listener for game changes: " + e.getMessage(), null);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Log.d(TAG, "Update to game occured: " + snapshot.getData());
                        Game game = snapshot.get("game",Game.class);
                        listener.onUpdate(RESULT_CODE.PLAYER2_ACTION, "", game);
                    } else {
                        Log.d(TAG, "Update to game occured, but sent a null update.");
                        listener.onUpdate(RESULT_CODE.ERROR, "Game ended", null);
                    }
                });

    }

    // Attempt to join an existing game
    public void findGame() {
        if (user == null) {
            Log.d(TAG, "Firestore user is null when trying to find game");
            listener.onUpdate(RESULT_CODE.ERROR, "No user logged in.", null);
            return;
        }

        Log.d(TAG, "Attempting to find open game");
        db.collection("games")
                .whereEqualTo("status", "open")
                .orderBy("start_timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && task.getResult().size() > 0) {
                            Log.d(TAG, "Game found.  Attempting to join game.");
                            DocumentSnapshot snapshot = task.getResult().getDocuments().get(0);
                            joinGame(snapshot.getReference());
                        }
                        else {
                            Log.d(TAG, "No open games found.");
                            listener.onUpdate(RESULT_CODE.ERROR, "No open games found.", null);
                        }
                    }
                });
    }

    // Attempt to join the game described by document
    private void joinGame(final DocumentReference document) {
        db.runTransaction( transaction -> {
            Log.d(TAG, "Starting transaction to join game.");
            DocumentSnapshot snapshot = transaction.get(document);
            Game game = snapshot.get("game",Game.class);
            if (snapshot.get("status").equals("open")) {
                transaction.update(document, "status", "started");
                game.setUser2ID(user.getUid());
                transaction.update(document, "game", game);
            }
            else {
                Log.d(TAG, "Status field is no longer equal to 'open'.  Cannot join this game.");
                throw new FirebaseFirestoreException("Game no longer open",FirebaseFirestoreException.Code.ABORTED);
            }
            return game;
        }).addOnSuccessListener(game-> {
            Log.d(TAG, "Successfully joined game.");
            setCurrentGame(document);
            listener.onUpdate(RESULT_CODE.STARTED, "", game);
        }).addOnFailureListener((@NonNull Exception e) -> {
            Log.d(TAG, "Transaction failed - could not join game.");
            listener.onUpdate(RESULT_CODE.ERROR, "Game state changed while transaction was executing", null);
        });
    }

    public void quitCurrentGame() {
        if (currentGame != null) {
            Log.d(TAG, "Deleting current game");
            currentGame.delete();
            currentGameListener.remove();
        }
        currentGame = null;
        currentGameListener = null;
    }

    public void guess(String guess) {
        if (currentGame == null) {
            Log.d(TAG, "Attempted to submit a guess with no game in progress");
            listener.onUpdate(RESULT_CODE.ERROR, "no game in progress", null);
        }
        else {
            db.runTransaction( transaction -> {
                Log.d(TAG, "Starting a transaction to add a new guess for game");
                DocumentSnapshot snapshot = transaction.get(currentGame);
                Game game = snapshot.get("game",Game.class);
                GameUtils.addGuess(game, user.getUid(), guess);
                transaction.update(currentGame, "game", game);
                return null;
            }).addOnSuccessListener( aVoid -> {
                Log.d(TAG, "Successfully submitted a new guess");
                // Do nothing on success
            }).addOnFailureListener((@NonNull Exception e) -> {
                Log.d(TAG, "Unable to submit a guess due to concurrent modification");
                listener.onUpdate(RESULT_CODE.ERROR, "Unable to add guess, maybe due to concurrent modification, but more likely because of a non-robust way of handling deleted games. To make this robust, you should try again.", null);
            });
        }
    }

}
