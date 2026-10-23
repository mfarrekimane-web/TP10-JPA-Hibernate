package com.example.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class MigrationExecutor {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public MigrationExecutor(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public void executeMigration(String scriptPath) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(scriptPath);
        if (is == null) {
            throw new IllegalArgumentException(
                    "Script introuvable dans src/main/resources : " + scriptPath);
        }

        // 1. Lire le script (en ignorant les lignes de commentaire "--")
        StringBuilder sql = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("--")) continue;
                sql.append(line).append("\n");
            }
        }

        // 2. Exécuter les requêtes une par une (séparées par ';')
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            for (String requete : sql.toString().split(";")) {
                String r = requete.trim();
                if (r.isEmpty()) continue;
                try {
                    stmt.execute(r);
                } catch (java.sql.SQLException e) {
                    // 1060 = colonne déjà existante, 1050 = table existe déjà,
                    // 1061 = index déjà existant, 1091 = élément à supprimer introuvable
                    int code = e.getErrorCode();
                    if (code == 1060 || code == 1050 || code == 1061 || code == 1091) {
                        System.out.println("Migration déjà appliquée, ignorée : " + r);
                    } else {
                        throw e;
                    }
                }
            }
        }
    }}