package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "wot")
public class WotEntry {

	@EmbeddedId
	private Id id;

	@Column(name = "signature", nullable = false)
	private String signature;

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	@Embeddable
	public record Id(@Column(name = "user_id") String userId, @Column(name = "signer_id") String signerId) {
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<WotEntry, Id> {
	}
}
