package com.example.spring.service;

import com.example.spring.dto.NPCConversationRequestDTO;
import com.example.spring.dto.NPCConversationResponseDTO;
import com.example.spring.entity.*;
import com.example.spring.repository.LectureRepository;
import com.example.spring.repository.NpcConversationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;


@Service
@RequiredArgsConstructor
public class NpcConversationService {

    private final NpcConversationRepository conversationRepository;
    private final LectureRepository lectureRepository;

    @Transactional
    public void create(Long professorId, NPCConversationRequestDTO requestDTO){
        Lecture lecture = lectureRepository.findById(requestDTO.getLectureId())
                .orElseThrow(() -> new IllegalArgumentException("강의 없음"));

        validateLectureOwner(lecture, professorId);

        NPCConversation npc = NPCConversation.builder()
                .professor(lecture.getProfessor())
                .lecture(lecture)
                .country(requestDTO.getCountry())
                .place(requestDTO.getPlace())
                .level(requestDTO.getLevel())
                .npcScript(requestDTO.getNpcScript())
                .question(requestDTO.getQuestion())
                .options(requestDTO.getOptions())
                .answers(requestDTO.getAnswers())
                .explanation(requestDTO.getExplanation())
                .topic(requestDTO.getTopic())
                .active(true)
                .build();

        conversationRepository.save(npc);

        if (requestDTO.getNextConversationId() != null) {
            setNextConversation(npc, requestDTO.getNextConversationId());
        }

    }

    @Transactional
    public void update(Long id, Long professorId, NPCConversationRequestDTO requestDTO){
        NPCConversation npc = getOwnedNpc(id, professorId);

        npc.update(
                requestDTO.getNpcScript(),
                requestDTO.getQuestion(),
                requestDTO.getOptions(),
                requestDTO.getAnswers(),
                requestDTO.getExplanation(),
                requestDTO.getTopic());

        if (requestDTO.getNextConversationId() != null) {
            setNextConversation(npc, requestDTO.getNextConversationId());
        } else {
            npc.setNextConversationId(null);
        }
    }

    @Transactional
    public void deactivate(Long id, Long professorId){
        NPCConversation npc = getOwnedNpc(id, professorId);
        npc.deactivate();
    }

    @Transactional(readOnly = true)
    public List<NPCConversationResponseDTO> list(Long professorId, Country country, Place place, Level level) {
        return conversationRepository
                .findByProfessorWithFilter(professorId, country, level, place)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NPCConversationResponseDTO detailList(Long id, Long professorId){
        NPCConversation npc = conversationRepository.findByIdAndProfessorUserId(id, professorId)
                .orElseThrow(() -> new IllegalArgumentException("NPC 대화 없음 또는 권한 없음"));

        return toResponse(npc);
    }

    //다음 대화 후보목록 조회
    @Transactional(readOnly = true)
    public List<NPCConversationResponseDTO> getNextCandidate(Long professorId, Long id){
        NPCConversation current = conversationRepository.findByIdAndProfessorUserId(id, professorId)
                .orElseThrow(() -> new IllegalArgumentException("NPC대화 없음"));

        return conversationRepository.findNextCandidate(
                current.getLecture().getId(),
                current.getCountry(),
                current.getPlace(),
                current.getLevel(),
                current.getId()
                ).stream()
                .map(n -> NPCConversationResponseDTO.builder()
                        .id(n.getId())
                        .topic(n.getTopic())
                        .build()).toList();

    }

    //다음대화 선택
    @Transactional
    public void connectNextConversation(Long professorId, Long currentId, Long nextConversationId) {
        NPCConversation current = getOwnedNpc(currentId, professorId);
        setNextConversation(current, nextConversationId);
    }

    private void setNextConversation(NPCConversation current, Long nextId) {

        if (nextId == null) {
            current.setNextConversationId(null);
            return;
        }

        if (current.getId().equals(nextId)) {
            throw new IllegalArgumentException("자기 자신은 다음 대화로 설정 불가");
        }

        NPCConversation next = conversationRepository.findById(nextId)
                .orElseThrow(() -> new IllegalArgumentException("다음 NPC 대화 없음"));

        if (!current.getLecture().getId().equals(next.getLecture().getId())) {
            throw new IllegalStateException("같은 강의 내 NPC 대화만 연결 가능");
        }

        current.setNextConversationId(nextId);
    }


    private NPCConversation getOwnedNpc(Long npcId, Long professorId) {
        return conversationRepository
                .findByIdAndProfessorUserId(npcId, professorId)
                .orElseThrow(() -> new IllegalArgumentException("NPC 대화 없음 또는 권한 없음"));
    }

    private void validateLectureOwner(Lecture lecture, Long professorId) {
        if (!lecture.getProfessor().getUserId().equals(professorId)) {
            throw new IllegalStateException("본인 강의에만 NPC 대화 등록 가능");
        }
    }

    private NPCConversationResponseDTO toResponse(NPCConversation npc) {
        return NPCConversationResponseDTO.builder()
                .id(npc.getId())
                .professorId(npc.getProfessor().getUserId())
                .lectureId(npc.getLecture().getId())
                .lectureTitle(npc.getLecture().getTitle())
                .country(npc.getCountry())
                .place(npc.getPlace())
                .level(npc.getLevel())
                .npcScript(npc.getNpcScript())
                .question(npc.getQuestion())
                .options(npc.getOptions())
                .answers(npc.getAnswers())
                .explanation(npc.getExplanation())
                .topic(npc.getTopic())
                .nextConversationId(npc.getNextConversationId())
                .active(npc.isActive())
                .build();
    }

}
