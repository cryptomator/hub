package org.cryptomator.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.cryptomator.hub.entities.Group;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class GroupDto extends AuthorityDto {

	@JsonProperty("memberSize")
	public final @Nullable Integer memberSize;

	@JsonProperty("vaultCount")
	public final @Nullable Integer vaultCount;

	GroupDto(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("pictureUrl") @Nullable String pictureUrl, @JsonProperty("memberSize") @Nullable Integer memberSize, @JsonProperty("vaultCount") @Nullable Integer vaultCount) {
		super(id, Type.GROUP, name, pictureUrl);
		this.memberSize = memberSize;
		this.vaultCount = vaultCount;
	}

	public static GroupDto fromEntity(Group group) {
		return fromEntity(group, false);
	}

	public static GroupDto fromEntity(Group group, boolean withMemberSize) {
		Integer memberSize = withMemberSize ? group.getMemberSize() : null;
		return new GroupDto(group.getId(), group.getName(), group.getPictureUrl(), memberSize, null);
	}

	public static GroupDto fromEntity(Group group, Integer memberSize, Integer vaultCount) {
		return new GroupDto(group.getId(), group.getName(), group.getPictureUrl(), memberSize, vaultCount);
	}

	WithDetails withDetails(List<AuthorityDto> members, List<VaultResource.VaultDtoWithRole> vaults) {
		return new WithDetails(this, members, vaults);
	}

	public record WithDetails(
			@JsonUnwrapped GroupDto group,
			@JsonProperty("members") List<AuthorityDto> members,
			@JsonProperty("vaults") List<VaultResource.VaultDtoWithRole> vaults
	) {
	}
}
