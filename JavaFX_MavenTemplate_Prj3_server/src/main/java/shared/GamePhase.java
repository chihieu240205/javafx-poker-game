package shared;

import java.io.Serializable;

public enum GamePhase implements Serializable {
    WAITING_BETS,
    DEALT,
    AWAITING_ACTION,
    RESOLVED
}
