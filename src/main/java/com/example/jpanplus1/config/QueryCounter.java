package com.example.jpanplus1.config;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

/**
 * Helper component to track Hibernate query statistics.
 * Used to demonstrate the exact number of queries executed in each scenario.
 */
@Component
public class QueryCounter {

    private final Statistics statistics;

    public QueryCounter(EntityManagerFactory emf) {
        this.statistics = emf.unwrap(SessionFactory.class).getStatistics();
        this.statistics.setStatisticsEnabled(true);
    }

    /**
     * Reset counters before a test scenario.
     */
    public void clear() {
        statistics.clear();
    }

    /**
     * Get the total number of SQL statements executed since last clear().
     */
    public long getQueryCount() {
        return statistics.getQueryExecutionCount()
                + statistics.getEntityLoadCount();
    }

    /**
     * Get the number of SQL queries prepared and executed.
     */
    public long getPrepareStatementCount() {
        return statistics.getPrepareStatementCount();
    }

    public Statistics getStatistics() {
        return statistics;
    }
}
