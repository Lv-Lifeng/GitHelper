package gitlab.enums;

import lombok.Getter;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/19 01:05
 */
@Getter
public enum StatusEnum {

    SUCCESSED("GitLab Operation Successful:"),
    FAILED("GitLab Operation Failed:"),
    ;


    StatusEnum(String desc) {
        this.desc = desc;
    }

    private String desc;
}
