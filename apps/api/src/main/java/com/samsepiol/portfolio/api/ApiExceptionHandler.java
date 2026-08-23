package com.samsepiol.portfolio.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.net.URI;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
    private static final URI INVALID_CAPABILITY_TYPE = URI.create("https://portfolio.invalid/problems/invalid-capability");
    private static final URI INVALID_NEWSLETTER_SUBSCRIPTION = URI.create("https://portfolio.invalid/problems/invalid-newsletter-subscription");
    private static final URI NEWSLETTER_UNAVAILABLE = URI.create("https://portfolio.invalid/problems/newsletter-unavailable");
    private static final URI NEWSLETTER_RATE_LIMITED = URI.create("https://portfolio.invalid/problems/newsletter-rate-limited");

    @ExceptionHandler(InvalidCapabilityException.class)
    ProblemDetail invalidCapability(InvalidCapabilityException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(INVALID_CAPABILITY_TYPE);
        problem.setTitle("Invalid capability");
        return problem;
    }

    @ExceptionHandler({NewsletterSubscriptionException.class, MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ProblemDetail invalidNewsletterSubscription(Exception exception) {
        return newsletterProblem(HttpStatus.BAD_REQUEST, "Invalid newsletter subscription");
    }

    @ExceptionHandler(NewsletterOriginRejectedException.class)
    ProblemDetail newsletterOriginRejected(NewsletterOriginRejectedException exception) {
        return newsletterProblem(HttpStatus.FORBIDDEN, "Newsletter subscription forbidden");
    }

    @ExceptionHandler(NewsletterServiceUnavailableException.class)
    ProblemDetail newsletterUnavailable(NewsletterServiceUnavailableException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Newsletter subscriptions are temporarily unavailable.");
        problem.setType(NEWSLETTER_UNAVAILABLE);
        problem.setTitle("Newsletter unavailable");
        return problem;
    }

    @ExceptionHandler(NewsletterRateLimitedException.class)
    ProblemDetail newsletterRateLimited(NewsletterRateLimitedException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "The subscription request could not be accepted.");
        problem.setType(NEWSLETTER_RATE_LIMITED);
        problem.setTitle("Too many subscription requests");
        return problem;
    }

    private ProblemDetail newsletterProblem(HttpStatus status, String title) {
        var problem = ProblemDetail.forStatusAndDetail(status, "The subscription request could not be accepted.");
        problem.setType(INVALID_NEWSLETTER_SUBSCRIPTION);
        problem.setTitle(title);
        return problem;
    }
}
