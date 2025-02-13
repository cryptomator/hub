package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "audit_event_user_setupcode_change")
@DiscriminatorValue(UserSetupCodeChangeEvent.TYPE)
public class UserSetupCodeChangeEvent extends AuditEvent {

	public static final String TYPE = "USER_SETUP_CODE_CHANGE";

	@Column(name = "changed_by")
	private String changedBy;

	public String getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(String changedBy) {
		this.changedBy = changedBy;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		UserSetupCodeChangeEvent that = (UserSetupCodeChangeEvent) o;
		return Objects.equals(changedBy, that.changedBy);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), changedBy);
	}
}
