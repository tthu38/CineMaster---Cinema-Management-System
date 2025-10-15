package com.example.cinemaster.repository;

import com.example.cinemaster.entity.TicketDiscount;
import com.example.cinemaster.entity.TicketDiscountId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketDiscountRepository extends JpaRepository<TicketDiscount, TicketDiscountId> {
}
