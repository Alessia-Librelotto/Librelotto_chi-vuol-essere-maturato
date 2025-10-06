public class PlayerStatistics {
    private String playerName;
    private int correctAnswers;
    private boolean used5050;
    private boolean usedAudience;

    public PlayerStatistics(String playerName, int correctAnswers, boolean used5050, boolean usedAudience) {
        this.playerName = playerName;
        this.correctAnswers = correctAnswers;
        this.used5050 = used5050;
        this.usedAudience = usedAudience;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public boolean isUsed5050() {
        return used5050;
    }

    public boolean isUsedAudience() {
        return usedAudience;
    }
}