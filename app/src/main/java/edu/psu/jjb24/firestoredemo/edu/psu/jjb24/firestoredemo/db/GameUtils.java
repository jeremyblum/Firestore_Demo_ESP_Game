package edu.psu.jjb24.firestoredemo.edu.psu.jjb24.firestoredemo.db;

public class GameUtils {
    public static boolean addGuess(Game game, String userId, String guess) {

        if (game.getTaboo().contains(guess)) {
            return false;
        }
        if (userId.equals(game.getUser1ID())) {
            game.getUser1Guess().add(guess);
        }
        else {
            game.getUser2Guess().add(guess);
        }
        return true;
    }

    public static String getCommonGuess(Game game) {
        if (game.getUser1Guess() == null || game.getUser2Guess() == null)
            return null;

        for (String s: game.getUser1Guess()) {
            if (game.getUser2Guess().contains(s))
                return s;
        }
        return null;
    }

}
