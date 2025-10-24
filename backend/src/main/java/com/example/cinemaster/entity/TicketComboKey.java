package com.example.cinemaster.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;


import java.io.Serializable;


@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TicketComboKey implements Serializable {


    @Column(name = "TicketID")
    private Integer ticketId;


    @Column(name = "ComboID")
    private Integer comboId;
}

