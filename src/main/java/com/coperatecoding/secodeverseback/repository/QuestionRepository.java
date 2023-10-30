package com.coperatecoding.secodeverseback.repository;

import com.coperatecoding.secodeverseback.domain.Comment;
import com.coperatecoding.secodeverseback.domain.board.Board;
import com.coperatecoding.secodeverseback.domain.question.Level;
import com.coperatecoding.secodeverseback.domain.question.Question;
import com.coperatecoding.secodeverseback.domain.question.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
        List<Question> findByCategory(QuestionCategory questionCategory);
        List<Question> findByLevel(Level level);
        List<Question>findByCategoryAndLevel(QuestionCategory questionCategory, Level level);
}


