package de.fschullerer.preproxyfs.unitest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.github.markusbernhardt.proxy.selector.pac.JavaxPacScriptParser;
import com.github.markusbernhardt.proxy.selector.pac.ProxyEvaluationException;
import de.fschullerer.preproxyfs.DirectForwardServer;
import de.fschullerer.preproxyfs.DistributeServer;
import de.fschullerer.preproxyfs.PreProxyFS;
import de.fschullerer.preproxyfs.PreProxyFSException;
import de.fschullerer.preproxyfs.testutil.PacScriptSourceString;
import de.fschullerer.preproxyfs.testutil.ServerSocketThread;
import de.fschullerer.preproxyfs.testutil.UtilT;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for main class.
 */
class PreProxyFSTest {
    
    @Test
    @Tag("UnitTest")
    @DisplayName("PREFS001: Assert that an specific PreProxyFSException occurs when trying to "
                         + "start PreProxyFS with non existing property file. Starting via main method.")
    void assertPreProxyFS1() {
        String[] args = {"/path/to/non-existing-property-file"};
        Exception exception = assertThrows(PreProxyFSException.class, () -> {
            PreProxyFS.main(args);
        });
        String expectedMessage = "Failure loading the settings file";
        String actualMessage = exception.getMessage();
        assertThat(actualMessage).as("Not the exact PreProxyFSException message is displayed!")
                .startsWith(expectedMessage);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("PREFS002: Assert that an specific PreProxyFSException occurs when trying to "
                         + "start PreProxyFS with non existing PAC file. Starting via startPreProxyFS method.")
    void assertPreProxyFS2() {
        String[] args = {"/path/to/non-existing-PAC-file"};
        Exception exception = assertThrows(PreProxyFSException.class, () -> {
            PreProxyFS.startPreProxyFS("/path/to/non-existing-PAC-file", 0, null);
        });
        String expectedMessage = "A fatal error occurred during startup.";
        String actualMessage = exception.getMessage();
        assertThat(actualMessage).as("Not the exact PreProxyFSException message is displayed!")
                .startsWith(expectedMessage);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("PREFS003: Assert that an specific PreProxyFSException occurs when trying to "
                         + "start PreProxyFS with a properties file without a PAC URL value.")
    void assertPreProxyFS3() throws IOException {
        String pathToFile = UtilT.createTempPropFile("NO_CONTENT!");
        String[] args = {pathToFile};
        Exception exception = assertThrows(PreProxyFSException.class, () -> {
            PreProxyFS.main(args);
        });
        String expectedMessage = "No PAC file/URL given in settings.";
        String actualMessage = exception.getMessage();
        assertThat(actualMessage).as("Not the exact PreProxyFSException message is displayed!")
                .startsWith(expectedMessage);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("PREFS004: Assert that an specific PreProxyFSException occurs when trying to "
                         + "start PreProxyFS with non existing PAC file setting in property file. " 
                         + "Starting via main method.")
    void assertPreProxyFS4() throws IOException {
        String pathToFile = UtilT.createTempPropFile("PAC_URL = /path/to/non-existing-PAC-file");
        String[] args = {pathToFile};
        Exception exception = assertThrows(PreProxyFSException.class, () -> {
            PreProxyFS.main(args);
        });
        String expectedMessage = "A fatal error occurred during startup.";
        String actualMessage = exception.getMessage();
        assertThat(actualMessage).as("Not the exact PreProxyFSException message is displayed!")
                .startsWith(expectedMessage);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("PREFS005: Assert that an specific PreProxyFSException occurs when trying to "
                         + "start PreProxyFS with wrong configured USER_PASSWORD_MAP value in property file. ")
    void assertPreProxyFS5() throws IOException {
        String pathToPacFile = UtilT.createTempPropFile(UtilT.PAC_SCRIPT_1);
        String pathToPropertyFile = UtilT.createTempPropFile("PAC_URL = " 
                                                                     + pathToPacFile + "\nUSER_PASSWORD_MAP = []]]");
        String[] args = {pathToPropertyFile};
        Exception exception = assertThrows(PreProxyFSException.class, () -> {
            PreProxyFS.main(args);
        });
        String expectedMessage = "The configuration for USER_PASSWORD_MAP is wrong";
        String actualMessage = exception.getMessage();
        assertThat(actualMessage).as("Not the exact PreProxyFSException message is displayed!")
                .startsWith(expectedMessage);
    }

}
