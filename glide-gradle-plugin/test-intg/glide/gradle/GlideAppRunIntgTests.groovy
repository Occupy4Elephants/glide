package glide.gradle

import directree.DirTree
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GlideAppRunIntgTests extends Specification {

    public static final File testProjectDir = new File("build", "test-run-project")

    @Shared def runResult
    @Shared List<File> pluginClasspath


    def setup() {

    }

    def cleanup() { // teardown


    }

    def setupSpec() {  // before-class

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")

        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        this.pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }

        DirTree.create(testProjectDir.absolutePath) {
            dir "app", {
                file "index.groovy", "println 'home'"
                file "index.html", "<h1>hello world</h1>"
                file "_routes.groovy", "get '/', forward:'index.groovy'"
            }
            file "build.gradle", """\
                   plugins {
                    id 'com.appspot.glide-gae'
                   }
                   appengine {
                        daemon = true
                   }
                """.stripIndent()
        }

        runResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withTestKitDir(IntgTestHelpers.testKitGradleHome)
                .withPluginClasspath(pluginClasspath)
                .withArguments('appengineRun', '--debug' ,"--stacktrace")
                .build()

        println runResult.output

        def buildDir = new File(testProjectDir, "build")


    }

    def cleanupSpec() {  // after-class
        def stopResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withTestKitDir(IntgTestHelpers.testKitGradleHome)
                .withPluginClasspath(pluginClasspath)
                .withArguments('appengineStop', '--debug' ,"--stacktrace")
                .build()
    }

    def "starts the development server"() {
        expect:
        runResult.task(":glideSync").outcome == SUCCESS
        runResult.task(":appengineRun").outcome == SUCCESS
        runResult.output.contains('Dev App Server is now running')
    }

    @Ignore('cant get the latest server logs')
    def "output contains logs"() {
        new URL("http://localhost:8080/").text

        expect:
        runResult.output.contains "uri=/"
    }


    def "serves groovy scripts"() {
        expect:
        new URL("http://localhost:8080/index.groovy").text.contains 'home'
    }

    def "routes work"() {
        def resp = new URL("http://localhost:8080/").text

        expect:
        resp.contains 'home'
    }




}
