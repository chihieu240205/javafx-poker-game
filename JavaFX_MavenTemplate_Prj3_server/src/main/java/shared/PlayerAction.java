package shared;

import java.io.Serializable;

public enum PlayerAction implements Serializable {
    NONE, REQUEST_DEAL, PLAY, FOLD, REQUEST_STATUS
}
