package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.cryptomator.hub.entities.LegacyDevice;

@ApplicationScoped
@Deprecated
public class LegacyDeviceRepository implements PanacheRepositoryBase<LegacyDevice, String> {
}
