package com.trinhcong1120.survey_service.dto.statistics;

import java.util.List;

public class MatrixStatisticsResponse {

    private Long totalResponses;
    private List<RowStatistic> rows;
    private List<ColumnStatistic> columns;

    public MatrixStatisticsResponse() {
    }

    public MatrixStatisticsResponse(Long totalResponses, List<RowStatistic> rows, List<ColumnStatistic> columns) {
        this.totalResponses = totalResponses;
        this.rows = rows;
        this.columns = columns;
    }

    public Long getTotalResponses() {
        return totalResponses;
    }

    public void setTotalResponses(Long totalResponses) {
        this.totalResponses = totalResponses;
    }

    public List<RowStatistic> getRows() {
        return rows;
    }

    public void setRows(List<RowStatistic> rows) {
        this.rows = rows;
    }

    public List<ColumnStatistic> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnStatistic> columns) {
        this.columns = columns;
    }

    public static class RowStatistic {
        private String rowId;
        private String rowLabel;
        private Long totalCount;
        private Double averageScore;
        private List<CellStatistic> cells;

        public RowStatistic() {}

        public RowStatistic(String rowId, String rowLabel, Long totalCount, Double averageScore, List<CellStatistic> cells) {
            this.rowId = rowId;
            this.rowLabel = rowLabel;
            this.totalCount = totalCount;
            this.averageScore = averageScore;
            this.cells = cells;
        }

        public String getRowId() { return rowId; }
        public void setRowId(String rowId) { this.rowId = rowId; }

        public String getRowLabel() { return rowLabel; }
        public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }

        public Double getAverageScore() { return averageScore; }
        public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }

        public List<CellStatistic> getCells() { return cells; }
        public void setCells(List<CellStatistic> cells) { this.cells = cells; }
    }

    public static class CellStatistic {
        private String columnId;
        private String columnLabel;
        private Long count;
        private Double percentage;

        public CellStatistic() {}

        public CellStatistic(String columnId, String columnLabel, Long count, Double percentage) {
            this.columnId = columnId;
            this.columnLabel = columnLabel;
            this.count = count;
            this.percentage = percentage;
        }

        public String getColumnId() { return columnId; }
        public void setColumnId(String columnId) { this.columnId = columnId; }

        public String getColumnLabel() { return columnLabel; }
        public void setColumnLabel(String columnLabel) { this.columnLabel = columnLabel; }

        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }

        public Double getPercentage() { return percentage; }
        public void setPercentage(Double percentage) { this.percentage = percentage; }
    }

    public static class ColumnStatistic {
        private String columnId;
        private String columnLabel;
        private String type;
        private Long filledCount;
        private Double averageValue;
        private Double minValue;
        private Double maxValue;

        public ColumnStatistic() {}

        public ColumnStatistic(String columnId, String columnLabel, String type, Long filledCount, Double averageValue, Double minValue, Double maxValue) {
            this.columnId = columnId;
            this.columnLabel = columnLabel;
            this.type = type;
            this.filledCount = filledCount;
            this.averageValue = averageValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        public String getColumnId() { return columnId; }
        public void setColumnId(String columnId) { this.columnId = columnId; }

        public String getColumnLabel() { return columnLabel; }
        public void setColumnLabel(String columnLabel) { this.columnLabel = columnLabel; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Long getFilledCount() { return filledCount; }
        public void setFilledCount(Long filledCount) { this.filledCount = filledCount; }

        public Double getAverageValue() { return averageValue; }
        public void setAverageValue(Double averageValue) { this.averageValue = averageValue; }

        public Double getMinValue() { return minValue; }
        public void setMinValue(Double minValue) { this.minValue = minValue; }

        public Double getMaxValue() { return maxValue; }
        public void setMaxValue(Double maxValue) { this.maxValue = maxValue; }
    }
}

