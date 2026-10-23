package com.example;

import com.example.util.DataInitializer;
import com.example.util.PerformanceReport;
import com.example.util.MigrationExecutor;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {

    public static void main(String[] args) throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("ReservationPU");
        EntityManager em = emf.createEntityManager();

        // Exécuter la migration
        MigrationExecutor migrationExecutor = new MigrationExecutor(
                "jdbc:mysql://localhost:3306/reservation?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC",
                "root",
                "root")
        ;
        migrationExecutor.executeMigration("db/migration/v2_0_add_description.sql");

        // Initialiser les données
        DataInitializer initializer = new DataInitializer(em);
        initializer.initData();

        // Générer rapport de performance
        PerformanceReport report = new PerformanceReport(em);
        report.printReport();

        em.close();
        emf.close();
    }
}