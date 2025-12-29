plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

}

tasks.test {
    useJUnitPlatform()
}

// JavaFX ayarları
javafx {
    version = "21"
    modules = listOf("javafx.controls")
}

//uygulamanın ana sınıfı(mainapp.java)
application {
    mainClass.set("com.hopelab.graph.MainApp")
}
