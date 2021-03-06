package org.alfresco.maven.plugin.archiver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.alfresco.repo.module.tool.ModuleManagementTool;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.ConfigurationContainer;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginConfiguration;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.AbstractZipUnArchiver;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
/**
 * This class provides AMP unpacking support for projects depending on AMPs
 * This allows for example AMPs to be specified as <overlay> in the maven-war-plugin
 * @author mindthegab
 *
 */
public class AmpUnArchiver extends AbstractZipUnArchiver {

    public AmpUnArchiver()
    {
    }
    @Requirement
    private LegacySupport legacySupport;
    
    private Map pluginContext = new HashMap();
    
    /**
     * @parameter default-value="${mojoExecution}"
     * @readonly
     */
    private MojoExecution mojoExecution; 

    @Override
    /**
     * By default the AMPs are unpacked in ${project.directory}/${project.build.finalName}
     */
    public File getDestDirectory() {
    	
        MavenSession session = legacySupport.getSession();
        MavenProject project = session.getCurrentProject();
        // By default we use build finalName
        File location = new File(project.getBuild().getDirectory() + File.separator + project.getBuild().getFinalName());
        // If the war plugin configures a custom webappDirectory instead, we pick it up
        Plugin warPlugin = project.getPlugin("org.apache.maven.plugins:maven-war-plugin");
        if(warPlugin != null)
        {
        	Xpp3Dom warPluginConfig = (Xpp3Dom) warPlugin.getConfiguration();
        	if(warPluginConfig != null)
        	{
        		Xpp3Dom warConfigElement = warPluginConfig.getChild("webappDirectory");
        		if(warConfigElement != null)
        		{
        			String webappDir = warConfigElement.getValue();
        			if(webappDir != null && !webappDir.isEmpty())
        			{
        				location = new File(webappDir);
        			}
        		}
        	}
        
        }
        return location;
    }

    @Override
    protected void execute() throws ArchiverException {
        try {
            /**
             * Invoke the ModuleManagementTool to install AMP modules on the WAR file;
             * so far, no backup or force flags are enabled
             */
        	File destDirectory = getDestDirectory();
            ModuleManagementTool mmt = new ModuleManagementTool();
            mmt.setVerbose(false);
            
            if(getDestFile() != null)
            	getLogger().info("Installing into destination file: " + getDestFile());
            
            if(getDestDirectory()!= null)
            	getLogger().info("Installing into destination folder: " + destDirectory);
            
            File destLocation = (getDestFile() == null || !getDestFile().exists() ) ? destDirectory : getDestFile();
            
            getLogger().info("Installing " + getSourceFile() + " into " + destLocation);
            try {
            	mmt.installModule(
                        getSourceFile().getAbsolutePath(),
                        destLocation.getAbsolutePath(),
                        false,  //preview
                        true,   //force install
                        false); //backup
            } catch (Exception e) {
                throw new MojoExecutionException("Problems while installing " + 
            getSourceFile().getAbsolutePath() + " onto " + destLocation.getAbsolutePath(), e);
            }
            getLogger().debug("MMT invocation for " +  getSourceFile().getAbsolutePath() + "complete");
        } catch (Exception e) {
            throw new ArchiverException("Error while expanding "
                    + getSourceFile().getAbsolutePath(), e);
        } finally {
      
        }
    }


}
