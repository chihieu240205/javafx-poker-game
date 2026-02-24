package server.game;

import java.util.ArrayList;
import java.util.List;

import shared.RoundOutcome;

public class GameResult {
    public RoundOutcome outcome;
    public int netChange;
    public List<String> messages = new ArrayList<>();
}
