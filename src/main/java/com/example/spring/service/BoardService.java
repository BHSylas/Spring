package com.example.spring.service;

import com.example.spring.dto.BoardRequestDTO;
import com.example.spring.dto.BoardResponseDTO;
import com.example.spring.entity.Board;
import com.example.spring.entity.BoardType;
import com.example.spring.entity.Lecture;
import com.example.spring.entity.User;
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

    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final BoardCommentRepository boardCommentRepository;

    @Transactional
    public Long create(Long userId, BoardRequestDTO requestDTO){
        Lecture lecture = null;
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("회원 정보 없음"));

        if(requestDTO.getBoardType() == BoardType.LECTURE_QNA){
            lecture = lectureRepository.findById(requestDTO.getLectureId())
                    .orElseThrow(() -> new IllegalArgumentException("강의 없음"));

            boolean enrolled = enrollmentRepository.existsByUser_UserIdAndLecture_LectureId(userId, requestDTO.getLectureId());

            if(!enrolled){
                throw new IllegalStateException("수강생만 강의 Q&A 작성 가능");
            }
        }

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

        if(!board.getWriter().getUserId().equals(userId) &&  user.getUserRole() != 2){
            throw new IllegalStateException("수정 권한 없음");
        }

        board.setTitle(requestDTO.getTitle());
        board.setContent(requestDTO.getContent());

        if( user.getUserRole() == 2){
            board.setPinned(requestDTO.isPinned());
        }
        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);
    }

    public void delete(Long userId, Long boardId){
        Board board = getBoard(boardId);
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("회원 정보 없음"));

        if(!board.getWriter().getUserId().equals(userId) &&  user.getUserRole() != 2){
            throw new IllegalStateException("삭제 권한 없음");
        }

        board.setDeleted(true);
        boardRepository.save(board);
    }

//    @Transactional(readOnly = true)
//    public List<BoardResponseDTO> boardList(BoardType boardType, Long lectureId){
//        List<Board> boards;
//
//        if(boardType == BoardType.LECTURE_QNA){
//            Lecture lecture = lectureRepository.findById(lectureId)
//                    .orElseThrow(() -> new IllegalArgumentException("강좌 없음"));
//
//            boards = boardRepository.findByBoardTypeAndLectureAndDeletedFalseOrderByCreatedAtDesc(boardType, lecture);
//        }else{
//            boards =  boardRepository.findByBoardTypeAndDeletedFalseOrderByPinnedDescCreatedAtDesc(boardType);
//        }
//        return boards.stream().map(this::toDTO).toList();
//
//    }

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

        if(user.getUserRole() != 1 && user.getUserRole() != 2){
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

        if(board.getBoardType() == BoardType.NOTICE){
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
        if(boardType == BoardType.NOTICE ||  boardType == BoardType.MANUAL ||  boardType == BoardType.FAQ ){
            if(user.getUserRole() != 2){
                throw new IllegalStateException("작성 권한 없음. 관리자만 작성 가능");
            }
        }

        if(boardType == BoardType.LECTURE_QNA){
            if(user.getUserRole() != 1){
                throw new IllegalStateException("작성 권한 없음. 교수만 작성 가능");
            }
        }
    }

    private BoardResponseDTO toDTO(Board board) {
        long commentCount = boardCommentRepository.countByBoardBoardIdAndDeletedFalse(board.getBoardId());
        
        return BoardResponseDTO.builder()
                .boardId(board.getBoardId())
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
