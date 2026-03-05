package com.example.spring.service;

import com.example.spring.dto.BoardRequestDTO;
import com.example.spring.dto.BoardResponseDTO;
import com.example.spring.entity.*;
import com.example.spring.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final LectureRepository lectureRepository;

    @Transactional
    public Long create(Long userId, BoardRequestDTO requestDTO){
        Long lectureId = requestDTO.getLectureId();
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("회원 정보 없음"));
        Lecture lecture = lectureRepository.findById(lectureId).orElse(null)

                ;
        validateWriterPermission(user, requestDTO.getBoardType());

        Board board = Board.builder()
                .boardType(requestDTO.getBoardType())
                .lecture(lecture)
                .writer(user)
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .answered(false)
                .pinned(false)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        boardRepository.save(board);

        return board.getBoardId();

    }
    @Transactional
    public void update(Long userId, Long boardId, BoardRequestDTO requestDTO){
        Board board = getBoard(boardId);
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("회원 정보 없음"));
        UserRole role = UserRole.fromCode(user.getUserRole());

        if(!board.getWriter().getUserId().equals(userId) &&  role != UserRole.ADMIN){
            throw new IllegalStateException("수정 권한 없음");
        }

        board.setTitle(requestDTO.getTitle());
        board.setContent(requestDTO.getContent());

        if( role == UserRole.ADMIN ){
            board.setPinned(requestDTO.isPinned());
        }
        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);
    }

    public void delete(Long userId, Long boardId){
        Board board = getBoard(boardId);
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("회원 정보 없음"));
        UserRole role = UserRole.fromCode(user.getUserRole());

        if(!board.getWriter().getUserId().equals(userId) &&  role != UserRole.ADMIN){
            throw new IllegalStateException("삭제 권한 없음");
        }

        board.setDeleted(true);
        boardRepository.save(board);
    }

    @Transactional(readOnly = true)
    public Page<BoardResponseDTO> searchBoard(BoardType boardType, Long lectureId,
                                              String writerName, String title, Pageable pageable){
        return boardRepository.searchBoard(boardType, lectureId, writerName, title, pageable)
                .map(this::toDTO);
    }

    @Transactional
    public BoardResponseDTO board(Long boardId){
        Board board = getBoard(boardId);

        board.increaseViewCount();

        return toDTO(board);
    }

    public void markAnswered(Long userId, Long boardId){
        Board board = getBoard(boardId);
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("회원 정보 없음"));
        UserRole role = UserRole.fromCode(user.getUserRole());

        if(role != UserRole.PROFESSOR && role != UserRole.ADMIN){
            throw new IllegalStateException("답변 처리 권한 없음");
        }

        board.setAnswered(true);
    }

    //관리자 공지 고정
    @Transactional
    public void pinNotice(Long userId, Long boardId, boolean pinned){
        User user = userRepository.findById(userId).orElseThrow(() ->  new IllegalArgumentException("회원 정보 없음"));

        if (user.getUserRole() != 2) {
            throw new IllegalStateException("공지 고정 권한 없음. 관리자만 가능");

        }

        Board board = getBoard(boardId);

        if(board.getBoardType() != BoardType.NOTICE){
            throw new IllegalStateException("공지사항만 고정 가능");
        }

        if(board.isDeleted()){
            throw new IllegalStateException("삭제된 공지는 고정할 수 없음");
        }
        board.setPinned(pinned);
        board.setUpdatedAt(LocalDateTime.now());

    }

    private Board getBoard(Long boardId){
        return boardRepository.findByBoardIdAndDeletedFalse(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

    }

    private void validateWriterPermission(User user, BoardType boardType){
        UserRole role = UserRole.fromCode(user.getUserRole());
        if(boardType == BoardType.NOTICE ||  boardType == BoardType.MANUAL ||  boardType == BoardType.FAQ ){
            if(role != UserRole.ADMIN){
                throw new IllegalStateException("작성 권한 없음. 관리자만 작성 가능");
            }
        }

    }

    private BoardResponseDTO toDTO(Board board) {
        long commentCount = boardCommentRepository.countByBoardBoardIdAndDeletedFalse(board.getBoardId());
        
        return BoardResponseDTO.builder()
                .boardId(board.getBoardId())
                .writer(board.getWriter().getUserId())
                .boardType(board.getBoardType())
                .lectureId(board.getLecture() != null ? board.getLecture().getLectureId() : null)
                .title(board.getTitle())
                .content(board.getContent())
                .writerName(board.getWriter().getUserNickname())
                .answered(board.isAnswered())
                .pinned(board.isPinned())
                .viewCount(board.getViewCount())
                .commentCount((int) commentCount)
                .createdAt(board.getCreatedAt())
                .build();
    }


}
