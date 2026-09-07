package org.cryptomator.hub.entities.events;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@Table(name = "audit_event_user_account_reset")
@DiscriminatorValue(UserAccountResetEvent.TYPE)
public class UserAccountResetEvent extends AuditEvent {

	public static final String TYPE = "USER_ACCOUNT_RESET";

	@Column(name = "reset_by")
	private @Nullable String resetBy;

	public @Nullable String getResetBy() {
		return resetBy;
	}

	public void setResetBy(@Nullable String resetBy) {
		this.resetBy = resetBy;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		UserAccountResetEvent that = (UserAccountResetEvent) o;
		return Objects.equals(resetBy, that.resetBy);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), resetBy);
	}
}
