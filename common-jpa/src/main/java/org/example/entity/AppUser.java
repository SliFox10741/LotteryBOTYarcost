package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.entity.enums.UserState;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Getter
@Setter
@EqualsAndHashCode(exclude = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long telegramUserId;
    private String username;
    private String name;
    private String phoneNumber;
    @Enumerated(EnumType.STRING)
    private UserState state;
    private ArrayList<String> ticketNumber;
    private LocalDateTime localDateTime;
    private Boolean isActive;
}
