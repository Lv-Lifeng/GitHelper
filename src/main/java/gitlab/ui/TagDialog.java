package gitlab.ui;

import cn.hutool.core.collection.CollectionUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import gitlab.bean.Result;
import gitlab.bean.SelectedProjectDto;
import gitlab.enums.OperationTypeEnum;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/16 18:12
 */
public class TagDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField tagName;
    private JComboBox createFrom;
    private JTextField message;
    private SelectedProjectDto selectedProjectDto;

    public TagDialog(SelectedProjectDto selectedProjectDto) {
        super(true);
        setTitle("Create Tag");
        this.selectedProjectDto = selectedProjectDto;
        init();

    }

    @Override
    protected void init() {
        super.init();
        ProgressManager.getInstance().run(new Task.Modal(null, "Create Tag", false) {

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Loading common branches and tags...");
                List<String> commonFrom = new ArrayList<>();
                List<String> commonBranch = selectedProjectDto.getSelectedProjectList().stream()
                        .map(o -> selectedProjectDto.getGitLabSettingsState().api(o.getGitlabServer())
                                .getBranchesByProject(o)
                                .stream()
                                .map(GitlabBranch::getName)
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList())
                        .stream()
                        .reduce((a, b) -> CollectionUtil.intersectionDistinct(a, b).stream().collect(Collectors.toList()))
                        .orElse(Lists.newArrayList());
                commonBranch.stream().sorted(String::compareToIgnoreCase);
                List<String> commonTag = selectedProjectDto.getSelectedProjectList().stream()
                        .map(o -> selectedProjectDto.getGitLabSettingsState().api(o.getGitlabServer())
                                .getTagsByProject(o)
                                .stream()
                                .map(GitlabTag::getName)
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList())
                        .stream()
                        .reduce((a, b) -> CollectionUtil.intersectionDistinct(a, b).stream().collect(Collectors.toList()))
                        .orElse(Lists.newArrayList());
                commonTag.stream().sorted(String::compareToIgnoreCase);
                if (CollectionUtil.isNotEmpty(commonBranch)) {
                    commonFrom.addAll(commonBranch);
                }
                if (CollectionUtil.isNotEmpty(commonTag)) {
                    commonFrom.addAll(commonTag);
                }
                createFrom.setModel(new DefaultComboBoxModel(commonFrom.toArray()));
                createFrom.setSelectedIndex(-1);
                createFrom.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        super.keyReleased(e);
                        JTextField textField = (JTextField) e.getSource();
                        String text = textField.getText();
                        createFrom.setModel(new DefaultComboBoxModel(searchBranchOrTag(text, commonFrom).toArray()));
                        textField.setText(text);
                        createFrom.showPopup();
                    }
                });
                indicator.setText("Common branches and tags loaded");
            }
        });

    }

    private List<String> searchBranchOrTag(String text, List<String> commonBranch) {
        return commonBranch.stream().filter(o ->
                (StringUtils.isNotEmpty(text)
                        && o.toLowerCase().contains(text.toLowerCase()))
                        || StringUtils.isEmpty(text))
                .collect(Collectors.toList());
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (createFrom.getSelectedItem() == null || StringUtils.isBlank(createFrom.getSelectedItem().toString())) {
            return new ValidationInfo("Create From cannot be empty.", createFrom);
        }

        if (StringUtils.isBlank(tagName.getText())) {
            return new ValidationInfo("Tag Name cannot be empty.", tagName);
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        List<Result> resultList = (List<Result>) ProgressManager.getInstance().run(new Task.WithResult(null, "Create Tag", false) {
            @Override
            protected Object compute(@NotNull ProgressIndicator indicator) {
                indicator.setText("New tag is creating...");
                String source = (String) createFrom.getSelectedItem();
                if (StringUtils.isEmpty(source) || StringUtils.isEmpty(tagName.getText())) {
                    return Lists.newArrayList();
                }
                List<Result> results = selectedProjectDto.getSelectedProjectList().stream().map(s -> {
                    try {
                        GitlabTag tagResult = selectedProjectDto.getGitLabSettingsState().api(s.getGitlabServer())
                                .addTag(s.getId(), tagName.getText(), source, message.getText(), null);
                        /*Result re = new Result();
                        re.setType(OperationTypeEnum.CREATE_TAG)
                                .setProjectName(s.getName())
                                .setDesc(tagResult.getMessage());
                        return re;*/
                        return null;
                    } catch (IOException ioException) {
                        Result re = new Result(new GitlabMergeRequest());
                        re.setType(OperationTypeEnum.CREATE_TAG)
                                .setProjectName(s.getName())
                                .setErrorMsg(ioException.getMessage());
                        return re;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
                indicator.setText("New tag created");
                return results;
            }
        });
        if (CollectionUtil.isNotEmpty(resultList)) {
            new ResultDialog(resultList, OperationTypeEnum.CREATE_TAG.getDialogTitle()).showAndGet();
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
