package gitlab.bean;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import gitlab.enums.OperationTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabMergeRequest;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/11 23:49
 */
@Getter
@Setter
@Accessors(chain = true)
public class Result extends GitlabMergeRequest {
    private String projectName;
    private String errorMsg;
    private String changeFilesCount;
    private OperationTypeEnum type;
    private String desc;

    public Result() {
    }

    public Result(GitlabMergeRequest gitlabMergeRequest) {
        BeanUtil.copyProperties(gitlabMergeRequest, this);
    }

    public void setErrorMsg(String errorMsg) {
        if (JSONUtil.isJson(errorMsg)) {
            JSONObject jsonObject = JSONUtil.parseObj(errorMsg);
            this.errorMsg = jsonObject.getStr("message");
        }
    }

    @Override
    public String toString() {
        if (StringUtils.isNotEmpty(errorMsg)) {
            return projectName + " (" + errorMsg + ")";
        }
        switch (type) {
            case CREATE_MERGE_REQUEST:
                return getWebUrl() + " [ChangeFiles:" + getChangeFilesCount() + "]";
            case MERGE:
            case CLOSE_MERGE_REQUEST:
                return projectName + "  "+ getSourceBranch() + "->" + getTargetBranch()+"  " +  getState();
            case CREATE_TAG:
                return projectName + " Tag " + desc + " created";
            default:
        }
        return null;
    }
}
