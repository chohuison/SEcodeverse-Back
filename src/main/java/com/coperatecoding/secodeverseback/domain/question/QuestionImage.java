package com.coperatecoding.secodeverseback.domain.question;

import com.coperatecoding.secodeverseback.domain.User;
import com.coperatecoding.secodeverseback.dto.QuestionDTO;
import com.coperatecoding.secodeverseback.dto.QuestionImgDTO;
import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "question_image")
public class QuestionImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_pk", referencedColumnName = "pk")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_pk")
    @JsonIgnore
    private User user;

    @JoinColumn(name = "img_url")
    private String imgUrl;

    public static QuestionImage makeQuestionImg(String imgUrl, Question question){
        QuestionImage questionImage = new QuestionImage();
        questionImage.imgUrl=imgUrl;
        questionImage.question=question;
        return questionImage;
    }


    public void modifyQuestionImg(String imgUrl){
        this.imgUrl=imgUrl;
    }

}
