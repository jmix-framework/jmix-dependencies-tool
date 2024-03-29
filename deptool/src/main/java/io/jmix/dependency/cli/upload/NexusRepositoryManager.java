package io.jmix.dependency.cli.upload;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.jmix.dependency.cli.upload.model.Artifact;
import io.jmix.dependency.cli.upload.model.ArtifactNpm;
import io.jmix.dependency.cli.upload.model.ArtifactsBundle;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexusRepositoryManager {

    private static final Logger logger = LoggerFactory.getLogger(NexusRepositoryManager.class);

    private final String nexusUrl;

    private final String repositoryName;

    private final String username;

    private final String password;

    public NexusRepositoryManager(String nexusUrl, String repositoryName, String username, String password) {
        this.nexusUrl = nexusUrl;
        this.repositoryName = repositoryName;
        this.username = username;
        this.password = password;
    }

    /**
     * Checks whether an artifact is uploaded to Nexus repository
     */
    public boolean isArtifactUploaded(Artifact artifact) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(getArtifactUrl(artifact));
            return httpClient.execute(httpGet, response -> {
                return response.getCode() == 200;
            });
        } catch (Exception e) {
            throw new RuntimeException("Error on checking that artifact is uploaded", e);
        }
    }

    private String getArtifactUrl(Artifact artifact) {
        return nexusUrl + "/repository/" + repositoryName + "/" +
                artifact.getGroupId().replace(".", "/") + "/" +
                artifact.getArtifactId() + "/" +
                artifact.getVersion() + "/" +
                artifact.getArtifactId() + "-" +
                artifact.getVersion() +
                (shouldAddClassifierToFilename(artifact.getClassifier()) ? "-" + artifact.getClassifier() : "") +
                "." + artifact.getExtension();
    }

    private boolean shouldAddClassifierToFilename(String classifier) {
        return "sources".equals(classifier);
    }

    /**
     * Uploads several artifacts of the same dependency (pom, jar, sources) to the Nexus repository.
     * <p>
     * See <a href="https://help.sonatype.com/repomanager3/integrations/rest-and-integration-api/components-api">Nexus
     * Components API</a>
     *
     * @param artifactsBundle
     */
    public void uploadArtifacts(ArtifactsBundle artifactsBundle) {
        //todo shared http client?
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            multipartEntityBuilder.addTextBody("maven2.groupId", artifactsBundle.getGroupId())
                    .addTextBody("maven2.artifactId", artifactsBundle.getArtifactId())
                    .addTextBody("maven2.version", artifactsBundle.getVersion());

            for (int i = 1; i <= artifactsBundle.getArtifacts().size(); i++) {
                Artifact artifact = artifactsBundle.getArtifacts().get(i - 1);
                multipartEntityBuilder.addBinaryBody("maven2.asset" + i, artifact.getFile());
                multipartEntityBuilder.addTextBody("maven2.asset" + i + ".extension", artifact.getExtension());
                if (artifact.getClassifier() != null) {
                    multipartEntityBuilder.addTextBody("maven2.asset" + i + ".classifier", artifact.getClassifier());
                }
            }

            HttpEntity httpEntity = multipartEntityBuilder.build();

            String uploadUrl = nexusUrl + "/service/rest/v1/components?repository=" + repositoryName;
            HttpPost httpPost = new HttpPost(uploadUrl);
            String credentials = username + ":" + password;
            byte[] encodedCredentialsBytes = Base64.encodeBase64(credentials.getBytes());
            httpPost.addHeader("Authorization", "Basic " + new String(encodedCredentialsBytes));
            httpPost.setEntity(httpEntity);

            logger.info("Uploading artifacts: {}", artifactsBundle);

            httpClient.execute(httpPost, response -> {
                if (response.getCode() != 204) {
                    HttpEntity responseEntity = response.getEntity();
                    logger.info("Response status line: {}", new StatusLine(response));
                    if (responseEntity != null) {
                        String responseText = EntityUtils.toString(responseEntity);
                        logger.info("Response body: {}", responseText);
                    }
                    EntityUtils.consume(responseEntity);
                }
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Error on uploading artifact", e);
        }
    }

    /**
     * Checks whether an NPM artifact is uploaded to Nexus repository
     */
    public boolean isNpmArtifactUploaded(ArtifactNpm artifact) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String artifactUrl = this.nexusUrl + "/service/rest/v1/search/assets?"
                    + "repository=" + repositoryName
                    + "&name=" + artifact.getModuleName()
                    + "&version=" + artifact.getVersion();
            logger.info("Search artifact Url={}", artifactUrl);


            HttpGet httpGet = new HttpGet(artifactUrl);
            return httpClient.execute(httpGet, response -> {
                logger.info("Check artifact response: {}", response.getCode());
                HttpEntity responseEntity = response.getEntity();
                try {
                    if (responseEntity != null) {
                        String responseString = EntityUtils.toString(responseEntity);
                        JsonObject jsonResp = new Gson().fromJson(responseString, JsonObject.class);
                        if (jsonResp.has("items") && jsonResp.get("items").isJsonArray()) {
                            JsonArray items = jsonResp.getAsJsonArray("items");
                            return !items.isEmpty();
                        }
                    }
                    return false;
                } catch (Exception e) {
                    throw new RuntimeException("Error on checking that artifact is uploaded", e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error on checking that artifact is uploaded", e);
        }
    }

    /**
     * Uploads NPM artifact to the Nexus repository.
     * <p>
     * See <a href="https://help.sonatype.com/repomanager3/integrations/rest-and-integration-api/components-api">Nexus
     * Components API</a>
     *
     * @param artifact artifact
     */
    public void uploadNpmArtifacts(ArtifactNpm artifact) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            multipartEntityBuilder.addBinaryBody("npm.asset", artifact.getFile());

            HttpEntity httpEntity = multipartEntityBuilder.build();

            String uploadUrl = nexusUrl + "/service/rest/v1/components?repository=" + repositoryName;
            HttpPost httpPost = new HttpPost(uploadUrl);
            String credentials = username + ":" + password;
            byte[] encodedCredentialsBytes = Base64.encodeBase64(credentials.getBytes());
            httpPost.addHeader("Authorization", "Basic " + new String(encodedCredentialsBytes));
            httpPost.setEntity(httpEntity);

            logger.info("Uploading artifact: {}", artifact);

            httpClient.execute(httpPost, response -> {
                if (response.getCode() != 204) {
                    HttpEntity responseEntity = response.getEntity();
                    logger.info("Response status line: {}", new StatusLine(response));
                    if (responseEntity != null) {
                        String responseText = EntityUtils.toString(responseEntity);
                        logger.info("Response body: {}", responseText);
                    }
                    EntityUtils.consume(responseEntity);
                }
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Error on uploading artifact", e);
        }
    }
}
