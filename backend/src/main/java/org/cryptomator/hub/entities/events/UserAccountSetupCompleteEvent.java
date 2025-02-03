package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "audit_event_user_account_setup_complete")
@DiscriminatorValue(UserAccountSetupCompleteEvent.TYPE)
public class UserAccountSetupCompleteEvent extends AuditEvent {

	public static final String TYPE = "USER_ACCOUNT_SETUP_COMPLETE";

	@Column(name = "completed_by")
	private String completedBy;

	public String getCompletedBy() {
		return completedBy;
	}

	public void setCompletedBy(String completedBy) {
		this.completedBy = completedBy;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		UserAccountSetupCompleteEvent that = (UserAccountSetupCompleteEvent) o;
		return Objects.equals(completedBy, that.completedBy);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), completedBy);
	}
}
