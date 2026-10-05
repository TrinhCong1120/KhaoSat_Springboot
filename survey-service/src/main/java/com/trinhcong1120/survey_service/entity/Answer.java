package com.trinhcong1120.survey_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "answers")
public class Answer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "response_id")
  private Response response;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id")
  private Question question;

  @Column(name = "answer_text", columnDefinition = "TEXT")
  private String answerText;

  @Column(name = "answer_number")
  private BigDecimal answerNumber;

  @Column(name = "answer_date")
  private LocalDateTime answerDate;

  @Column(name = "province_code")
  private String provinceCode;

  @Column(name = "ward_code")
  private String wardCode;

  @Column(name = "province")
  private String province;

  @Column(name = "ward")
  private String ward;

  @Column(name = "address_detail", columnDefinition = "TEXT")
  private String addressDetail;

  @JsonIgnore
  @OneToMany(
          mappedBy = "answer",
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL
  )
  private List<AnswerOption> answerOptions = new ArrayList<>();

  public Answer() {}

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public Response getResponse() { return response; }
  public void setResponse(Response response) { this.response = response; }

  public Question getQuestion() { return question; }
  public void setQuestion(Question question) { this.question = question; }

  public String getAnswerText() { return answerText; }
  public void setAnswerText(String answerText) { this.answerText = answerText; }

  public BigDecimal getAnswerNumber() { return answerNumber; }
  public void setAnswerNumber(BigDecimal answerNumber) { this.answerNumber = answerNumber; }

  public LocalDateTime getAnswerDate() { return answerDate; }
  public void setAnswerDate(LocalDateTime answerDate) { this.answerDate = answerDate; }

  public String getProvinceCode() { return provinceCode; }
  public void setProvinceCode(String provinceCode) { this.provinceCode = provinceCode; }

  public String getWardCode() { return wardCode; }
  public void setWardCode(String wardCode) { this.wardCode = wardCode; }

  public String getProvince() { return province; }
  public void setProvince(String province) { this.province = province; }

  public String getWard() { return ward; }
  public void setWard(String ward) { this.ward = ward; }

  public String getAddressDetail() { return addressDetail; }
  public void setAddressDetail(String addressDetail) { this.addressDetail = addressDetail; }

  public List<AnswerOption> getAnswerOptions() { return answerOptions; }
  public void setAnswerOptions(List<AnswerOption> answerOptions) {
    this.answerOptions = answerOptions;
  }
}
