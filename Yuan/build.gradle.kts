import java.util.Base64
plugins {
    alias(libs.plugins.android.library)
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("signing")
}

group = "io.github.bear27570"
version = "1.3.0"

android {
    namespace = "com.bear27570.yuan"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("Yuan Android Library")
        description.set("A Controller which called ‘Yuan’ developed by 27570")
        inceptionYear.set("2025")
        url.set("https://github.com/bear27570/Yuan-main")
        licenses {
            license {
                name.set("GNU General Public License, Version 3.0")
                url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("bear27570")
                name.set("Luca Li")
                email.set("qumingquchangbuhuichong@outlook.com")
                url.set("https://github.com/bear27570")
            }
        }
        scm {
            url.set("https://github.com/bear27570/Yuan-main")
            connection.set("scm:git:git://github.com/bear27570/Yuan-main.git")
            developerConnection.set("scm:git:ssh://git@github.com/bear27570/Yuan-main.git")
        }
    }
}
signing {
    useInMemoryPgpKeys(
        findProperty("signing.secretKey") as String?,
        findProperty("signing.password") as String?
    )
    sign(publishing.publications)
}

val ftcSdkVersion: String by project

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    compileOnly("org.firstinspires.ftc:RobotCore:$ftcSdkVersion")
    compileOnly("org.firstinspires.ftc:FtcCommon:$ftcSdkVersion")
    compileOnly("org.firstinspires.ftc:Hardware:$ftcSdkVersion")
    compileOnly("org.firstinspires.ftc:RobotServer:$ftcSdkVersion")
}