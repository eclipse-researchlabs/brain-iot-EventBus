import java.util.*;
import java.util.jar.*;

println 'Tests for smart-behaviour-maven-plugin \'simple\''
println " basedir: ${basedir}"

// Check the bundles exist!
File behaviour = new File(basedir, 'target/custom-bndrun-test-0.0.1-SNAPSHOT-brain-iot-smart-behaviour.jar')
assert behaviour.isFile()

// Load JAR and Manifest
JarFile behaviour_jar = new JarFile(behaviour)
Attributes behaviour_manifest = behaviour_jar.getManifest().getMainAttributes()

// Check Manifest Entry
assert behaviour_manifest.getValue("BRAIN-IoT-Smart-Behaviour-SymbolicName") == "com.paremus.brain.iot.maven.test.custom-bndrun-test"
assert behaviour_manifest.getValue("BRAIN-IoT-Smart-Behaviour-Version") == "0.0.1.SNAPSHOT"
requirement = behaviour_manifest.getValue("BRAIN-IoT-Deploy-Requirement")
assert requirement == "osgi.identity;filter:=\"(osgi.identity=custom-bndrun-test)\""

// Check contents
assert behaviour_jar.getEntry('custom-bndrun-test-0.0.1-SNAPSHOT.jar') != null
assert behaviour_jar.getEntry('org.apache.felix.scr-2.1.10.jar') != null
