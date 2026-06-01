package org.telemetry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telemetry.model.TestReport;

@Repository
public interface TestReportRepository extends JpaRepository<TestReport, Long> {
    // Spring magically writes the SQL for this just based on the method name
    java.util.List<TestReport> findByVinOrderByTestDateDesc(String vin);
}