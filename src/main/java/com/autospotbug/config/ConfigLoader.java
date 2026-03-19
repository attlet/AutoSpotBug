package com.autospotbug.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    @SuppressWarnings("unchecked")
    public List<ProjectConfig> load(String configPath) throws IOException {
        log.info("설정 파일 로드: {}", configPath);

        try (InputStream is = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(is);

            if (root == null || !root.containsKey("projects")) {
                log.warn("프로젝트 목록이 비어있습니다: {}", configPath);
                return Collections.emptyList();
            }

            List<Map<String, String>> rawProjects = (List<Map<String, String>>) root.get("projects");

            return rawProjects.stream()
                    .map(m -> new ProjectConfig(
                            m.get("name"),
                            m.get("path"),
                            m.get("branch")
                    ))
                    .toList();
        }
    }
}
