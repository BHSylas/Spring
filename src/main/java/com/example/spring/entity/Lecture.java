package com.example.spring.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
//@Table(name = "Lecture")


public class Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private  String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;

    @Column(nullable = false)
    private Boolean approved;

}
