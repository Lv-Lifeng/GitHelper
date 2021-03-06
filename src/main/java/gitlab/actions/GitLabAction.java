package gitlab.actions;

import cn.hutool.core.collection.CollectionUtil;
import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import gitlab.bean.GitlabServer;
import gitlab.bean.ProjectDto;
import gitlab.settings.GitLabSettingsState;
import gitlab.settings.SettingsView;
import gitlab.ui.GitLabDialog;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/6 20:10
 */
public class GitLabAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        GitLabSettingsState gitLabSettingsState = GitLabSettingsState.getInstance();
        if (gitLabSettingsState.hasSettings()) {
            showGitLabDialog(project, gitLabSettingsState);
        } else {
            boolean b = new SettingsView(null).showAndGet();
            if (gitLabSettingsState.hasSettings()) {
                showGitLabDialog(project, gitLabSettingsState);
            }
        }

    }
    private void showGitLabDialog(Project project, GitLabSettingsState gitLabSettingsState){
        ProgressManager.getInstance().run(new Task.Modal(project, "GitLab", true) {
            List<ProjectDto> projectDtoList = new ArrayList<>();
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Loading projects...");
                List<GitlabServer> gitlabServers = gitLabSettingsState.getGitlabServers();
                AtomicInteger index = new AtomicInteger(1);
                projectDtoList = gitlabServers
                        .stream()
                        .filter(o -> !indicator.isCanceled())
                        .map(o -> {
                            indicator.setText2(o.getRepositoryUrl()+" ("+ index.getAndIncrement() +"/"+ gitlabServers.size()+")");
                            return gitLabSettingsState.loadMapOfServersAndProjects(Lists.newArrayList(o)).values();
                        }).flatMap(Collection::stream)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
                if (CollectionUtil.isEmpty(projectDtoList)) {
                    return;
                }
                indicator.setText("Projects loaded");
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                if (CollectionUtil.isNotEmpty(projectDtoList)) {
                    new GitLabDialog(project, projectDtoList).showAndGet();
                }
            }
        });
    }
}
