package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cryptomator.hub.entities.Group;

public final class GroupDto extends AuthorityDto {

    private final int memberCount;

    GroupDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("memberCount") int memberCount) {
        super(id, Type.GROUP, name, null);
        this.memberCount = memberCount;
    }

    @JsonProperty("memberCount")
    public int getMemberCount() {
        return memberCount;
    }

    public static GroupDto fromEntity(Group group, int memberCount) {
        return new GroupDto(group.getId(), group.getName(), memberCount);
    }
}
