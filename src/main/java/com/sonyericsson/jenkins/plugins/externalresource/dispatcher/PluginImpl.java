/*
 *  The MIT License
 *
 *  Copyright 2011 Sony Ericsson Mobile Communications. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.sonyericsson.jenkins.plugins.externalresource.dispatcher;

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;
import hudson.Plugin;
import hudson.model.Hudson;
import hudson.model.Items;
import hudson.model.Run;
import hudson.model.Descriptor.FormException;
import java.io.IOException;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Main plugin implementation.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class PluginImpl extends Plugin {

  /**
     * Release Key, used by releaseAll().
     */
    private String releaseKey;

  /**
     * Empty constructor, method getInstance() brings the singleton instance.
     */
    public PluginImpl() {
    }

  /**
     * Initializing configuration.
     *
     * @throws Exception an Exception.
     */
    @Override
    public void start() throws Exception {
        registerXStreamAlias();
        load();
    }

    /**
     * XStream registrations.
     */
    private void registerXStreamAlias() {
        Class[] types = {
                ExternalResource.class, };
        //Register it in all known XStreams just to be sure.
        Hudson.XSTREAM.processAnnotations(types);
        Items.XSTREAM.processAnnotations(types);
        Run.XSTREAM.processAnnotations(types);
    }

  /**
     * This method is executed when the user clicks "Save"
     * on the general configuration page, thus making the values available
     * as global configuration settings from this singleton.
     *
     * @param req the StaplerRequest object
     * @param formData the data sent by the page form as a JSONObject object
     * @throws java.io.IOException an IOException.
     * @throws ServletException a ServletException.
     * @throws hudson.model.Descriptor.FormException a FormException.
     */
    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, FormException {

        // Setting from the fields in the general config page
        releaseKey = formData.getString("releaseKey");

        save();
     }

  /**
     * Get this singleton when the user is going to configure the project.
     * The singleton will first make available some global configuration values
     * for the user, then it will be used to configure variables in the current project
     * and pass values for the builder.
     *
     * @return the instance for this singleton.
     */
    public static PluginImpl getInstance() {
        PluginImpl instance = Hudson.getInstance().getPlugin(PluginImpl.class);
        if (instance == null) {
            throw new IllegalStateException("Plugin is not loaded!");
        }
        return instance;
    }

  /**
     * Retrieves the releaseKey field, containing the value of Release Key.
     *
     * @return the releaseKey.
     */
    public String getReleaseKey() {
        return releaseKey;
    }

}
