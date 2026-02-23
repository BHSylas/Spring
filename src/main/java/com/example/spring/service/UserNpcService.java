package com.example.spring.service;

import com.example.spring.common.exception.NotFoundException;
import com.example.spring.dto.NPCConversationResponseDTO;
import com.example.spring.dto.NpcProgressResponseDTO;
import com.example.spring.dto.UserNpcConversationResponseDTO;
import com.example.spring.entity.*;
import com.example.spring.repository.NpcConversationRepository;
import com.example.spring.repository.UserNpcAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNpcService {
    private final NpcConversationRepository npcConversationRepository;
    private final UserNpcAnswerRepository userNpcAnswerRepository;

    // =========================================================
    // 학습 시작: 필터 조건에서 미풀이 첫 번째 NPC 반환
    // GET /api/user/npc/start
    // =========================================================

    /**
     * country / place / level 필터로 전체 NPC 중
     * 아직 시도하지 않은 첫 번째 문제를 반환.
     * 모두 풀었으면 첫 번째 문제를 반환.
     */

    public UserNpcConversationResponseDTO start(Long userId, Country country, Place place, Level level) {
        List<NPCConversation> candidates = npcConversationRepository.findAllActiveWithFilter(country, place, level);

        if(candidates.isEmpty()) {
            throw new NotFoundException("해당 조건에 맞는 NPC대화가 없습니다.");
        }

        Map<Long, UserNpcAnswer> answerMap = buildAnswerMap(userId, candidates);

        NPCConversation target = candidates.stream().filter(npc -> !answerMap.containsKey(npc.getNpcId())).findFirst().orElse(candidates.get(0));

        return toUserResponse(target, answerMap.get(target.getNpcId()));

    }
    // =========================================================
    // 특정 NPC 대화 단건 조회
    // GET /api/user/npc/conversation/{conversationId}
    // =========================================================

    public UserNpcConversationResponseDTO getConversation(Long userId, Long conversationId) {
        NPCConversation npc = npcConversationRepository.findById(conversationId).
                orElseThrow(() -> new NotFoundException("NPC 대화를  찾을 수 없습니다."));

        if(!npc.isActive()){
            throw new NotFoundException("비활성화된 NPC 대화입니다.");
        }

        UserNpcAnswer record = userNpcAnswerRepository.findByUserUserIdAndNpcConversation(userId, npc).orElse(null);

        return toUserResponse(npc, record);
    }

    // =========================================================
    // 전체 NPC 진행 현황 조회
    // GET /api/user/npc/progress
    // =========================================================

    /**
     * 국가 → 레벨 → 장소 3단계 계층 구조로 진행 현황 반환.
     * country / level / place 필터 모두 선택적.
     */

    public NpcProgressResponseDTO getProgress(Long userId, Country country, Level level, Place place) {
        List<NPCConversation> allNpcs = npcConversationRepository.findAllActiveWithFilter(country, place, level);

        if(allNpcs.isEmpty()) {
            return NpcProgressResponseDTO.builder()
                    .totalCount(0)
                    .solvedCount(0)
                    .correctCount(0)
                    .progressRate(0.0).correctRate(0.0)
                    .countries(List.of())
                    .build();
        }
        Map<Long, UserNpcAnswer> answerMap = buildAnswerMap(userId, allNpcs);

        long totalCount = allNpcs.size();
        long solvedCount = answerMap.size();
        long correctCount = answerMap.values().stream().filter(UserNpcAnswer::isCorrect).count();

        List<NpcProgressResponseDTO.NpcCountryProgressDTO> countries = groupByCountry(allNpcs, answerMap);

        return NpcProgressResponseDTO.builder()
                .totalCount(totalCount)
                .solvedCount(solvedCount)
                .correctCount(correctCount)
                .progressRate(calcRate(solvedCount, totalCount))
                .correctRate(calcRate(correctCount, totalCount))
                .countries(countries)
                .build();
    }

    //내부 함수
    /** NPC 목록에 해당하는 사용자 답변 기록을 Map으로 한 번에 조회 */
    private Map<Long, UserNpcAnswer> buildAnswerMap(Long userId, List<NPCConversation> npcs) {
        if (npcs.isEmpty()) return Map.of();

        List<Long> npcIds = npcs.stream().map(NPCConversation::getNpcId).toList();

        return userNpcAnswerRepository.findByUserUserIdAndNpcConversationNpcIdIn(userId, npcIds)
                .stream().collect(Collectors.toMap(a -> a.getNpcConversation().getNpcId(),
                        a ->a));
    }

    /** 국가별 그룹 → 레벨별 → 장소별 중첩 구조 생성 */
    private List<NpcProgressResponseDTO.NpcCountryProgressDTO> groupByCountry(
            List<NPCConversation> npcs, Map<Long, UserNpcAnswer> answerMap
    ){
        return npcs.stream().collect(Collectors.groupingBy(NPCConversation::getCountry))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                .map(e -> {
                    Country c = e.getKey();
                    List<NPCConversation> cNpcs = e.getValue();

                    long cTotal = cNpcs.size();
                    long cSolved = cNpcs.stream().filter(n -> answerMap.containsKey(n.getNpcId())).count();
                    long cCorrect = cNpcs.stream().filter(n -> answerMap.containsKey(n.getNpcId()) &&
                            answerMap.get(n.getNpcId()).isCorrect()).count();

                    return NpcProgressResponseDTO.NpcCountryProgressDTO.builder()
                            .country(c)
                            .totalCount(cTotal)
                            .solvedCount(cSolved)
                            .correctCount(cCorrect)
                            .progressRate(calcRate(cSolved, cTotal))
                            .correctRate(calcRate(cCorrect, cTotal))
                            .levels(groupByLevel(cNpcs, answerMap))
                            .build();
                }).toList();
    }


    /** 레벨별 그룹 → 장소별 */
    private List<NpcProgressResponseDTO.NpcLevelProgressDTO> groupByLevel(
            List<NPCConversation> npcs,
            Map<Long, UserNpcAnswer> answerMap
    ) {
        return npcs.stream()
                .collect(Collectors.groupingBy(NPCConversation::getLevel))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                .map(e -> {
                    Level l = e.getKey();
                    List<NPCConversation> lNpcs = e.getValue();

                    long lTotal   = lNpcs.size();
                    long lSolved  = lNpcs.stream().filter(n -> answerMap.containsKey(n.getNpcId())).count();
                    long lCorrect = lNpcs.stream()
                            .filter(n -> answerMap.containsKey(n.getNpcId()) && answerMap.get(n.getNpcId()).isCorrect())
                            .count();

                    return NpcProgressResponseDTO.NpcLevelProgressDTO.builder()
                            .level(l)
                            .totalCount(lTotal)
                            .solvedCount(lSolved)
                            .correctCount(lCorrect)
                            .progressRate(calcRate(lSolved, lTotal))
                            .correctRate(calcRate(lCorrect, lTotal))
                            .places(groupByPlace(lNpcs, answerMap))
                            .build();
                })
                .toList();
    }

    /** 장소별 그룹 → NPC 아이템 */
    private List<NpcProgressResponseDTO.NpcPlaceProgressDTO> groupByPlace(
            List<NPCConversation> npcs,
            Map<Long, UserNpcAnswer> answerMap
    ) {
        return npcs.stream()
                .collect(Collectors.groupingBy(NPCConversation::getPlace))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                .map(e -> {
                    Place p = e.getKey();
                    List<NPCConversation> pNpcs = e.getValue();

                    long pTotal   = pNpcs.size();
                    long pSolved  = pNpcs.stream().filter(n -> answerMap.containsKey(n.getNpcId())).count();
                    long pCorrect = pNpcs.stream()
                            .filter(n -> answerMap.containsKey(n.getNpcId()) && answerMap.get(n.getNpcId()).isCorrect())
                            .count();

                    List<NpcProgressResponseDTO.NpcItemStatusDTO> items = pNpcs.stream()
                            .map(npc -> {
                                UserNpcAnswer ans = answerMap.get(npc.getNpcId());
                                return NpcProgressResponseDTO.NpcItemStatusDTO.builder()
                                        .conversationId(npc.getNpcId())
                                        .topic(npc.getTopic())
                                        .attempted(ans != null)
                                        .correct(ans != null && ans.isCorrect())
                                        .locked(ans != null && ans.isLocked())
                                        .attempts(ans != null ? ans.getAttempts() : 0)
                                        .nextConversationId(npc.getNextConversationId())
                                        .build();
                            })
                            .toList();

                    return NpcProgressResponseDTO.NpcPlaceProgressDTO.builder()
                            .place(p)
                            .totalCount(pTotal)
                            .solvedCount(pSolved)
                            .correctCount(pCorrect)
                            .progressRate(calcRate(pSolved, pTotal))
                            .correctRate(calcRate(pCorrect, pTotal))
                            .items(items)
                            .build();
                })
                .toList();
    }

    /**
     * NPCConversation → 학생 응답 DTO
     *
     * 레벨별 options 노출 규칙:
     *   BEGINNER     - 빈칸 정답 선택지 목록 노출 (단어 하나 선택)
     *   INTERMEDIATE - 문장 조합용 단어 선택지 목록 노출 (단어들을 순서대로 조합)
     *   ADVANCED     - options null (사용자가 직접 문장 입력)
     *
     * 정답/해설은 locked 상태일 때만 노출
     */
    private UserNpcConversationResponseDTO toUserResponse(NPCConversation npc, UserNpcAnswer record) {
        boolean attempted = record != null;
        boolean locked    = attempted && record.isLocked();
        boolean correct   = attempted && record.isCorrect();
        int     attempts  = attempted ? record.getAttempts() : 0;

        // ADVANCED는 직접 입력이므로 선택지 불필요
        List<String> options = switch (npc.getLevel()) {
            case BEGINNER, INTERMEDIATE -> npc.getOptions();
            case ADVANCED               -> null;
        };

        return UserNpcConversationResponseDTO.builder()
                .conversationId(npc.getNpcId())
                .country(npc.getCountry())
                .place(npc.getPlace())
                .level(npc.getLevel())
                .topic(npc.getTopic())
                .npcScript(npc.getNpcScript())
                .question(npc.getQuestion())
                .options(options)
                .attempted(attempted)
                .correct(correct)
                .locked(locked)
                .attempts(attempts)
                .explanation(locked ? npc.getExplanation() : null)
                .correctAnswer(locked ? npc.getAnswers() : null)
                .nextConversationId(npc.getNextConversationId())
                .build();

    }

    private double calcRate(long part, long total) {
        if (total == 0) return 0.0;
        return Math.round((part * 100.0) / total);
    }
}
