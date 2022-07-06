package fr.aym.acslib.services.impl.stats;

import com.google.gson.JsonObject;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.Scanner;

public class StatsSheet
{
	private final String identifier, product;
	private final String system_details, fileName, fileContent;
	private String shutdown_state = "";
	
	protected StatsSheet(String userName, long timestampSecs, SystemInfoProvider gpu)
	{
		identifier = userName+"-"+timestampSecs;
		product = gpu.getProductName();
		String data = "n/a";
		String fileContent = null, fileName = null;
		try
	    {
	    	String javaVm = System.getProperty("java.vm.name");
	        OperatingSystemMXBean sys = ManagementFactory.getOperatingSystemMXBean();
	        Method md = sys.getClass().getDeclaredMethod("getTotalPhysicalMemorySize");
	        md.setAccessible(true);
	        Long memory = (Long) md.invoke(sys);
	        long systemPhysicMemory = memory / (1000*1000*1000);
	        long allocated = Runtime.getRuntime().totalMemory()/1024/1024;
	        long max = Runtime.getRuntime().maxMemory()/1024/1024;
	        
	        data = "JavaVM:"+javaVm+";SystemMemory(Go):"+systemPhysicMemory+";Allocated(Mo):"+allocated+";Maximum(Mo):"+max;
	        data += ";SystemInfo:"+System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") version " + System.getProperty("os.version");
	        data += ";CpuCores:"+Runtime.getRuntime().availableProcessors();
	        
	        data += ";CpuId:" + System.getenv("PROCESSOR_IDENTIFIER");
	        //data += ";CpuArchi:" + System.getenv("PROCESSOR_ARCHITECTURE");
	        //data += ";CpuW6432:" + System.getenv("PROCESSOR_ARCHITEW6432");

			if(FMLCommonHandler.instance().getSide().isClient())
	        	data += ";GpuInfo:"+gpu.getGPUInfo();
			else
				data += ";GpuInfo:DedicatedServer";
	        
	        File report = gpu.getFileToSend();
	        if(report != null && report.exists())
	        {
	        	addShutdown_state("CrashReport:"+report.getName());
	        	fileName = report.getName();
	        	Scanner sc = new Scanner(report);
	        	fileContent = "";
	        	while(sc.hasNextLine())
	        	{
	        		fileContent+=sc.nextLine()+"\r\n";
	        	}
	        	sc.close();
	        	if(!gpu.isInterestingReport(fileContent)) {
					System.out.println("[StatsBot] Completely ignoring the crash report : not interesting for the SystemInfoProvider !");
	        		fileName = null;
	        		fileContent = null;
				}
	        }
	    }
	    catch(Exception e)
	    {
	    	System.out.println("[StatsBot] [ERROR] Cannot get system information !");
	    	e.printStackTrace();
	    	data = "Error: " + e.toString();
	    }
		finally
		{
			system_details = data;
			this.fileContent = fileContent;
			this.fileName = fileName;
		}
	}

	public StatsSheet addShutdown_state(String shutdown_state) {
		this.shutdown_state = this.shutdown_state+";"+shutdown_state;
		return this;
	}

	public StatsSheet addExceptionData(String title, Throwable e) {
		String es = "<b>"+e.toString();
		es += "</b><br>";
		for(int i=0;i<Math.min(3, e.getStackTrace().length);i++)
			es += e.getStackTrace()[i].toString() + "<br>";
		if(e.getCause() != null) {
			e = e.getCause();
			es += "<b>Caused by " + e.toString();
			es += "</b><br>";
			for(int i=0;i<Math.min(3, e.getStackTrace().length);i++)
				es += e.getStackTrace()[i].toString() + "<br>";
		}
		this.shutdown_state += ";" + title + ":" + es;
		return this;
	}

	public String getShutdown_state() {
		return shutdown_state;
	}

	@Nullable
	public String getFileContent() {
		return fileContent;
	}

	public JsonObject toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("Identitifer", identifier);
		o.addProperty("Product", product);
		o.addProperty("System", system_details);
		o.addProperty("Shutdowns", shutdown_state);
		if(fileContent != null && fileName != null)
		{
			o.addProperty("FileName", fileName);
			o.addProperty("FileContent", fileContent);
		}
		return o;
	}
}
