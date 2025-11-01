package com.awal.cineq.file;

import java.io.IOException;

public class FileStorageException extends IOException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

