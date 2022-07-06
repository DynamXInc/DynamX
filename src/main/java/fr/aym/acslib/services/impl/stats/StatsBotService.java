package fr.aym.acslib.services.impl.stats;

import fr.aym.acslib.api.ACsRegisteredService;
import fr.aym.acslib.api.services.StatsReportingService;
import fr.dynamx.common.DynamXMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.Display;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@ACsRegisteredService(name = "dnx_stats", version = "1.0.0", sides = Side.CLIENT, interfaceClass = StatsReportingService.class)
public class StatsBotService implements SystemInfoProvider, StatsReportingService {
    public static StatsBotService INSTANCE = new StatsBotService(); //Fake first instance, before init
    private boolean alreadyReported, crashed, notRealCrashPassed;
    private String URL = "not set";
    private String PRODUCT_NAME = "not set";
    private String USER_NAME = "not set";
    private String CREDENTIALS = "";
    private ReportLevel LEVEL = ReportLevel.ALL;

    public StatsBotService() {
        INSTANCE = this;
        notRealCrashPassed = true;
    }

    @Override
    public String getName() {
        return "dnx_stats";
    }

    @Override
    public String getProductName() {
        return PRODUCT_NAME;
    }

    @Override
    public void init(ReportLevel level, String url, String productName, String credentials) {
        LEVEL = level;
        URL = url;
        PRODUCT_NAME = productName;
        USER_NAME = FMLClientHandler.instance().getClient().getSession().getUsername();
        CREDENTIALS = credentials;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void disable() {
        LEVEL = ReportLevel.NONE;
        DynamXMain.log.info("[STATS_SERVICE] User has disabled automatic reporting.");
    }

    @Override
    public String getGPUInfo() {
        return String.format("Display: %dx%d (%s)", Display.getWidth(), Display.getHeight(), GlStateManager.glGetString(7936)) + "_" + GlStateManager.glGetString(7937) + "_" + GlStateManager.glGetString(7938);
    }

    @Override
    public String getUserId() {
        return USER_NAME;
    }

    @Override //Detected in external program, because not yet created here
    public @Nullable
    File getFileToSend() {
        File file1 = new File(Minecraft.getMinecraft().gameDir, "crash-reports");
        File file2 = new File(file1, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-client.txt");
        if (!file2.exists()) {
            Calendar tg = Calendar.getInstance();
            Date java = new Date();
            tg.set(Calendar.SECOND, tg.get(Calendar.SECOND) - 1);
            java.setTime(tg.getTimeInMillis());
            file2 = new File(file1, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(java) + "-client.txt");
            int i = 1;
            while (!file2.exists() && i < 5) {
                tg.set(Calendar.SECOND, tg.get(Calendar.SECOND) - 1);
                java.setTime(tg.getTimeInMillis());
                file2 = new File(file1, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(java) + "-client.txt");
                i++;
            }
        }
        if (file2.exists())
            System.out.println("Found crash report file " + file2);
        return file2.exists() ? file2 : null;
    }

    @Override
    public boolean isInterestingReport(String crash) {
        String interestingPart = crash.substring(0, crash.indexOf("-- System Details --"));
        return interestingPart.contains("fr.dynamx") || interestingPart.contains("fr.aym");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @SubscribeEvent
    public void onWorldUnloaded(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            Minecraft MC = Minecraft.getMinecraft();
            if (MC.getConnection() != null && MC.getConnection().getNetworkManager().getExitMessage() != null) {
                StatsBotService.INSTANCE.reportDisconnection(MC.getConnection().getNetworkManager().getExitMessage().toString());
            } else
                DynamXMain.log.info("[STATS_SERVICE] Connexion already null");
        }
    }

    public void reportDisconnection(String msg) {
        DynamXMain.log.info("[STATS_SERVICE] Report " + msg + " " + URL);
        if (LEVEL.ordinal() >= ReportLevel.ONLY_CRASHES.ordinal() || crashed || !URL.startsWith("http"))
            return;
        if ((!alreadyReported && LEVEL == ReportLevel.ALL) || msg.isEmpty() || msg.contains("error") || msg.contains("exception")) {
            alreadyReported = true;
            StatsSheet sh = StatsSender.provideStats(this, false).addShutdown_state("Deco-" + (msg.isEmpty() ? "n/a" : msg));
            try {
                StatsSender.reportStats(new URL(URL), CREDENTIALS, sh);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Throwable generatedError;

    //Fired by ASM code

    public void reportCrash(Throwable error) {
        DynamXMain.log.info("[STATS_SERVICE] Report crash " + error + " " + URL);
        if (LEVEL != ReportLevel.NONE && error != null && URL.startsWith("http") && generatedError == null) {
            if (notRealCrashPassed) {
                crashed = true;
                generatedError = error;
                //launchReportProgram(sh);
            } else if (error.toString().contains("Not real")) //FORGE FAKE ERROR TO LOAD CRASH CLASSES
                notRealCrashPassed = true;
        }
    }

    public void onExit() {
        if (generatedError != null) {
            DynamXMain.log.info("[STATS_SERVICE] Send crash " + generatedError + " " + URL);
            StatsSheet sh = null;
            try {
                System.out.println("[StatsBot] Calculus starting");
                sh = StatsSender.provideStats(this, true).addExceptionData("<b><i>Crash</i></b>", generatedError);
            } catch (Exception e) {
                System.out.println("[StatsBot] Calculus failed");
                e.printStackTrace();
            }
            try {
                if (sh == null) {
                    System.out.println("[StatsBot] Calculus restarting with no gui");
                    sh = StatsSender.provideStats(this, false).addExceptionData("<b><i>Crash</i></b>", generatedError);
                }
                System.out.println("[StatsBot] Report starting");
                StatsSender.reportStats(new URL(URL), CREDENTIALS, sh);
                System.out.println("[StatsBot] Report went well");
            } catch (Exception e) {
                System.out.println("[StatsBot] Report failed");
                e.printStackTrace();
            }
        } else {
            DynamXMain.log.warn("[STATS_SERVICE] FML exit function was called without any detected crash !");
        }
    }
}