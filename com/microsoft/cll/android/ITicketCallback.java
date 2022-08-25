package com.microsoft.cll.android;

public interface ITicketCallback {
    public String getMsaDeviceTicket(boolean forceRefresh);

    public String getAuthXToken(boolean forceRefresh);

    public TicketObject getXTicketForXuid(String xuid);
}
