package com.samsepiol.portfolio.api;

public record NewsletterSubscriptionResponse(String status) {
    public static NewsletterSubscriptionResponse accepted() {
        return new NewsletterSubscriptionResponse("accepted");
    }
}
