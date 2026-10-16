package com.trinhcong1120.survey_service;

import static org.junit.jupiter.api.Assertions.*;

import com.trinhcong1120.survey_service.entity.Condition;
import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Question;
import com.trinhcong1120.survey_service.util.ConditionUtil;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConditionScheduleTest {
  private Question question(Page page) {
    Question q = new Question();
    q.setId(UUID.randomUUID());
    q.setPage(page);
    return q;
  }

  private Condition show(Question source, Question target) {
    Condition c = new Condition();
    c.setSourceQuestion(source);
    c.setTargetQuestion(target);
    c.setAction("SHOW");
    return c;
  }

  @Test
  void hiddenSourceHidesUnreachableShowChainButAlternateOpenSourceKeepsTarget() {
    Page closed = new Page(); closed.setId(UUID.randomUUID());
    Page open = new Page(); open.setId(UUID.randomUUID());
    Question hidden = question(closed), target = question(open), downstream = question(open);
    Question alternative = question(open);
    Set<UUID> excluded = ConditionUtil.unreachableShowTargets(
        List.of(show(hidden, target), show(target, downstream)), Set.of(open.getId()));
    assertEquals(Set.of(target.getId(), downstream.getId()), excluded);
    assertTrue(ConditionUtil.unreachableShowTargets(
        List.of(show(hidden, target), show(alternative, target)), Set.of(open.getId())).isEmpty());
  }
}
