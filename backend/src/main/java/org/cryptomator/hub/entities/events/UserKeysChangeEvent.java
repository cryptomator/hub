package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@Table(name = "audit_event_user_keys_change")
@DiscriminatorValue(UserKeysChangeEvent.TYPE)
public class UserKeysChangeEvent extends AuditEvent {

	public static final String TYPE = "USER_KEYS_CHANGE";

	@Column(name = "changed_by")
	private @Nullable String changedBy;

	@Column(name = "user_name")
	private @Nullable String userName;

	public @Nullable String getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(@Nullable String changedBy) {
		this.changedBy = changedBy;
	}

	public @Nullable String getUserName() {
		return userName;
	}

	public void setUserName(@Nullable String userName) {
		this.userName = userName;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		UserKeysChangeEvent that = (UserKeysChangeEvent) o;
		return Objects.equals(changedBy, that.changedBy) && Objects.equals(userName, that.userName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), changedBy, userName);
	}

}
