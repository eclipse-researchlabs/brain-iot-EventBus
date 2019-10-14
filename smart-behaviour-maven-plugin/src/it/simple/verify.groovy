import java.util.*;
import java.util.jar.*;

println 'Tests for smart-behaviour-maven-plugin \'simple\''
println " basedir: ${basedir}"

// Check the bundle exists!
File behaviour = new File(basedir, 'target/simple-test-0.0.1-SNAPSHOT-brain-iot-smart-behaviour.jar')
assert behaviour.isFile()

// Load JAR and Manifest
JarFile behaviour_jar = new JarFile(behaviour)
Attributes behaviour_manifest = behaviour_jar.getManifest().getMainAttributes()

// Check Manifest Entry
assert behaviour_manifest.getValue("BRAIN-IoT-Smart-Behaviour-SymbolicName") == "com.paremus.brain.iot.maven.test.simple-test"
assert behaviour_manifest.getValue("BRAIN-IoT-Smart-Behaviour-Version") == "0.0.1.SNAPSHOT"
requirement = behaviour_manifest.getValue("BRAIN-IoT-Deploy-Requirement")
assert requirement.startsWith("osgi.identity;filter:=")
assert requirement.contains("(osgi.identity=simple-test)")
assert requirement.contains("(version>=0.0.1")
assert requirement.contains("(version<=0.0.1")


// Check contents
assert behaviour_jar.getEntry('simple-test-0.0.1-SNAPSHOT.jar') != null
assert behaviour_jar.getEntry('org.apache.felix.scr-2.1.10.jar') != null
