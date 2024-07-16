package org.example.entity;
import jakarta.persistence.*;
import lombok.*;
@Getter
@Setter
@EqualsAndHashCode(exclude = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "binary_content")
public class BinaryContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Lob
    @Column(name = "data", columnDefinition = "MEDIUMBLOB")
    private byte[] fileAsArrayOfBytes;

}
