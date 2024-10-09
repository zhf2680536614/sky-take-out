package com.sky.service;

import com.sky.vo.TurnoverReportVO;

import java.time.LocalDate;

public interface ReportService {
    public TurnoverReportVO getTurnoverReport(LocalDate start,LocalDate end);
}
