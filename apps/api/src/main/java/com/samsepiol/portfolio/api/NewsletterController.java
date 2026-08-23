package com.samsepiol.portfolio.api;

import com.samsepiol.portfolio.application.NewsletterSubscriptionService;
import com.samsepiol.portfolio.security.NewsletterRequestProtector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/newsletter/subscriptions")
public class NewsletterController {
    private final NewsletterSubscriptionService newsletterSubscriptionService;
    private final NewsletterOriginValidator originValidator;
    private final NewsletterRequestProtector requestProtector;

    public NewsletterController(NewsletterSubscriptionService newsletterSubscriptionService,
                                NewsletterOriginValidator originValidator,
                                NewsletterRequestProtector requestProtector) {
        this.newsletterSubscriptionService = newsletterSubscriptionService;
        this.originValidator = originValidator;
        this.requestProtector = requestProtector;
    }

    @PostMapping
    public ResponseEntity<NewsletterSubscriptionResponse> subscribe(
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin,
            @Valid @RequestBody NewsletterSubscriptionRequest request,
            HttpServletRequest servletRequest) {
        if (!originValidator.isAllowed(origin)) {
            throw new NewsletterOriginRejectedException();
        }
        if (request.website() != null) {
            throw new NewsletterSubscriptionException();
        }
        if (!requestProtector.allow(servletRequest.getRemoteAddr())) {
            throw new NewsletterRateLimitedException();
        }

        if (!requestProtector.wasAccepted(request)) {
            newsletterSubscriptionService.subscribe(request);
            requestProtector.recordAccepted(request);
        }
        return ResponseEntity.accepted()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(NewsletterSubscriptionResponse.accepted());
    }
}
