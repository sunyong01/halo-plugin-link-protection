package syweb.halo.link.protect;

import lombok.Data;
import java.util.List;

@Data
public class LinkProtectSetting {

    // BasicGroup
    private String protectedResourceScope;
    private String resourceServiceAddress;

    // Timestamp Anti-Leech Group
    private Boolean enableTimestampAntiLeech;
    private String expirationTimeMinutes;
    private String authKeys;
    private String paramFieldName;

    public List<String> getType() {
        return List.of(protectedResourceScope.split(","));
    }

}