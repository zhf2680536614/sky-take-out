package com.sky.service;

import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {
    public TurnoverReportVO getTurnoverReport(LocalDate start,LocalDate end);

    UserReportVO getUserReport(LocalDate begin, LocalDate end);
}
