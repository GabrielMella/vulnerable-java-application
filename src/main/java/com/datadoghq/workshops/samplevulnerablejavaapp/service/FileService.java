package com.datadoghq.workshops.samplevulnerablejavaapp.service;

import com.datadoghq.workshops.samplevulnerablejavaapp.exception.FileForbiddenFileException;
import com.datadoghq.workshops.samplevulnerablejavaapp.exception.FileReadException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileService {
    static final Path ALLOWED_PREFIX = Path.of("/tmp/files/");

    public String readFile(String path) throws FileForbiddenFileException, FileReadException {
        Path canonical;
        try {
            canonical = Paths.get(path).toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new FileReadException(e.getMessage());
        }
        if (!canonical.startsWith(ALLOWED_PREFIX)) {
            throw new FileForbiddenFileException("You are not allowed to read " + path);
        }
        try (BufferedReader br = new BufferedReader(new FileReader(canonical.toFile()))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            return sb.toString();
        } catch (IOException e) {
            throw new FileReadException(e.getMessage());
        }
    }
}
