package org.springframework.cloud.cnb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import org.springframework.cloud.AbstractCloudConnector;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.FallbackServiceInfoCreator;
import org.springframework.cloud.app.ApplicationInstanceInfo;
import org.springframework.cloud.service.BaseServiceInfo;
import org.springframework.cloud.util.EnvironmentAccessor;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Ramnivas Laddad
 * @author Scott Frederick
 */
public class CNBConnector extends AbstractCloudConnector<Map<String, Object>> {

    private EnvironmentAccessor environment = new EnvironmentAccessor();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public CNBConnector() {
        super((Class) CNBServiceInfoCreator.class);
    }

    /* package for testing purpose */
    void setCloudEnvironment(EnvironmentAccessor environment) {
        this.environment = environment;
    }

    @Override
    protected List<Map<String, Object>> getServicesData() {
        List<Map<String, Object>> servicesData = new ArrayList<Map<String, Object>>();
        String bindingsDirPath = environment.getEnvValue("CNB_BINDINGS");
        if (bindingsDirPath == null) {
            return servicesData;
        }
        File bindingsDir = new File(bindingsDirPath);
        if (!bindingsDir.isDirectory()) {
            throw new CloudException(String.format("CNB_BINDINGS '%s' is not a directory", bindingsDir.toString()));
        }
        for (File file : bindingsDir.listFiles()) {
            if (!file.isDirectory()) {
                continue;
            }
            Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("name", file.getName());
            try {
                servicesData.add(readBindingDir(file));
            } catch (Exception e) {
                throw new CloudException(e);
            }
        }

        //TODO: postProcessors

        return servicesData;
    }

    protected Map<String, Object> readBindingDir(File bindingDir) throws IOException {
        Map<String, Object> serviceData = new HashMap<>();
        serviceData.put("name", bindingDir.getName());
        for (File file : bindingDir.listFiles()) {
            if (file.getName().equals("secret")) {
                serviceData.put("credentials", readCredentials(file));
            }
            if (file.getName().equals("metadata")) {
                for (File metadataFile : file.listFiles()) {
                    switch (metadataFile.getName()) {
                        case "tags":
                            String tagsList;
                            tagsList = new String(Files.readAllBytes(metadataFile.toPath()));
                            String[] tags = tagsList.split(",");
                            serviceData.put("tags", Arrays.asList(tags));
                            break;
                        case "kind":
                        case "provider":
                            serviceData.put(metadataFile.getName(), new String(Files.readAllBytes(metadataFile.toPath())));
                    }
                }
            }
        }
        return serviceData;
    }

    private Map<String, String> readCredentials(File file) throws IOException {
        Map<String, String> credentials = new HashMap<String, String>();
        for (File secretFile : file.listFiles()) {
            credentials.put(secretFile.getName(), new String(Files.readAllBytes(secretFile.toPath())));
        }
        return credentials;
    }

    @Override
    protected FallbackServiceInfoCreator<?, Map<String, Object>> getFallbackServiceInfoCreator() {
        return new CNBFallbackServiceInfoCreator();
    }

    @Override
    public boolean isInMatchingCloud() {
        return environment.getEnvValue("CNB_BINDINGS") != null;
    }

    @Override
    public ApplicationInstanceInfo getApplicationInstanceInfo() {
        return null;
    }
}

class CNBFallbackServiceInfoCreator extends FallbackServiceInfoCreator<BaseServiceInfo, Map<String, Object>> {
    @Override
    public BaseServiceInfo createServiceInfo(Map<String, Object> serviceData) {
        String id = (String) serviceData.get("name");
        return new BaseServiceInfo(id);
    }
}