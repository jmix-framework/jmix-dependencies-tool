import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Paths;

public class GradleToolingApiTest {

    @Test
    public void test() {
        String gradleUserHomeDir = "/Users/gorbunkov/Work/jmix-sdk/gradle-home-2";
        GradleConnector gradleConnector = GradleConnector.newConnector()
                .forProjectDirectory(new File("/Users/gorbunkov/Work/jmix-sdk/jmix-dependencies-collector"))
                .useGradleUserHomeDir(new File(gradleUserHomeDir));
        try (ProjectConnection connection = gradleConnector.connect()) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            connection.newBuild()
                    .setStandardOutput(os)
//                    .addArguments("resolveDependencies")
                    .addArguments("resolveDependencies") //task name
                    .addArguments("--dependency", "io.jmix.core:jmix-core-starter")
//                    .addArguments("--dependency=io.jmix.core:jmix-core-starter")
                    .addArguments("--gradle-user-home", gradleUserHomeDir)
                    .addArguments("--info")
//                    .forTasks("resolveDependencies")
                    .run();

            String output = new String(os.toByteArray());
            System.out.println(output);
        }
    }
}
