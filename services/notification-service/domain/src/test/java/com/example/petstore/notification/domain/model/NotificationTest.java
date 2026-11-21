package com.example.petstore.notification.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    private static final Instant ADOPTED_AT = Instant.parse("2026-12-04T10:15:30Z");
    private static final EmailAddress SHELTER = new EmailAddress("refugio@petstore.example");

    @Test
    void theAdoptionNoticeTellsTheShelterWhoAdoptedWhichPet() {
        Notification notice = Notification.adoptionNotice(new Adoption("1", "Luna", "Ana", ADOPTED_AT), SHELTER);

        assertThat(notice.recipient()).isEqualTo(SHELTER);
        assertThat(notice.subject()).isEqualTo("Ana ha adoptado a Luna");
        assertThat(notice.text())
                .contains("Ana ha adoptado a Luna (mascota 1).")
                .contains("Fecha de la adopción: 2026-12-04T10:15:30Z");
    }

    @Test
    void anAdoptionIsIdentifiedByThePetAndTheMoment() {
        assertThat(new Adoption("1", "Luna", "Ana", ADOPTED_AT).key())
                .isEqualTo(new Adoption("1", "Luna", "Ana", ADOPTED_AT).key())
                .isNotEqualTo(new Adoption("1", "Luna", "Ana", ADOPTED_AT.plusSeconds(1)).key());
    }

    @Test
    void anIncompleteAdoptionIsRejected() {
        assertThatThrownBy(() -> new Adoption("1", "Luna", "  ", ADOPTED_AT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Adoption("1", "Luna", "Ana", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void anEmailAddressMustLookLikeOne() {
        assertThatThrownBy(() -> new EmailAddress("refugio")).isInstanceOf(IllegalArgumentException.class);
    }
}
