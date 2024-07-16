package org.example.dao;

import org.example.entity.BinaryContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTicketDAO extends JpaRepository<BinaryContent, Long> {
}
