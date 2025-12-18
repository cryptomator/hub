package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "audit_event_user_keys_change")
@DiscriminatorValue(UserKeysChangeEvent.TYPE)
public class UserKeysChangeEvent extends AuditEvent {

	public static final String TYPE = "USER_KEYS_CHANGE";

	@Column(name = "changed_by")
	private String changedBy;

	@Column(name = "user_name")
	private String userName;

	public String getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(String changedBy) {
		this.changedBy = changedBy;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
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
