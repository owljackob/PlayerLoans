package cz.sovajakub.economy.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DisagreeEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final int requestId;

    public DisagreeEvent(int requestId) {
        this.requestId = requestId;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public int getRequestId() {
        return requestId;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}