package fr.aym.acslib.api.services;

import fr.aym.acslib.api.ACsService;

/**
 * Required here in DynamX because of the coremod loading system :c
 */
public interface StatsReportingService extends ACsService
{
    /**
     * SHOULD BE CALLED BEFORE ANY REPORT
     * Sets the report url and product name
     */
    void init(ReportLevel level, String url, String productName, String credentials);

    void disable();

    enum ReportLevel
    {
        ALL, ALL_ERRORS, ONLY_CRASHES, NONE
    }
}
