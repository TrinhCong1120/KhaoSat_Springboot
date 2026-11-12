package com.trinhcong1120.survey_service.config;

import com.trinhcong1120.survey_service.entity.QuestionType;
import com.trinhcong1120.survey_service.repository.QuestionTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class QuestionTypeSeedConfig {

    private static final Logger log = LoggerFactory.getLogger(QuestionTypeSeedConfig.class);

    private record SeedItem(String code, String name) {}

    @Bean
    CommandLineRunner seedQuestionTypes(QuestionTypeRepository questionTypeRepository) {
        return args -> {
            List<SeedItem> defaultTypes = List.of(
                    new SeedItem("TEXT", "Văn bản"),
                    new SeedItem("SINGLE_CHOICE", "Chọn một đáp án"),
                    new SeedItem("MULTIPLE_CHOICE", "Chọn nhiều đáp án"),
                    new SeedItem("NUMBER", "Số"),
                    new SeedItem("DATE", "Ngày tháng"),
                    new SeedItem("ADDRESS", "Địa chỉ"),
                    new SeedItem("MATRIX_SINGLE", "Ma trận (1 lựa chọn / hàng)"),
                    new SeedItem("MATRIX_MULTI", "Ma trận (Nhiều lựa chọn / hàng)"),
                    new SeedItem("MATRIX_INPUT", "Ma trận nhập số / chữ"),
                    new SeedItem("DYNAMIC_TABLE", "Bảng động (Thêm dòng tự do)")
            );

            for (SeedItem item : defaultTypes) {
                if (!questionTypeRepository.existsByCode(item.code())) {
                    QuestionType qt = new QuestionType();
                    qt.setCode(item.code());
                    qt.setName(item.name());
                    questionTypeRepository.save(qt);
                    log.info("Auto-seeded QuestionType: {} ({})", item.code(), item.name());
                }
            }
        };
    }
}
