package fr.aym.acslib.services.impl.stats;

import javax.annotation.Nullable;
import java.io.File;

public interface SystemInfoProvider {
    String getUserId();

    String getProductName();

    String getGPUInfo();

    @Nullable
    File getFileToSend();

    boolean isInterestingReport(String crash);
}
