import backend, { AuthorityDto, DeviceDto, VaultDto, axiosAuth } from './backend';
import { Deferred, debounce } from './util';

/* DTOs */

export type AuditEventDto = {
  id: number;
  timestamp: Date;
  type: 'CREATE_VAULT' | 'GRANT_VAULT_ACCESS' | 'REGISTER_DEVICE' | 'REMOVE_DEVICE' | 'UNLOCK_VAULT' | 'UPDATE_VAULT' | 'UPDATE_VAULT_MEMBERSHIP';
}

export type CreateVaultEventDto = AuditEventDto & {
  userId: string;
  vaultId: string;
  vaultName: string;
  vaultDescription: string;
}

export type GrantVaultAccessEventDto = AuditEventDto & {
  userId: string;
  vaultId: string;
  authorityId: string;
}

export type RegisterDeviceEventDto = AuditEventDto & {
  userId: string;
  deviceId: string;
  deviceName: string;
  deviceType: 'BROWSER' | 'DESKTOP' | 'MOBILE';
}

export type RemoveDeviceEventDto = AuditEventDto & {
  userId: string;
  deviceId: string;
}

export type UnlockVaultEventDto = AuditEventDto & {
  userId: string;
  vaultId: string;
  deviceId: string;
  result: 'SUCCESS' | 'UNAUTHORIZED';
}

export type UpdateVaultEventDto = AuditEventDto & {
  userId: string;
  vaultId: string;
  vaultName: string;
  vaultDescription: string;
  vaultArchived: boolean;
}

export type UpdateVaultMembershipEventDto = AuditEventDto & {
  userId: string;
  vaultId: string;
  authorityId: string;
  operation: 'ADD' | 'REMOVE';
}

/* Entity Cache */

export class AuditLogEntityCache {
  private vaults: Map<string, Deferred<VaultDto>>;
  private authorities: Map<string, Deferred<AuthorityDto>>;
  private devices: Map<string, Deferred<DeviceDto>>;  

  constructor() {
    this.vaults = new Map();
    this.authorities = new Map();
    this.devices = new Map();
  }

  public async getVault(vaultId: string): Promise<VaultDto> {
    return this.getEntity<VaultDto>(vaultId, this.vaults, this.debouncedResolvePendingVaults);
  }

  public async getAuthority(authorityId: string): Promise<AuthorityDto> {
    return this.getEntity<AuthorityDto>(authorityId, this.authorities, this.debouncedResolvePendingAuthorities);
  }

  public async getDevice(deviceId: string): Promise<DeviceDto> {
    return this.getEntity<DeviceDto>(deviceId, this.devices, this.debouncedResolvePendingDevices);
  }

  private async getEntity<T>(entityId: string, entities: Map<string, Deferred<T>>, debouncedResolvePendingEntities: Function): Promise<T> {
    const cachedEntity = entities.get(entityId);
    if (!cachedEntity) {  
      const deferredEntity = new Deferred<T>();
      entities.set(entityId, deferredEntity);
      debouncedResolvePendingEntities();
      return deferredEntity.promise;
    } else {
      return cachedEntity.promise;
    }
  }

  private debouncedResolvePendingVaults = debounce(async () => await this.resolvePendingEntities<VaultDto>(this.vaults, backend.vaults.listSome), 100);
  private debouncedResolvePendingAuthorities = debounce(async () => await this.resolvePendingEntities<AuthorityDto>(this.authorities, backend.authorities.listSome), 100);
  private debouncedResolvePendingDevices = debounce(async () => await this.resolvePendingEntities<DeviceDto>(this.devices, backend.devices.listSome), 100);

  private async resolvePendingEntities<T extends { id: string }>(entities: Map<string, Deferred<T>>, listSome: (ids: string[]) => Promise<T[]>): Promise<void> {
    const pendingEntities = Array.from(entities.entries()).filter(([_, v]) => v.status === 'pending');
    const entitiesResult = await listSome(pendingEntities.map(([k, _]) => k));
    for (const [entityId, deferredEntity] of pendingEntities) {
      const entity = entitiesResult.find(v => v.id === entityId);
      if (entity) {
        deferredEntity.resolve(entity);
      } else {
        deferredEntity.reject(new Error(`Entity ${entityId} not found`));
      }
    }
  }

  public invalidateAll() {
    this.vaults.clear();
    this.authorities.clear();
    this.devices.clear();
  }
}

/* Service */

class AuditLogService {
  public async getAllEvents(startDate: Date, endDate: Date, paginationId: number, order: string, pageSize: number): Promise<AuditEventDto[]> {
    return axiosAuth.get<AuditEventDto[]>(`/auditlog?startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}&paginationId=${paginationId}&order=${order}&pageSize=${pageSize}`)
      .then(response => response.data.map(dto => {
        dto.timestamp = new Date(dto.timestamp);
        return dto;
      }));
  }
}

/* Export */

const auditlog = {
  entityCache: new AuditLogEntityCache(),
  service: new AuditLogService()
};

export default auditlog;
