package gitlab.bean;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import lombok.Setter;
import org.gitlab.api.models.GitlabUser;

import java.util.Map;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/9 11:25
 */

@Getter
@Setter
public class User extends GitlabUser {

    private Map<String, Integer> serverUserIdMap;


    public User resetId(String apiUrl) {
        super.setId(serverUserIdMap.getOrDefault(apiUrl, null));
        return this;
    }

    @Override
    public String toString() {
        return this.getName() + "@" + this.getUsername();
    }
}
