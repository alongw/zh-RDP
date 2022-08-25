package com.microsoft.cll.android;

public class TicketObject {
    public String ticket;
    public boolean hasDeviceClaims;

    public TicketObject(String ticket, boolean hasDeviceClaims) {

        this.ticket = ticket;
        this.hasDeviceClaims = hasDeviceClaims;
    }
}
