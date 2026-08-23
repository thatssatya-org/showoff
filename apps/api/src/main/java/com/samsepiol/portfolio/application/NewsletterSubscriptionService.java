package com.samsepiol.portfolio.application;

import com.samsepiol.portfolio.api.NewsletterSubscriptionRequest;

public interface NewsletterSubscriptionService {
    void subscribe(NewsletterSubscriptionRequest request);
}
