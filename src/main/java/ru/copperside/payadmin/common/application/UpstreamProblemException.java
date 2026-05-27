package ru.copperside.payadmin.common.application;

import org.springframework.http.HttpStatusCode;
import ru.copperside.payadmin.common.web.ProblemEnvelope;

public class UpstreamProblemException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final ProblemEnvelope problem;

    public UpstreamProblemException(HttpStatusCode statusCode, ProblemEnvelope problem) {
        super(problem == null || problem.error() == null ? "Upstream problem response" : problem.error().message());
        this.statusCode = statusCode;
        this.problem = problem;
    }

    public HttpStatusCode statusCode() {
        return statusCode;
    }

    public ProblemEnvelope problem() {
        return problem;
    }
}
