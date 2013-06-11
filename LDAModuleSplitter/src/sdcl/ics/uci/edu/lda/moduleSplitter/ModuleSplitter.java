package sdcl.ics.uci.edu.lda.moduleSplitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import sdcl.ics.uci.edu.lda.util.ExperimentDataUtil;
import sdcl.ics.uci.edu.lda.util.ModuleData;

public class ModuleSplitter {

	private static String CALICO_ROOT = ExperimentDataUtil.SPLITTER_PROJECT_REPO
			+ ExperimentDataUtil.SEPARATOR + "calicoPckNamsModif";

	public static String CALICO_CLIENT_SRC = CALICO_ROOT
			+ ExperimentDataUtil.SEPARATOR
			+ "CalicoClient/trunk/calico3client-bugfixes/src";

	public static String CALICO_SERVER_SRC = CALICO_ROOT
			+ ExperimentDataUtil.SEPARATOR + "CalicoServer/calico3server/src";

	private static String CALICO_CLIENT_MODULES_FILE = CALICO_ROOT
			+ ExperimentDataUtil.SEPARATOR + "clientmodules.txt";
	private static String CALICO_SERVER_MODULES_FILE = CALICO_ROOT
			+ ExperimentDataUtil.SEPARATOR + "servermodules.txt";

	public static void main(String[] args) throws Exception {
		ModuleSplitter ms = new ModuleSplitter();
		List<ModuleData> modules = ms.getProjectModules(Project.CALICO);
		List<String> fileNames = new ArrayList<String>();
		List<String> texts;
		for (ModuleData oneModule : modules) {
			texts = oneModule.getTexts(fileNames);
			System.out.println("done with module: " + oneModule.moduleName);
		}
		System.out.println("done");
	}

	public ModuleData getCalicoClient() {
		ModuleData ret = new ModuleData(CALICO_CLIENT_SRC);
		ret.moduleName = "calico.client";
		ret.addFolder("", true);
		return ret;
	}

	public ModuleData getCalicoServer() {
		ModuleData ret = new ModuleData(CALICO_SERVER_SRC);
		ret.moduleName = "calico.server";
		ret.addFolder("", true);
		return ret;
	}

	public List<ModuleData> getProjectModules(Project project) {
		List<ModuleData> ret = new ArrayList<ModuleData>();
		switch (project) {
		case CALICO:

			Properties client = readPropertiesFile(new File(
					CALICO_CLIENT_MODULES_FILE));
			ret.addAll(createModuleData(client, CALICO_CLIENT_SRC));

			Properties server = readPropertiesFile(new File(
					CALICO_SERVER_MODULES_FILE));
			ret.addAll(createModuleData(server, CALICO_SERVER_SRC));

			break;

		default:
			break;

		}
		return ret;
	}

	public enum Project {
		CALICO, MYLYN;
	}

	private List<ModuleData> createModuleData(Properties propertyFile,
			String projectRoot) {
		List<ModuleData> ret = new ArrayList<ModuleData>();
		Enumeration em = propertyFile.keys();
		while (em.hasMoreElements()) {
			String str = (String) em.nextElement();
			ModuleData oneModule = new ModuleData(projectRoot);
			String value = propertyFile.getProperty(str);
			oneModule.moduleName = str;
			String[] tokens = value.split(",");
			for (String token : tokens) {
				if (token.trim().endsWith("+")) {
					oneModule.addFolder(token.substring(0, token.indexOf("+")), true);
				} else {
					oneModule.addFolder(token, false);
				}
			}
			ret.add(oneModule);
		}
		return ret;
	}

	private Properties readPropertiesFile(File propertiesFile) {
		Properties prop = new Properties();

		try {
			// load a properties file
			prop.load(new FileInputStream(propertiesFile));

		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return prop;
	}
}
