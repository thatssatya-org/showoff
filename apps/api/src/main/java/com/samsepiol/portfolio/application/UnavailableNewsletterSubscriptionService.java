package com.samsepiol.portfolio.application;

import com.samsepiol.portfolio.api.NewsletterSubscriptionRequest;
import com.samsepiol.portfolio.api.NewsletterServiceUnavailableException;

public class UnavailableNewsletterSubscriptionService implements NewsletterSubscriptionService {
    @Override
    public void subscribe(NewsletterSubscriptionRequest request) {
        throw new NewsletterServiceUnavailableException();
    }
}
