package net.fabricmc.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class DioriteConfig {
	public String endpoint;
	public HashMap<String, String> queryParams;

  private static String getConfigFileLocation() {
		File root = new File(ExampleMod.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		
		return root.getParent() + "/diorite/config.yaml";
	}

	private static void createConfigSkeleton() throws IOException {		
		// Create directory
		File directory = new File(getConfigFileLocation());

		if (!directory.getParentFile().exists()) {
			directory.getParentFile().mkdir();
		}

		File file = new File(getConfigFileLocation());

		// Create file
		if (!file.exists()) {
			Files.copy(ExampleMod.class.getClassLoader().getResourceAsStream("config.sample.yaml"), Paths.get(file.getAbsolutePath()));
		}
	}

	public static DioriteConfig loadConfig() throws Exception {
		DioriteConfig.createConfigSkeleton();
		
		return new ObjectMapper(new YAMLFactory()).readValue(new File(getConfigFileLocation()), DioriteConfig.class);
	}
}
