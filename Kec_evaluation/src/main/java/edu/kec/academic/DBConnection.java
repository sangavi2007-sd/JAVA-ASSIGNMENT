package edu.kec.academic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class DBConnection {
    private static final String DEFAULT_URL = "jdbc:sqlite:kec_academic.db";

    private DBConnection() {
    }

    public static Connection open() throws SQLException {
        String url = System.getenv().getOrDefault("KEC_DB_URL", DEFAULT_URL);
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void initialize() throws SQLException, IOException {
        String schema = readSchema();
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                String command = sql.trim();
                if (!command.isEmpty()) {
                    statement.execute(command);
                }
            }
        }
    }

    private static String readSchema() throws IOException {
        Path localSchema = Paths.get("kec_schema.sql");
        if (Files.isRegularFile(localSchema)) {
            return new String(Files.readAllBytes(localSchema), StandardCharsets.UTF_8);
        }
        try (InputStream stream = DBConnection.class.getResourceAsStream("/kec_schema.sql")) {
            if (stream == null) {
                throw new IOException("Could not find kec_schema.sql in the working directory or application resources.");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
