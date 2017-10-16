package com.microsoft.azuretools.container.handlers;

import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.container.ui.PushImageDialog;
import com.microsoft.azuretools.container.utils.ConfigFileUtil;
import com.microsoft.azuretools.container.utils.WarUtil;
import com.microsoft.azuretools.core.actions.MavenExecuteAction;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;

import java.nio.file.Paths;
import java.util.Properties;

public class PushImageHandler extends AzureAbstractHandler {

    private static final String MAVEN_GOALS = "package";
    private IProject project;
    private String destinationPath;
    private String basePath;
    private Properties props;

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        project = PluginUtil.getSelectedProject();
        if (project == null) {
            return null;
        }
        basePath = project.getLocation().toString();
        props = ConfigFileUtil.loadConfig(project);
        DockerRuntime.getInstance().loadFromProps(props);

        try {
            if (MavenUtils.isMavenProject(project)) {
                destinationPath = MavenUtils.getTargetPath(project);
                MavenExecuteAction action = new MavenExecuteAction(MAVEN_GOALS);
                IContainer container = MavenUtils.getPomFile(project).getParent();
                action.launch(container, () -> {
                    buildAndRun();
                    return null;
                });
            } else {
                destinationPath = Paths.get(basePath, Constant.DOCKERFILE_FOLDER, project.getName() + ".war")
                        .normalize().toString();
                WarUtil.export(project, destinationPath);
                buildAndRun();
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendTelemetryOnException(event, e);
        }
        return null;
    }

    private void buildAndRun() {
        DefaultLoader.getIdeHelper().invokeAndWait(() -> {
            PushImageDialog pushImageDialog = new PushImageDialog(PluginUtil.getParentShell(), basePath, destinationPath);
            pushImageDialog.open();
        });
    }
}
