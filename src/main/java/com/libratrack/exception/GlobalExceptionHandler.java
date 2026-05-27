package com.libratrack.exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestControllerAdvice @Slf4j
public class GlobalExceptionHandler {
    private record ErrorResponse(int status,String error,String message){}
    @ExceptionHandler(ResourceNotFoundException.class) public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException e){return ResponseEntity.status(404).body(new ErrorResponse(404,"Not Found",e.getMessage()));}
    @ExceptionHandler(NoCopyAvailableException.class) public ResponseEntity<ErrorResponse> noCopy(NoCopyAvailableException e){return ResponseEntity.status(409).body(new ErrorResponse(409,"Conflict",e.getMessage()));}
    @ExceptionHandler(DuplicateResourceException.class) public ResponseEntity<ErrorResponse> dup(DuplicateResourceException e){return ResponseEntity.status(409).body(new ErrorResponse(409,"Conflict",e.getMessage()));}
    @ExceptionHandler(BorrowLimitExceededException.class) public ResponseEntity<ErrorResponse> limit(BorrowLimitExceededException e){return ResponseEntity.status(422).body(new ErrorResponse(422,"Unprocessable Entity",e.getMessage()));}
    @ExceptionHandler(UnpaidFineException.class) public ResponseEntity<ErrorResponse> fine(UnpaidFineException e){return ResponseEntity.status(422).body(new ErrorResponse(422,"Unprocessable Entity",e.getMessage()));}
    @ExceptionHandler(AuthenticationException.class) public ResponseEntity<ErrorResponse> unauthenticated(AuthenticationException e){return ResponseEntity.status(401).body(new ErrorResponse(401,"Unauthorized",e.getMessage()));}
    @ExceptionHandler(AccessDeniedException.class) public ResponseEntity<ErrorResponse> denied(AccessDeniedException e){return ResponseEntity.status(403).body(new ErrorResponse(403,"Forbidden",e.getMessage()));}
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<ErrorResponse> illegalArg(IllegalArgumentException e){return ResponseEntity.status(400).body(new ErrorResponse(400,"Bad Request",e.getMessage()));}
    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException e){Map<String,String> errors=new LinkedHashMap<>();for(FieldError fe:e.getBindingResult().getFieldErrors())errors.put(fe.getField(),fe.getDefaultMessage());return ResponseEntity.badRequest().body(errors);}
    @ExceptionHandler(Exception.class) public ResponseEntity<ErrorResponse> generic(Exception e){log.error("Unexpected",e);return ResponseEntity.status(500).body(new ErrorResponse(500,"Internal Server Error","Unexpected error"));}
}
