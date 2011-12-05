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
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.ExternalResourceManager;
import hudson.ExtensionList;
import hudson.Plugin;
import hudson.model.Computer;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Items;
import hudson.model.Run;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Main plugin implementation.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class PluginImpl extends Plugin {

    /**
     * The logger.
     */
    public static final Logger logger = Logger.getLogger(PluginImpl.class.getName());

    /**
     * Permission group for  {@link ExternalResource} related operations.
     */
    public static final PermissionGroup GROUP = new PermissionGroup(
            PluginImpl.class, Messages._ExternalResource_DisplayName());

    /**
     * Permission to enable or Disable an {@link ExternalResource}.
     */
    public static final Permission ENABLE_DISABLE_EXTERNAL_RESOURCE = new Permission(GROUP, "EnableDisable",
            Messages._ExternalResource_EnableDisable(), Computer.CONFIGURE);

    /**
     * Form field name for releaseKey on the config page.
     */
    protected static final String FORM_NAME_RELEASE_KEY = "releaseKey";

    /**
     * Form field name for the external resource manager on the config page.
     */
    protected static final String FORM_NAME_MANAGER = "manager";

    /**
     * Form field name for the external resource manager on the config page.
     */
    protected static final String FORM_NAME_RESERVE_TIME = "reserveTime";

    /**
     * Form field name for admin notifier file on the config page.
     */
    protected static final String FORM_NAME_ADMIN_FILE = "adminNotifierFile";

    /**
     * Release Key, used by releaseAll().
     */
    private String releaseKey;

    /**
     * The selected external resource manager.
     */
    private transient ExternalResourceManager manager;
    /**
     * The class name of the manager to store into config.
     */
    private String managerClass;

    private int reserveTime = Constants.DEFAULT_RESERVE_TIME;

    /**
     * admin notifier file.  {@link AdminNotifier}
     */
    private String adminNotifierFile;

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
     * This method is executed when the user clicks "Save" on the general configuration page, thus making the values
     * available as global configuration settings from this singleton.
     *
     * @param req      the StaplerRequest object
     * @param formData the data sent by the page form as a JSONObject object
     * @throws java.io.IOException an IOException.
     * @throws ServletException    a ServletException.
     * @throws hudson.model.Descriptor.FormException
     *                             a FormException.
     */
    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, FormException {

        // Setting from the fields in the general config page
        releaseKey = formData.getString(FORM_NAME_RELEASE_KEY);
        String managerName = formData.getString(FORM_NAME_MANAGER);

        //Find the name of the configured ExternalResourceManager
        ExternalResourceManager dynamic =
                getAvailableExternalResourceManagers().getDynamic(managerName);
        if (dynamic == null) {
            throw new FormException("Unknown manager: " + managerName, FORM_NAME_MANAGER);
        }
        this.manager = dynamic;
        this.managerClass = dynamic.getClass().getName();

        this.reserveTime = formData.getInt(FORM_NAME_RESERVE_TIME);
        this.adminNotifierFile = formData.getString(FORM_NAME_ADMIN_FILE);

        logger.fine("Saving config.");
        save();
    }

    /**
     * The list of {@link ExternalResourceManager} Extensions.
     *
     * @return a list of available managers.
     */
    public ExtensionList<ExternalResourceManager> getAvailableExternalResourceManagers() {
        return Hudson.getInstance().getExtensionList(ExternalResourceManager.class);
    }

    /**
     * Get this singleton when the user is going to configure the project. The singleton will first make available some
     * global configuration values for the user, then it will be used to configure variables in the current project and
     * pass values for the builder.
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

    //CS IGNORE LineLength FOR NEXT 6 LINES. REASON: JavaDoc

    /**
     * Gives the default
     * {@link com.sonyericsson.jenkins.plugins.externalresource.dispatcher.utils.ExternalResourceManager.NoopExternalResourceManager}.
     * @return the default manager.
     */
    public static ExternalResourceManager getNoopResourceManager() {
        return Hudson.getInstance().getExtensionList(ExternalResourceManager.class)
                .get(ExternalResourceManager.NoopExternalResourceManager.class);
    }

    /**
     * The selected manager.
     *
     * @return the manager.
     */
    public synchronized ExternalResourceManager getManager() {
        if (manager == null) {
            if (managerClass != null && !managerClass.isEmpty()) {
                ExternalResourceManager dynamic =
                        getAvailableExternalResourceManagers().getDynamic(managerClass);
                if (dynamic != null) {
                    manager = dynamic;
                } else {
                    logger.severe("The configured external resource manager could not be found! "
                            + managerClass + " Using the default No-Op-manager.");
                    manager = getNoopResourceManager();
                }
            } else {
                logger.severe("No configured external resource manager could be found! "
                        + "Using the default No-Op-manager.");
                manager = getNoopResourceManager();
            }
        }
        return manager;
    }

    /**
     * The configured number of seconds to reserve a resource during scheduling in
     * {@link ExternalResourceQueueTaskDispatcher}.
     *
     * @return the seconds.
     */
    public int getReserveTime() {
        return reserveTime;
    }

    /**
     * The default number of seconds to reserve a resource.
     * Used for simplified jelly usage.
     *
     * @return {@link Constants#DEFAULT_RESERVE_TIME}.
     */
    @SuppressWarnings("unused")
    public int getDefaultReserveTime() {
        return Constants.DEFAULT_RESERVE_TIME;
    }

    /**
     * Retrieves the file name for statistics log used by {@link AdminNotifier}.
     *
     * @return the adminNotifierFile.
     */
    public String getAdminNotifierFile() {
        return adminNotifierFile;
    }
}
