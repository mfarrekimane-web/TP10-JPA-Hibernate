package com.example.util.logging.Logger;

import com.example.model.Reservation;

import java.util.logging.Logger;

public class ReservationService {
    private static final Logger LOGGER = Logger.getLogger(ReservationService.class.getName());

    public void createReservation(Reservation reservation) {
        try {
            // logique de création
            LOGGER.info("Création de la réservation réussie pour utilisateur : " + reservation.getUtilisateur().getEmail());
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la création de la réservation : " + e.getMessage());
            throw e;
        }
    }
}