package com.coperatecoding.secodeverseback.controller;

import com.coperatecoding.secodeverseback.domain.User;
import com.coperatecoding.secodeverseback.domain.board.Board;
import com.coperatecoding.secodeverseback.dto.*;
import com.coperatecoding.secodeverseback.exception.CategoryNotFoundException;
import com.coperatecoding.secodeverseback.exception.ForbiddenException;
import com.coperatecoding.secodeverseback.exception.NotFoundException;
import com.coperatecoding.secodeverseback.service.BoardImgService;
import com.coperatecoding.secodeverseback.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@Tag(name = "게시글", description = "게시글 관련 API")
@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/v1/board")
public class BoardController {

    private final BoardService boardService;
    private final BoardImgService boardImgService;

    @Operation(summary = "게시글 작성", description = """
    [로그인 필요]
    일단 지금은 이미지 제외함. s3 비용때문에 최대한 나중에 구현 예정
    201: 성공<br>
    400: 필요한 값을 넣지 않음(모든 값은 not null)<br>
    403: 권한없음
    """)
    @PostMapping("")
    public ResponseEntity makeBoard(@AuthenticationPrincipal User user, @RequestBody @Valid BoardAndImageDTO.AddBoardAndImageRequest addBoardAndImageRequest) {
        Board board = boardService.makeBoard(user, addBoardAndImageRequest.getBoard());

        for (BoardImgDTO.AddBoardImgRequest image : addBoardAndImageRequest.getImgList())
        {
            if(image != null)
                boardImgService.makeBoardImage(board.getPk(), image);
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


//    @Operation(summary = "게시글 목록 조회", description = """
//    [모두 접근가능]
//    sort: 1. POP(인기순) 2. NEW(최신순) 3. COMMENT(댓글순) <br>
//    200: 성공<br>
//    """)
//    @Parameters({
//            @Parameter(name = "categoryPk", description = "카테고리의 pk (nullable)"),
//            @Parameter(name = "q", description = "검색어 (nullable)"),
//            @Parameter(name = "pageSize", description = "페이지 크기(1보다 커야함)"),
//            @Parameter(name = "page", description = "페이지(0보다 커야함)"),
//            @Parameter(name = "sort", description = "정렬기준(최신, 인기, 댓글)")
//    })
//    @GetMapping("")
//    public ResponseEntity<BoardDTO.SearchListResponse> getBoardList(
//            @RequestParam(required = false) Long categoryPk,
//            @RequestParam(required = false) String q,
//            @RequestParam(required = false, defaultValue = "10") @Min(value = 2, message = "page 크기는 1보다 커야합니다") int pageSize,
//            @RequestParam(required = false, defaultValue = "1") @Min(value = 1, message = "page는 0보다 커야합니다") int page,
//            @RequestParam(required = false, defaultValue = "RECENT") QuestionSortType sort
//    ) throws CategoryNotFoundException {
//        BoardDTO.SearchListResponse boardList = boardService.getBoardList(categoryPk, q, page, pageSize, sort);
//
//        return ResponseEntity.ok(boardList);
//    }

    @Operation(summary = "게시글 목록 조회", description = """
    [모두 접근가능]
    sort: 1. POP(인기순) 2. NEW(최신순) 3. COMMENT(댓글순) <br>
    200: 성공<br>
    """)
    @GetMapping("")
    public ResponseEntity<BoardDTO.SearchListResponse> getBoardList(
            @RequestParam(required = false) Long categoryPk,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "10") @Min(value = 2, message = "page 크기는 1보다 커야합니다") int pageSize,
            @RequestParam(required = false, defaultValue = "1") @Min(value = 1, message = "page는 0보다 커야합니다") int page,
            @RequestParam(required = false, defaultValue = "RECENT") BoardSortType sort
    ) throws CategoryNotFoundException {
        Page<BoardDTO.SearchResponse> boardPage = boardService.getBoardList(categoryPk, q, page, pageSize, sort);

        BoardDTO.SearchListResponse response = BoardDTO.SearchListResponse.builder()
                .cnt((int) boardPage.getTotalElements())
                .list(boardPage.getContent())
                .build();

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "게시글 상세 조회", description = """
    [모두 접근가능]<br>
    200: 성공<br>
    404: 해당하는 pk의 게시글이 없음
    """)
    @GetMapping("/{boardPk}")
    public ResponseEntity<BoardAndImageDTO.DetailResponse> getBoard(@PathVariable Long boardPk) throws NoSuchElementException {

        BoardDTO.BoardDetailResponse board = boardService.getDetailBoard(boardPk);
        List<BoardImgDTO.SearchResponse> imgs = boardImgService.getBoardImg(boardPk);
        BoardAndImageDTO.DetailResponse response = new BoardAndImageDTO.DetailResponse();

        response.setBoard(board);
        response.setImgList(imgs);

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "게시글 수정", description = """
      [로그인 필요] 작성자 or 관리자만 게시글을 수정 가능<br>
      null로 들어오면 해당 값은 수정되지 않음<br>
      200: 성공<br>
      403: 수정할 권한 없음<br>
      404: 해당하는 pk의 게시글이 없음
      """)
    @PatchMapping("/{boardPk}")
    public ResponseEntity editBoard(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardPk,
            @RequestBody BoardDTO.AddBoardRequest addBoardRequest
    ) throws NoSuchElementException, ForbiddenException {

        boardService.editBoard(user, boardPk, addBoardRequest);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "게시글 삭제", description = """
      [로그인 필요] 작성자 or 관리자만 정보글을 삭제 가능<br>
      200: 성공<br>
      403: 수정할 권한 없음<br>
      404: 해당하는 pk의 게시글이 없음
      """)
    @DeleteMapping("/{boardPk}")
    public ResponseEntity deleteBoard(@AuthenticationPrincipal User user, @PathVariable Long boardPk) throws NoSuchElementException, ForbiddenException {
        try{
            List<BoardImgDTO.SearchResponse> imgDTOS = boardImgService.getBoardImg(boardPk);

            for (BoardImgDTO.SearchResponse img : imgDTOS) {
                boardImgService.delete(img.getPk());
            }
            boardService.deleteBoard(user, boardPk);
            return ResponseEntity.noContent().build();
        }
        catch(NotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "인기 게시글 조회", description = """
        [모두 접근가능] 인기 게시글을 조회합니다.<br>
        200: 성공
        """)
    @GetMapping("/boards/popular")
    public ResponseEntity<BoardDTO.PopularBoardListResponse> getPopularBoardList() {

        List<BoardDTO.PopularBoardResponse> popularBoardList = boardService.getPopularBoardList();

        BoardDTO.PopularBoardListResponse response = BoardDTO.PopularBoardListResponse.builder()
                .cnt(popularBoardList.size())
                .list(popularBoardList)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내가 쓴 게시글 조회", description = """
    [로그인 필요] 내가 쓴 글 모두 조회<br>
    200: 성공<br>
    403: 로그인 필요
    """)
    @GetMapping("/my")
    public ResponseEntity<BoardDTO.SearchListResponse> getMyBoardList(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "1") @Min(value = 1, message = "page는 1보다 커야합니다") int page,
            @RequestParam(required = false, defaultValue = "10") @Min(value = 1, message = "pageSize는 1보다 커야합니다") int pageSize) {

        Page<BoardDTO.SearchResponse> boardPage = boardService.getMyBoardList(user, page, pageSize);

        BoardDTO.SearchListResponse response = BoardDTO.SearchListResponse.builder()
                .cnt((int) boardPage.getTotalElements())
                .list(boardPage.getContent())
                .build();

        return ResponseEntity.ok(response);
    }




}