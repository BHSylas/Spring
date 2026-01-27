package com.example.spring.repository;

import com.example.spring.entity.Country;
import com.example.spring.entity.Level;
import com.example.spring.entity.NPCConversation;
import com.example.spring.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NpcConversationRepository extends JpaRepository<NPCConversation, Long> {
    List<NPCConversation> findByCountryAndLevelAndPlaceOrderBySequence(Country country, Level level, Place place);
}
