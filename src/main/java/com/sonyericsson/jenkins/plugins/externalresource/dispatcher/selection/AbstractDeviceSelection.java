package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.selection;

import hudson.model.Describable;
import hudson.model.Descriptor;

import java.io.Serializable;

import org.kohsuke.stapler.export.ExportedBean;

import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.data.ExternalResource;



/**
 * Abstract DeviceSelection.
 *
 * @author Ren Wei &lt;wei2.ren@sonyericsson.com&gt;
 *
 */
@ExportedBean
public abstract class AbstractDeviceSelection implements Serializable, Describable<AbstractDeviceSelection> {
    /**
     *Abstract DeviceSelectionDescriptor {@link AbstractDeviceSelection}.
     */
    public abstract static class AbstractDeviceSelectionDescriptor extends Descriptor<AbstractDeviceSelection> {
    }
    /**
     * Device selection input compare With ExternalResource leaf value.
     *
     * @param externalResource External Resource
     * @return true if device selection equals to ExternalResource leaf value
     */
    public abstract boolean equalToExternalResourceValue(ExternalResource externalResource);
}
