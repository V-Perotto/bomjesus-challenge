package br.com.bomjesus.academic.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tratador global único (Princípio II): converte toda exceção em um {@link ProblemDetail}
 * (RFC 9457) com título em pt-BR, {@code instance}, {@code timestamp} e, quando aplicável,
 * a lista {@code errors} de erros por campo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String TITLE_INVALID_DATA = "Dados inválidos";
    static final String TITLE_BAD_REQUEST = "Requisição inválida";
    static final String TITLE_NOT_FOUND = "Recurso não encontrado";
    static final String TITLE_CONFLICT = "Conflito";
    static final String TITLE_INTERNAL_ERROR = "Erro interno";
    static final String DETAIL_INTERNAL_ERROR = "Ocorreu um erro inesperado. Tente novamente mais tarde.";
    static final String DETAIL_DATA_CONFLICT = "Os dados conflitam com um registro existente.";
    static final String INVALID_VALUE = "valor inválido";

    private static final String PROPERTY_TIMESTAMP = "timestamp";
    private static final String PROPERTY_ERRORS = "errors";

    private static final Map<Integer, String> TITLES_BY_STATUS = Map.of(
            400, TITLE_BAD_REQUEST,
            404, TITLE_NOT_FOUND,
            405, "Método não permitido",
            406, "Formato de resposta não suportado",
            409, TITLE_CONFLICT,
            415, "Tipo de mídia não suportado",
            500, TITLE_INTERNAL_ERROR);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        return respond(problem(HttpStatus.NOT_FOUND, TITLE_NOT_FOUND, ex.getMessage(), List.of()), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException ex, WebRequest request) {
        List<FieldErrorDetail> errors = List.of(new FieldErrorDetail(ex.getField(), ex.getFieldMessage()));
        return respond(problem(HttpStatus.CONFLICT, TITLE_CONFLICT, ex.getMessage(), errors), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                               WebRequest request) {
        log.warn("Violação de integridade em {}: {}", path(request), ex.getMostSpecificCause().getMessage());
        return respond(problem(HttpStatus.CONFLICT, TITLE_CONFLICT, DETAIL_DATA_CONFLICT, List.of()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Erro inesperado em {}", path(request), ex);
        return respond(problem(HttpStatus.INTERNAL_SERVER_ERROR, TITLE_INTERNAL_ERROR, DETAIL_INTERNAL_ERROR,
                List.of()), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldErrorDetail::field).thenComparing(FieldErrorDetail::message))
                .toList();
        String detail = "A requisição possui %d campo(s) inválido(s).".formatted(errors.size());
        return handleExceptionInternal(ex, problem(HttpStatus.BAD_REQUEST, TITLE_INVALID_DATA, detail, errors),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = findCause(ex, MismatchedInputException.class)
                .filter(mismatch -> !mismatch.getPath().isEmpty())
                .map(mismatch -> problem(HttpStatus.BAD_REQUEST, TITLE_INVALID_DATA,
                        "A requisição possui 1 campo(s) inválido(s).",
                        List.of(new FieldErrorDetail(fieldPath(mismatch.getPath()),
                                invalidValueMessage(mismatch.getTargetType())))))
                .orElseGet(() -> problem(HttpStatus.BAD_REQUEST, TITLE_BAD_REQUEST,
                        "O corpo da requisição está ausente ou não é um JSON válido.", List.of()));
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String parameter = ex instanceof MethodArgumentTypeMismatchException argumentMismatch
                ? argumentMismatch.getName()
                : ex.getPropertyName();
        String detail = "O parâmetro '%s' possui um valor inválido.".formatted(parameter);
        List<FieldErrorDetail> errors = List.of(
                new FieldErrorDetail(parameter, invalidValueMessage(ex.getRequiredType())));
        return handleExceptionInternal(ex, problem(HttpStatus.BAD_REQUEST, TITLE_BAD_REQUEST, detail, errors),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers,
                                                                         HttpStatusCode status,
                                                                         WebRequest request) {
        String detail = "O método %s não é suportado neste recurso.".formatted(ex.getMethod());
        return handleExceptionInternal(ex, problem(status, null, detail, List.of()), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers, HttpStatusCode status,
                                                                     WebRequest request) {
        String detail = "Tipo de conteúdo não suportado. Envie os dados como application/json.";
        return handleExceptionInternal(ex, problem(status, null, detail, List.of()), headers, status, request);
    }

    /**
     * O cliente não aceita JSON (ex.: {@code Accept: text/html}): responde 406 sem corpo, já que
     * ele também não aceitaria o formato de erro (spec, Edge Cases).
     */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                      HttpHeaders headers, HttpStatusCode status,
                                                                      WebRequest request) {
        return ResponseEntity.status(status).build();
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
                                                                    HttpHeaders headers, HttpStatusCode status,
                                                                    WebRequest request) {
        String detail = "O recurso %s não existe.".formatted(path(request));
        return handleExceptionInternal(ex, problem(status, TITLE_NOT_FOUND, detail, List.of()), headers, status,
                request);
    }

    /**
     * Ponto comum de saída dos erros tratados pela classe base: garante título em pt-BR,
     * {@code instance} e {@code timestamp} também para os erros gerados pelo próprio framework.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            complete(problem, request);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private ResponseEntity<Object> respond(ProblemDetail problem, WebRequest request) {
        complete(problem, request);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private static ProblemDetail problem(HttpStatusCode status, String title, String detail,
                                         List<FieldErrorDetail> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        if (title != null) {
            problem.setTitle(title);
        }
        if (!errors.isEmpty()) {
            problem.setProperty(PROPERTY_ERRORS, errors);
        }
        return problem;
    }

    private static void complete(ProblemDetail problem, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(problem.getStatus());
        boolean hasDefaultTitle = problem.getTitle() == null
                || (status != null && status.getReasonPhrase().equals(problem.getTitle()));
        if (hasDefaultTitle) {
            problem.setTitle(TITLES_BY_STATUS.getOrDefault(problem.getStatus(), problem.getTitle()));
        }
        if (problem.getInstance() == null) {
            problem.setInstance(URI.create(path(request)));
        }
        problem.setProperty(PROPERTY_TIMESTAMP, Instant.now());
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : "";
    }

    private static String fieldPath(List<JacksonException.Reference> references) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : references) {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static String invalidValueMessage(Class<?> targetType) {
        if (targetType != null && targetType.isEnum()) {
            String accepted = Arrays.stream(targetType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(" ou "));
            return INVALID_VALUE + "; use " + accepted;
        }
        return INVALID_VALUE;
    }

    private static <T extends Throwable> Optional<T> findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return Optional.of(type.cast(current));
            }
            current = current.getCause();
        }
        return Optional.empty();
    }
}
