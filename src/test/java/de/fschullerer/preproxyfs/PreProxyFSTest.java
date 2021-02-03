package de.fschullerer.preproxyfs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.fschullerer.preproxyfs.testutil.UtilT;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for main class. */
class PreProxyFSTest {

    @Test
    @Tag("UnitTest")
    @DisplayName(
            "PREFS001: Assert that an specific PreProxyFSException occurs when trying to "
                    + "start PreProxyFS with non existing property file. Starting via main method.")
    void assertPreProxyFS1() {
        String[] args = {"/path/to/non-existing-property-file"};
        Exception exception =
                assertThrows(
                        PreProxyFSException.class,
                        () -> {
                            PreProxyFS.main(args);
                        });
        String expectedMessage = "Failure loading the settings file";
        String actualMessage = exception.getMessage();
        assertThat(actualMessage)
                .as("Not the exact PreProxyFSException message is displayed!")
                .startsWith(expectedMessage);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName(
            "PREFS002: Assert that an specific PreProxyFSException occurs when trying to "
                    + "start PreProxyFS with non existing PAC file. Starting via startPreProxyFS method.")
    void assertPreProxyFS2() {
        String[] args = {"/path/to/non-existing-PAC-file"};
        Exception exception =
                assertThrows(
                        PreProxyFSException.class,
                        () -> {
                            PreProxyFS.startPreProxyFS("/path/to/non-existing-PAC-file", 0, null);
                        });
        String expectedMessage = "A fatal error occurred during startup.";
        String actualMessage = exception.getMessage();
        assertThat(actualMessage)
                .as("Not the exact PreProxyFSException message is displayed!")
                .startsWith(expectedMessage);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName(
            "PREFS003: Assert that an specific PreProxyFSException occurs when trying to "
                    + "start PreProxyFS with a properties file without a PAC URL value.")
    void assertPreProxyFS3() throws IOException {
        String pathToFile = UtilT.createTempPropFile("NO_CONTENT!");
        String[] args = {pathToFile};
        Exception exception =
                assertThrows(
                        PreProxyFSException.class,
                        () -> {
                            PreProxyFS.main(args);
                        });
        String expectedMessage = "No PAC file/URL given in settings.";
        String actualMessage = exception.getMessage();
        assertThat(actualMessage)
                .as("Not the exact PreProxyFSException message is displayed!")
                .startsWith(expectedMessage);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName(
            "PREFS004: Assert that an specific PreProxyFSException occurs when trying to "
                    + "start PreProxyFS with non existing PAC file setting in property file. "
                    + "Starting via main method.")
    void assertPreProxyFS4() throws IOException {
        String pathToFile = UtilT.createTempPropFile("PAC_URL = /path/to/non-existing-PAC-file");
        String[] args = {pathToFile};
        Exception exception =
                assertThrows(
                        PreProxyFSException.class,
                        () -> {
                            PreProxyFS.main(args);
                        });
        String expectedMessage = "A fatal error occurred during startup.";
        String actualMessage = exception.getMessage();
        assertThat(actualMessage)
                .as("Not the exact PreProxyFSException message is displayed!")
                .startsWith(expectedMessage);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName(
            "PREFS005: Assert that an specific PreProxyFSException occurs when trying to "
                    + "start PreProxyFS with wrong configured USER_PASSWORD_MAP value in property file. ")
    void assertPreProxyFS5() throws IOException {
        String pathToPacFile = UtilT.createTempPropFile(UtilT.PAC_SCRIPT_1);
        String pathToPropertyFile =
                UtilT.createTempPropFile(
                        "PAC_URL = " + pathToPacFile + "\nUSER_PASSWORD_MAP = []]]");
        String[] args = {pathToPropertyFile};
        Exception exception =
                assertThrows(
                        PreProxyFSException.class,
                        () -> {
                            PreProxyFS.main(args);
                        });
        String expectedMessage = "The configuration for USER_PASSWORD_MAP is wrong";
        String actualMessage = exception.getMessage();
        assertThat(actualMessage)
                .as("Not the exact PreProxyFSException message is displayed!")
                .startsWith(expectedMessage);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("PREFS006: Assert that setUserPasswordMap is working correctly.")
    void assertPreProxyFS6() {
        String proxy1 = "remote.proxy1.com:8080";
        String user1 = "myUserName";
        String pass1 = "myPassword";
        String proxy2 = "remote.proxy2.com:8080";
        String user2 = "myUserName2";
        String pass2 = "myPassword2";
        String userAuthString =
                "[" + proxy1 + "[[" + user1 + "][" + pass1 + "]]]" + "[" + proxy2 + "[[" + user2
                        + "][" + pass2 + "]]]";
        Map<String, String[]> userAuthActual = PreProxyFS.setUserPasswordMap(userAuthString);
        assertThat(userAuthActual)
                .as("The user authentication map should contain 2 entries!")
                .hasSize(2);
    }
}
