import backend, { AuthorityDto, DeviceDto, VaultDto, axiosAuth, rethrowAndConvertIfExpected } from './backend';
import { Deferred, debounce } from './util';

/* DTOs */

export type AuditEventDto = {
  id: number;
  timestamp: Date;
  type: 'DEVICE_REGISTER' | 'DEVICE_REMOVE' | 'VAULT_CREATE' | 'VAULT_UPDATE' | 'VAULT_ACCESS_GRANT' | 'VAULT_KEY_RETRIEVE' | 'VAULT_MEMBER_ADD' | 'VAULT_MEMBER_REMOVE';
}

export type AuditEventDeviceRegisterDto = AuditEventDto & {
  registeredBy: string;
  deviceId: string;
  deviceName: string;
  deviceType: 'BROWSER' | 'DESKTOP' | 'MOBILE';
}

export type AuditEventDeviceRemoveDto = AuditEventDto & {
  removedBy: string;
  deviceId: string;
}

export type AuditEventVaultCreateDto = AuditEventDto & {
  createdBy: string;
  vaultId: string;
  vaultName: string;
  vaultDescription: string;
}

export type AuditEventVaultUpdateDto = AuditEventDto & {
  updatedBy: string;
  vaultId: string;
  vaultName: string;
  vaultDescription: string;
  vaultArchived: boolean;
}

export type AuditEventVaultAccessGrantDto = AuditEventDto & {
  grantedBy: string;
  vaultId: string;
  authorityId: string;
}

export type AuditEventVaultKeyRetrieveDto = AuditEventDto & {
  retrievedBy: string;
  vaultId: string;
  result: 'SUCCESS' | 'UNAUTHORIZED';
}

export type AuditEventVaultMemberAddDto = AuditEventDto & {
  addedBy: string;
  vaultId: string;
  authorityId: string;
  role: 'MEMBER' | 'OWNER';
}

export type AuditEventVaultMemberRemoveDto = AuditEventDto & {
  removedBy: string;
  vaultId: string;
  authorityId: string;
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
      }))
      .catch((error) => rethrowAndConvertIfExpected(error, 402));
  }
}

/* Export */

const auditlog = {
  entityCache: new AuditLogEntityCache(),
  service: new AuditLogService()
};

export default auditlog;
