package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<Option, Integer> {

  List<Option> findByQuestion_IdOrderByOrderIndexAsc(Integer questionId);

  boolean existsByIdAndQuestion_Id(
          Integer optionId,
          Integer questionId
  );

  void deleteByQuestion_Id(Integer questionId);
}