package com.trinhcong1120.survey_service.dto.statistics;

import java.util.List;

public class TextStatisticsResponse {

  private Long count;
  private List<TextItem> topAnswers;

  public TextStatisticsResponse() {
  }

  public TextStatisticsResponse(
          Long count,
          List<TextItem> topAnswers
  ) {
    this.count = count;
    this.topAnswers = topAnswers;
  }

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

  public List<TextItem> getTopAnswers() {
    return topAnswers;
  }

  public void setTopAnswers(List<TextItem> topAnswers) {
    this.topAnswers = topAnswers;
  }

  public static class TextItem {

    private String text;
    private Long count;

    public TextItem() {
    }

    public TextItem(
            String text,
            Long count
    ) {
      this.text = text;
      this.count = count;
    }

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }

    public Long getCount() {
      return count;
    }

    public void setCount(Long count) {
      this.count = count;
    }
  }
}