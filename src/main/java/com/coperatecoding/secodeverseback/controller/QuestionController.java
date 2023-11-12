package com.coperatecoding.secodeverseback.controller;

import com.coperatecoding.secodeverseback.domain.User;
import com.coperatecoding.secodeverseback.domain.question.Question;
import com.coperatecoding.secodeverseback.dto.*;
import com.coperatecoding.secodeverseback.exception.NotFoundException;
import com.coperatecoding.secodeverseback.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Tag(name = "문제", description = "문제 관련 API")
@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/v1/question")

public class QuestionController {
    private final QuestionService questionService;
    private final TestCaseService testCaseService;
    private final LevelService levelService;
    private final QuestionCategoryService questionCategoryService;
    private final QuestionImgService questionImgService;
    private final CodeService codeService;
    @PostMapping("/post")
    public ResponseEntity makeQuestion(@AuthenticationPrincipal User user, @RequestBody QuestionAndTestAndImageDTO.AddQuestionAndTestAndImageRequest addQuestionAndTestAndImageRequest) {
        Question question = questionService.makeQuestion(user, addQuestionAndTestAndImageRequest.getQuestion());


        for (TestCaseDTO.AddtestCaseRequest testCase : addQuestionAndTestAndImageRequest.getTestCase()) {
            testCaseService.makeTestCase(question.getPk(), testCase);
        }

        for(QuestionImgDTO.AddQuestionImgRequest questionImg: addQuestionAndTestAndImageRequest.getImg()){
            questionImgService.makeQuestionImg(question.getPk(),questionImg);
        }


        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

//    @PatchMapping("/{questionPk}")
//    public ResponseEntity modifyQuestion(@PathVariable Long questionPk, @RequestBody QuestionAndTestAndImageDTO.AddQuestionAndTestAndImageRequest addQuestionAndTestRequest) {
//        questionService.modifyQuestion(questionPk, addQuestionAndTestRequest.getQuestion());
//        List<TestCaseDTO.SearchResponse> testCaseDTOS = testCaseService.getTestCaseList(questionPk);
//        List<QuestionImgDTO.SearchQuestionImgResponse> questionImgDTOS = questionImgService.getQuestionImg(questionPk);
//        System.out.println("이미지 크기는"+questionImgDTOS.size());
//        int i=0;
//        for (TestCaseDTO.SearchResponse testCase : testCaseDTOS) {
//            testCaseService.modifyTestCase(testCase.getPk(),addQuestionAndTestRequest.getTestCase().get(i));
//            i++;
//        }
//        int j=0;
//        System.out.println(addQuestionAndTestRequest.getImg().size());
//        for(QuestionImgDTO.SearchQuestionImgResponse questionImg: questionImgDTOS){
//            System.out.println(addQuestionAndTestRequest.getImg().get(j).getImgUrl());
//            questionImgService.modifyQuestionImg(questionImg.getPk(),addQuestionAndTestRequest.getImg().get(j));
//            j++;
//        }
//        return ResponseEntity.status(HttpStatus.CREATED).build();
//    }

    @PatchMapping("/{questionPk}")
    public ResponseEntity modifyQuestion(@PathVariable Long questionPk, @RequestBody QuestionAndTestAndImageDTO.AddQuestionAndTestAndImageRequest addQuestionAndTestRequest) {
        questionService.modifyQuestion(questionPk, addQuestionAndTestRequest.getQuestion());
        List<TestCaseDTO.SearchResponse> testCaseDTOS = testCaseService.getTestCaseList(questionPk);
        List<QuestionImgDTO.SearchQuestionImgResponse> questionImgDTOS = questionImgService.getQuestionImg(questionPk);

        // 수정할 테스트케이스
        int i = 0;
        for (TestCaseDTO.SearchResponse testCase : testCaseDTOS) {
            if (i < addQuestionAndTestRequest.getTestCase().size()) {
                testCaseService.modifyTestCase(testCase.getPk(), addQuestionAndTestRequest.getTestCase().get(i));
            } else {
                // 새로운 테스트케이스의 크기를 벗어나면 삭제
                testCaseService.delete(testCase.getPk());
            }
            i++;
        }

        // 수정할 이미지
        int j = 0;
        for (QuestionImgDTO.SearchQuestionImgResponse questionImg : questionImgDTOS) {
            if (j < addQuestionAndTestRequest.getImg().size()) {
                questionImgService.modifyQuestionImg(questionImg.getPk(), addQuestionAndTestRequest.getImg().get(j));
            } else {
                // 새로운 이미지의 크기를 벗어나면 삭제
                questionImgService.delete(questionImg.getPk());
            }
            j++;
        }

        // 나머지 새 이미지 추가
        for (; j < addQuestionAndTestRequest.getImg().size(); j++) {
            questionImgService.makeQuestionImg(questionPk, addQuestionAndTestRequest.getImg().get(j));
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @DeleteMapping("/{questionPk}")
    public ResponseEntity deleteQuestion(@PathVariable Long questionPk){
        try{
            List<QuestionImgDTO.SearchQuestionImgResponse> imgDTOS=questionImgService.getQuestionImg(questionPk);
            for (QuestionImgDTO.SearchQuestionImgResponse img : imgDTOS) {
                questionImgService.delete(img.getPk());
            }
            List<TestCaseDTO.SearchResponse>testCaseDTOS = testCaseService.getTestCaseList(questionPk);
            for (TestCaseDTO.SearchResponse testCase : testCaseDTOS) {
                testCaseService.delete(testCase.getPk());
            }
            questionService.deleteQuestion(questionPk);
            return ResponseEntity.noContent().build();
        }
        catch(NotFoundException e){
            return ResponseEntity.notFound().build();
        }

    }
    @GetMapping("/solve/user={userPk}")
    public ResponseEntity<List<QuestionDTO.questionPagingResponse>> getUserQuestion(@AuthenticationPrincipal User user,
                                                                                    @RequestParam(defaultValue = "1") int page,
                                                                                    @RequestParam(defaultValue = "10") int pageSize){

        List<CodeDTO.PageableCodeListResponse>codes=codeService.getUserCodes(user,page,pageSize);
        List<QuestionDTO.SearchQuestionResponse> questions=new ArrayList<>();
        for(CodeDTO.PageableCodeListResponse code: codes){
            Question question = questionService.findByPk(code.getQuestionPk());
            QuestionDTO.SearchQuestionResponse questionDTO=questionService.getByPk(question);
            questions.add(questionDTO);

        }
        List<QuestionDTO.questionPagingResponse> pagingQuestion = questionService.userPagingQuestion(codes.get(0).getCnt(),questions);

        return ResponseEntity.ok(pagingQuestion);
    }

    @GetMapping("/wrong/user={userPk}")
    public ResponseEntity<List<QuestionDTO.questionPagingResponse>> getWrongQuestion(@AuthenticationPrincipal User user,
                                                                                     @RequestParam(defaultValue = "1") int page,
                                                                                     @RequestParam(defaultValue = "10") int pageSize){

        List<CodeDTO.PageableCodeListResponse>codes=codeService.getWrongCodes(user,page,pageSize);
        List<QuestionDTO.SearchQuestionResponse> questions=new ArrayList<>();
        for(CodeDTO.PageableCodeListResponse code: codes){
            Question question = questionService.findByPk(code.getQuestionPk());
            QuestionDTO.SearchQuestionResponse questionDTO=questionService.getByPk(question);
            questions.add(questionDTO);

        }
        List<QuestionDTO.questionPagingResponse> pagingQuestion = questionService.userPagingQuestion(codes.get(0).getCnt(),questions);


        return ResponseEntity.ok(pagingQuestion);
    }



    @GetMapping("/{questionPk}")
    public ResponseEntity<QuestionAndTestAndImageDTO.QuestionAndTest> detailQuestion(@PathVariable Long questionPk) {

        Question question = questionService.getDetailQuestion(questionPk);
        List<TestCaseDTO.SearchResponse> testCases = testCaseService.getTestCaseList(questionPk);
        List<QuestionImgDTO.SearchQuestionImgResponse> imgs = questionImgService.getQuestionImg(questionPk);
        QuestionAndTestAndImageDTO.QuestionAndTest response = new QuestionAndTestAndImageDTO.QuestionAndTest();
        response.setQuestion(question);
        response.setTestCase(testCases);
        response.setImg(imgs);

        return ResponseEntity.ok(response);

    }

    @GetMapping("post/user={userPk}")
    public ResponseEntity<List<QuestionDTO.questionPagingResponse>> userPostQuestion(@AuthenticationPrincipal User user,
                                                                                     @RequestParam(defaultValue = "1") int page,
                                                                                     @RequestParam(defaultValue = "10") int pageSize){

        List<QuestionDTO.questionPagingResponse> question= questionService.userPostQuestion(user,page,pageSize);
        return ResponseEntity.ok(question);
    }

    @GetMapping("/keyword={keyword}")
    public ResponseEntity<List<QuestionDTO.SearchQuestionResponse>> getKeywordQuestion(@PathVariable String keyword){
        List<QuestionDTO.SearchQuestionResponse> question= questionService.getKeywordQuestion(keyword);
        return ResponseEntity.ok(question);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<QuestionDTO.SearchQuestionResponse>> getRecentQuestion(){
        List<QuestionDTO.SearchQuestionResponse> questions = questionService.getRecentQuestion();
        return ResponseEntity.ok(questions);
    }

    @GetMapping("")
    public ResponseEntity<QuestionDTO.SearchListResponse> getQuestions(
            @RequestParam(required = false, defaultValue = "10") @Min(value = 2, message = "page 크기는 1보다 커야합니다") int pageSize,
            @RequestParam(required = false, defaultValue = "1") @Min(value = 1, message = "page는 0보다 커야합니다") int page,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "sort", required = false) QuestionSortType sort,
            @RequestParam(value = "categoryPk", required = false) List<Long> categoryPks,
            @RequestParam(value = "levelPk", required = false) List<Long> levelPks
    ) {

        Page<QuestionDTO.SearchQuestionResponse> questions = questionService.getQuestionList(page, pageSize, q, sort, categoryPks, levelPks);
        System.out.println(sort);
        System.out.println(categoryPks);
        System.out.println(levelPks);

        QuestionDTO.SearchListResponse response = QuestionDTO.SearchListResponse.builder()
                .cnt((int) questions.getTotalElements())
                .list(questions.getContent())
                .build();

        return ResponseEntity.ok(response);
    }

    //python :28
    //java:4
    //
    //Response body
    //Download
    //{"stdout":"hello World\n","time":"0.052","memory":26928,"stderr":null,"token":"42f06f39-2574-4d3e-9e90-35d33059ab14","compile_output":null,"message":null,"status":{"id":3,"description":"Accepted"}}
    //{"stdout":null,"time":"0.112","memory":33472,"stderr":"  File \"/box/script.py\", line 1\n    cHJpbnQoJ2hlbGxvIFdvcmxkJyk=\n                                ^\nSyntaxError: invalid syntax\n","token":"92e5da6a-a84d-4d83-9bc6-836f56a3258d","compile_output":null,"message":"Exited with error status 1","status":{"id":11,"description":"Runtime Error (NZEC)"}}
    @GetMapping("/solveQuestion")
    public ResponseEntity<String> solveQuestion() throws IOException, InterruptedException {

        String JUDGE0_API_URL = "https://judge0-extra-ce.p.rapidapi.com";
        String RAPIDAPI_HOST = "judge0-extra-ce.p.rapidapi.com";
        String RAPIDAPI_KEY = "ce1aa40351mshd1b8e179b49a600p12e79djsn3c18e4473ece";

        String python3LanguageId = "28";
        String javaLanguageId="4";

        // Send the code execution request
        String code = "print('hello World')";
        String encodedCode = Base64.getEncoder().encodeToString(code.getBytes(StandardCharsets.UTF_8));
        System.out.println(encodedCode);
        String requestBody = "{\"language_id\":\"" + python3LanguageId + "\",\"source_code\":\"" + encodedCode + "\",\"stdin\":null}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(JUDGE0_API_URL + "/submissions"))
                .header("X-RapidAPI-Key", RAPIDAPI_KEY)
                .header("X-RapidAPI-Host", RAPIDAPI_HOST)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();



        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        String input = response.body();
        String token="";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(input);
            token = jsonNode.get("token").asText();

            System.out.println(token);
        } catch (Exception e) {
            e.printStackTrace();
        }


        Thread.sleep(1000);

        HttpRequest resultRequest = HttpRequest.newBuilder()
                .uri(URI.create(JUDGE0_API_URL + "/submissions/"+token))
                .header("X-RapidAPI-Key", RAPIDAPI_KEY)
                .header("X-RapidAPI-Host", RAPIDAPI_HOST)
                .header("Content-Type", "application/json")

                .build();
        HttpResponse<String> resultResponse = client.send(resultRequest, HttpResponse.BodyHandlers.ofString());
        String resultBody = resultResponse.body();

        return ResponseEntity.ok(resultBody);
    }

    /*
            if (sort == sort.RECENT) {
            Page<QuestionDTO.SearchQuestionResponse> tmpQ;
            if(categoryPks == null && levelPks == null){
                tmpQ = questionService.getQuestion(page, pageSize, q, sort, categoryPks, levelPks);
            }
            if (categoryPks != null && !categoryPks.isEmpty() && levelPks != null && !levelPks.isEmpty()) {
                for (Long categoryPk : categoryPks) {
                    for (Long levelPk : levelPks) {
                        Page<QuestionDTO.SearchQuestionResponse> matchingQuestions = questionService.getMatchingQuestions( categoryPk, levelPk);
                        tmpQ.addAll(matchingQuestions);
                    }
                }
            } else if (categoryPks != null && !categoryPks.isEmpty()) {
                for (Long categoryPk : categoryPks) {
                    Page<QuestionDTO.SearchQuestionResponse> categoryQuestions = questionService.getCategoryQuestion( categoryPk);
                    tmpQ.addAll(categoryQuestions);
                }
            } else if (levelPks != null && !levelPks.isEmpty()) {
                for (Long levelPk : levelPks) {
                    Page<QuestionDTO.SearchQuestionResponse> levelQuestions = questionService.getLevelQuestionList( levelPk);
                    tmpQ.addAll(levelQuestions);
                }
            }
            for(int j=tmpQ.size()-1;j>-1;j--){
                questions.add(tmpQ.get(j));
            }
        } else {
            if(categoryPks == null && levelPks == null){
                questions=questionService.getQuestion(page, pageSize, q, sort, categoryPks, levelPks);
            }

            if (categoryPks != null && !categoryPks.isEmpty() && levelPks != null && !levelPks.isEmpty()) {
                for (Long categoryPk : categoryPks) {
                    for (Long levelPk : levelPks) {
                        Page<QuestionDTO.SearchQuestionResponse> matchingQuestions = questionService.getMatchingQuestions( categoryPk, levelPk);
                        questions.addAll(matchingQuestions);
                    }
                }
            } else if (categoryPks != null && !categoryPks.isEmpty()) {
                for (Long categoryPk : categoryPks) {
                    Page<QuestionDTO.SearchQuestionResponse> categoryQuestions = questionService.getCategoryQuestion( categoryPk);
                    questions.addAll(categoryQuestions);
                }
            } else if (levelPks != null && !levelPks.isEmpty()) {
                for (Long levelPk : levelPks) {
                    Page<QuestionDTO.SearchQuestionResponse> levelQuestions = questionService.getLevelQuestionList( levelPk);
                    questions.addAll(levelQuestions);
                }
            }

        }

    * */


}

