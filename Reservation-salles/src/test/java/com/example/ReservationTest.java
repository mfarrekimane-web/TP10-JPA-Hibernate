package com.example;

import com.example.model.*;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.jupiter.api.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.List;
import java.time.LocalDateTime;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;
public class ReservationTest {

    private static EntityManagerFactory emf;
    private EntityManager em;

    @BeforeAll
    public static void init() {
        emf = Persistence.createEntityManagerFactory("ReservationPU");
    }

    @BeforeEach
    public void setUp() {
        em = emf.createEntityManager();
    }

    @AfterEach
    public void tearDown() {
        if (em != null) em.close();
    }

    @AfterAll
    public static void close() {
        if (emf != null) emf.close();
    }

    @Test
    public void testCreateReservation() {
        em.getTransaction().begin();

        Utilisateur user = new Utilisateur();
        user.setNom("Test User");
        user.setEmail("test.user@example.com");
        em.persist(user);

        Salle salle = new Salle();
        salle.setNom("Salle A");
        salle.setCapacite(30);
        em.persist(salle);

        Reservation reservation = new Reservation();
        reservation.setUtilisateur(user);
        reservation.setSalle(salle);
        reservation.setDateDebut(LocalDateTime.now().plusDays(2));
        reservation.setDateFin(LocalDateTime.now().plusDays(2).plusHours(1));

        em.persist(reservation);
        em.getTransaction().commit();

        Assertions.assertNotNull(reservation.getId());
    }
    @Test
    public void testRequetes() {
        em.getTransaction().begin();

        Utilisateur user = new Utilisateur();
        user.setNom("Test");
        user.setEmail("requetes@example.com");
        em.persist(user);

        Salle salle = new Salle();
        salle.setNom("Salle A");
        salle.setCapacite(30);
        em.persist(salle);

        Reservation r = new Reservation();
        r.setUtilisateur(user);
        r.setSalle(salle);
        r.setDateDebut(LocalDateTime.now().plusDays(1));
        r.setDateFin(LocalDateTime.now().plusDays(1).plusHours(2));
        em.persist(r);

        em.getTransaction().commit();

        // --- JPQL ---
        String jpql = "SELECT r FROM Reservation r WHERE r.salle.nom = :nomSalle AND r.dateDebut > :date";
        List<Reservation> jpqlResults = em.createQuery(jpql, Reservation.class)
                .setParameter("nomSalle", "Salle A")
                .setParameter("date", LocalDateTime.now())
                .getResultList();

        // --- Criteria API ---
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Reservation> cq = cb.createQuery(Reservation.class);
        Root<Reservation> root = cq.from(Reservation.class);
        Predicate predicate = cb.and(
                cb.equal(root.get("salle").get("nom"), "Salle A"),
                cb.greaterThan(root.<LocalDateTime>get("dateDebut"), LocalDateTime.now())
        );
        cq.select(root).where(predicate);
        List<Reservation> criteriaResults = em.createQuery(cq).getResultList();

        Assertions.assertEquals(1, jpqlResults.size());
        Assertions.assertEquals(1, criteriaResults.size());
    }
    @Test
    public void testOptimisticLock() {
        // 1. Créer une réservation
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            Utilisateur user = new Utilisateur();
            user.setNom("Lock");
            user.setEmail("lock@example.com");
            em.persist(user);

            Salle salle = new Salle();
            salle.setNom("Salle B");
            salle.setCapacite(20);
            em.persist(salle);

            Reservation r = new Reservation();
            r.setUtilisateur(user);
            r.setSalle(salle);
            r.setDateDebut(LocalDateTime.now().plusDays(3));
            r.setDateFin(LocalDateTime.now().plusDays(3).plusHours(1));
            em.persist(r);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        }
        Long id = em.createQuery("SELECT MAX(r.id) FROM Reservation r", Long.class).getSingleResult();

        // 2. Deux EntityManagers lisent la même réservation (version 0)
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        Reservation r1 = em1.find(Reservation.class, id);
        Reservation r2 = em2.find(Reservation.class, id);

        // 3. La première transaction modifie et valide : version 0 -> 1
        em1.getTransaction().begin();
        r1.setDateFin(r1.getDateFin().plusHours(1));
        em1.getTransaction().commit();

        // 4. La seconde modifie avec l'ancienne version : conflit
        em2.getTransaction().begin();
        r2.setDateFin(r2.getDateFin().plusHours(2));
        try {
            em2.getTransaction().commit();
            Assertions.fail("Un conflit de version était attendu");
        } catch (RollbackException e) {
            Assertions.assertTrue(e.getCause() instanceof OptimisticLockException);
            System.out.println("Conflit détecté, veuillez réessayer.");
        } finally {
            if (em2.getTransaction().isActive()) em2.getTransaction().rollback();
            em1.close();
            em2.close();
        }
    }
}