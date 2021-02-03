package de.fschullerer.preproxyfs.testutil;

import com.github.markusbernhardt.proxy.selector.pac.PacScriptSource;

/**
 * Only needed for testing. Set PAC as string within a PacScriptSource.
 *
 * @author Frank Schullerer
 */
public class PacScriptSourceString implements PacScriptSource {

    private final String content;

    /**
     * Set PAC script.
     *
     * @param content The PAC script content.
     */
    public PacScriptSourceString(String content) {
        this.content = content;
    }

    @Override
    public String getScriptContent() {
        return content;
    }

    @Override
    public boolean isScriptValid() {
        return true;
    }
}
