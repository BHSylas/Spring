package com.example.spring.service;

import com.example.spring.dto.BoardAnswerRequestDTO;
import com.example.spring.dto.BoardAnswerResponseDTO;
import com.example.spring.entity.Board;
import com.example.spring.entity.BoardAnswer;
import com.example.spring.entity.BoardType;
import com.example.spring.entity.User;
import com.example.spring.repository.BoardAnswerRepository;
import com.example.spring.repository.BoardRepository;
import com.example.spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardAnswerService {
    private final BoardRepository boardRepository;
    private final BoardAnswerRepository boardAnswerRepository;
    private final UserRepository userRepository;

    public Long createAnswer(Long professorId, Long boardId, BoardAnswerRequestDTO requestDTO){
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("질문 없음"));
        User professor = userRepository.findById(professorId).orElseThrow(() -> new IllegalArgumentException("사용자 정보 없음"));

        if(board.getBoardType() != BoardType.LECTURE_QNA){
            throw new IllegalStateException("강의 Q&A에만 답변 가능");
        }

        if(professor.getUserRole() != 1){
            throw new IllegalStateException("교수만 답변 가능");
        }

        if (!board.getLecture().getProfessor().getUserId().equals(professorId)){
            throw new IllegalStateException("본인 강좌 질문에만 답변 가능");
        }

        BoardAnswer boardAnswer = BoardAnswer.builder()
                .board(board)
                .writer(professor)
                .content(requestDTO.getContent())
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        boardAnswerRepository.save(boardAnswer);

        board.setAnswered(true);
        return board.getBoardId();
    }

    public void deleteAnswer(Long answerId, Long professorId){
        BoardAnswer answer = boardAnswerRepository.findById(answerId).orElseThrow(() ->
                new IllegalArgumentException("답변 없음"));

        if(!answer.getWriter().getUserId().equals(professorId)){
            throw  new IllegalStateException("본인 답변에만 삭제 가능");
        }

        answer.setDeleted(true);

        Board board = answer.getBoard();
        boolean hasActiveAnswer = board.getAnswers().stream().anyMatch(a -> !a.isDeleted());

        if (!hasActiveAnswer){
            board.setAnswered(false);
        }
    }

    @Transactional
    public void updateAnswer(Long answerId, Long professorId, BoardAnswerRequestDTO requestDTO){
        BoardAnswer boardAnswer = boardAnswerRepository.findById(answerId).orElseThrow(() -> new IllegalArgumentException("답변 없음"));
        if(!boardAnswer.getWriter().getUserId().equals(professorId)){
            throw new IllegalStateException("수정 권한 없음");
        }

        if(boardAnswer.isDeleted()){
            throw new IllegalStateException("삭제된 답변은 수정할 수 없습니다.");
        }

        boardAnswer.setContent(requestDTO.getContent());
        boardAnswer.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<BoardAnswerResponseDTO> getAnswers(Long boardId){
        return boardAnswerRepository.findByBoardBoardIdAndDeletedFalse(boardId)
                .stream()
                .map(answer -> BoardAnswerResponseDTO.builder()
                        .answerId(answer.getAnswerId())
                        .writerId(answer.getWriter().getUserId())
                        .writerName(answer.getWriter().getUserName())
                        .content(answer.getContent())
                        .createdAt(answer.getCreatedAt())
                        .build()
                ).toList();
    }
}
