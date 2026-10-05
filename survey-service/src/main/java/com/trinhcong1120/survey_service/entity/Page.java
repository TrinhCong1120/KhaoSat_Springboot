package com.trinhcong1120.survey_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pages")
public class Page {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "survey_id")
  private Survey survey;

  @Column(name = "title", length = 500)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "image_url", columnDefinition = "TEXT")
  private String imageUrl;

  @Column(name = "video_url", columnDefinition = "TEXT")
  private String videoUrl;

  @Column(name = "audio_url", columnDefinition = "TEXT")
  private String audioUrl;

  @Column(name = "order_index")
  private Integer orderIndex;

  @JsonIgnore
  @OneToMany(mappedBy = "page", fetch = FetchType.LAZY)
  private List<Question> questions = new ArrayList<>();

  public Page() {}

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public Survey getSurvey() { return survey; }
  public void setSurvey(Survey survey) { this.survey = survey; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

  public String getVideoUrl() { return videoUrl; }
  public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

  public String getAudioUrl() { return audioUrl; }
  public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

  public Integer getOrderIndex() { return orderIndex; }
  public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

  public List<Question> getQuestions() { return questions; }
  public void setQuestions(List<Question> questions) { this.questions = questions; }
}
