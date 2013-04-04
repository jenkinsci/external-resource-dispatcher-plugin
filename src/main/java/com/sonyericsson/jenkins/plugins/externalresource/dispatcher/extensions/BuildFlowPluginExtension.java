package com.sonyericsson.jenkins.plugins.externalresource.dispatcher.extensions;

import hudson.Extension;

import com.cloudbees.plugins.flow.BuildFlowDSLExtension;
import com.cloudbees.plugins.flow.FlowDelegate;
import com.sonyericsson.jenkins.plugins.externalresource.dispatcher.PluginImpl;

/**
 * Exposes the External Resource Manager to the Build Flow plugin
 * See <a href="https://wiki.jenkins-ci.org/display/JENKINS/Build+Flow+Plugin">Build+Flow+Plugin</a>
 *
 * @author Patrik Johansson &lt;patrik.x.johansson@ericsson.com&gt;
 *
 */
@Extension
public class BuildFlowPluginExtension extends BuildFlowDSLExtension {

  public Object createExtension(String extensionName, FlowDelegate dsl){
    if(extensionName.equalsIgnoreCase("externalresource-dispatcher")){
      return PluginImpl.getInstance().getManager();
    }
    else{
      return null;
    }
  }
}
