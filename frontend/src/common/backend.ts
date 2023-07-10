import AxiosStatic, { AxiosRequestConfig, AxiosRequestHeaders, AxiosResponse } from 'axios';
import { JdenticonConfig, toSvg } from 'jdenticon';
import { base64 } from 'rfc4648';
import authPromise from './auth';
import config, { backendBaseURL } from './config';
import { VaultKeys } from './crypto';
import { JWTHeader } from './jwt';
import { Deferred, debounce } from './util';

const axiosBaseCfg: AxiosRequestConfig = {
  baseURL: backendBaseURL,
  headers: {
    'Content-Type': 'application/json'
  }
};

/**
 * Any mutating requests require authentication. We use the Authorization header,
 * which transports the Bearer Token and doubles as a CSRF-Protection.
 * See https://security.stackexchange.com/a/177174/78702
 */
const axiosAuth = AxiosStatic.create(axiosBaseCfg);
axiosAuth.interceptors.request.use(async request => {
  try {
    const token = await authPromise.then(auth => auth.bearerToken());
    if (request.headers) {
      request.headers['Authorization'] = `Bearer ${token}`;
    } else {
      request.headers = { 'Authorization': `Bearer ${token}` } as AxiosRequestHeaders;
    }
    return request;
  } catch (err: unknown) {
    // only things from auth module can throw errors here
    throw new UnauthorizedError();
  }
});

/* DTOs */

export type VaultDto = {
  id: string;
  name: string;
  description: string;
  archived: boolean;
  creationTime: Date;
  masterkey: string;
  iterations: number;
  salt: string;
  authPublicKey: string;
  authPrivateKey: string;
};

export type DeviceDto = {
  id: string;
  name: string;
  type: 'BROWSER' | 'DESKTOP' | 'MOBILE';
  publicKey: string;
  accessTo: VaultDto[];
  creationTime: Date;
};

enum AuthorityType {
  User = 'USER',
  Group = 'GROUP'
}

export abstract class AuthorityDto {
  private _pictureUrl?: string;

  constructor(public id: string, public name: string, public type: AuthorityType, pictureUrl?: string) {
    this._pictureUrl = pictureUrl;
  }

  public get pictureUrl() {
    if (this._pictureUrl) {
      return this._pictureUrl;
    } else {
      const svg = toSvg(this.id, 100, this.getIdenticonConfig());
      const bytes = new TextEncoder().encode(svg);
      return `data:image/svg+xml;base64,${base64.stringify(bytes)}`;
    }
  }

  abstract getIdenticonConfig(): JdenticonConfig;
}

export class UserDto extends AuthorityDto {
  constructor(public id: string, public name: string, public type: AuthorityType, public email: string, public devices: DeviceDto[], pictureUrl?: string) {
    super(id, name, type, pictureUrl);
  }

  getIdenticonConfig(): JdenticonConfig {
    return {
      hues: [6, 28, 48, 121, 283],
      saturation: {
        color: 0.59,
      },
      lightness: {
        color: [0.32, 0.49],
        grayscale: [0.32, 0.49]
      },
      backColor: '#F7F7F7',
      padding: 0
    };
  }

  static typeOf(obj: any): obj is UserDto {
    const userDto = obj as UserDto;
    return typeof userDto.id === 'string'
      && typeof userDto.name === 'string'
      && (typeof userDto.pictureUrl === 'string' || userDto.pictureUrl === null)
      && userDto.type === AuthorityType.User;
  }

  static copy(obj: UserDto): UserDto {
    return new UserDto(obj.id, obj.name, obj.type, obj.email, obj.devices, obj.pictureUrl);
  }
}

export class GroupDto extends AuthorityDto {
  constructor(public id: string, public name: string, public type: AuthorityType, pictureUrl: string) {
    super(id, name, type, pictureUrl);
  }

  getIdenticonConfig(): JdenticonConfig {
    return {
      hues: [190],
      saturation: {
        color: 0.59
      },
      lightness: {
        color: [0.81, 0.97],
        grayscale: [0.81, 0.97]
      },
      backColor: '#005E71',
      padding: 0
    };
  }

  static typeOf(obj: any): obj is GroupDto {
    const groupDto = obj as GroupDto;
    return typeof groupDto.id === 'string'
      && typeof groupDto.name === 'string'
      && (typeof groupDto.pictureUrl === 'string' || groupDto.pictureUrl === null)
      && groupDto.type === AuthorityType.Group;
  }

  static copy(obj: GroupDto): GroupDto {
    return new GroupDto(obj.id, obj.name, obj.type, obj.pictureUrl);
  }
}

export type BillingDto = {
  hubId: string;
  hasLicense: boolean;
  email: string;
  totalSeats: number;
  remainingSeats: number;
  issuedAt: Date;
  expiresAt: Date;
  managedInstance: boolean;
}

export type VersionDto = {
  hubVersion: string;
  keycloakVersion: string;
}

export type AuditEventDto = {
  id: number;
  timestamp: Date;
  type: 'CREATE_VAULT' | 'UNLOCK_VAULT' | 'UPDATE_VAULT_MEMBERSHIP';
}

export type CreateVaultEventDto = AuditEventDto & {
  userId: string;
  vaultId: string;
}

export type UnlockVaultEventDto = AuditEventDto & {
  userId: string;
  vaultId: string;
  deviceId: string;
  result: 'SUCCESS' | 'UNAUTHORIZED';
}

export type UpdateVaultMembershipEventDto = AuditEventDto & {
  userId: string;
  vaultId: string;
  authorityId: string;
  operation: 'ADD' | 'REMOVE';
}

/* AuditLogEntityCache */

export class AuditLogEntityCache {
  private static instance: AuditLogEntityCache;

  private vaults: Map<string, Deferred<VaultDto>>;
  private authorities: Map<string, Deferred<AuthorityDto>>;
  private devices: Map<string, Deferred<DeviceDto>>;  

  private constructor() {
    this.vaults = new Map();
    this.authorities = new Map();
    this.devices = new Map();
  }

  public static getInstance(): AuditLogEntityCache {
    if (!AuditLogEntityCache.instance) {
      AuditLogEntityCache.instance = new AuditLogEntityCache();
    }
    return AuditLogEntityCache.instance;
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

  private debouncedResolvePendingVaults = debounce(async () => await this.resolvePendingEntities<VaultDto>(this.vaults, services.vaults.listSome), 100);
  private debouncedResolvePendingAuthorities = debounce(async () => await this.resolvePendingEntities<AuthorityDto>(this.authorities, services.authorities.listSome), 100);
  private debouncedResolvePendingDevices = debounce(async () => await this.resolvePendingEntities<DeviceDto>(this.devices, services.devices.listSome), 100);

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

/* Services */

export interface VaultIdHeader extends JWTHeader {
  vaultId: string;
}

class VaultService {
  public async listAccessible(): Promise<VaultDto[]> {
    return axiosAuth.get('/vaults').then(response => response.data);
  }

  public async listSome(vaultsIds: string[]): Promise<VaultDto[]> {
    const query = `ids=${vaultsIds.join('&ids=')}`;
    return axiosAuth.get(`/vaults/some?${query}`).then(response => response.data);
  }

  public async listAll(): Promise<VaultDto[]> {
    return axiosAuth.get('/vaults/all').then(response => response.data);
  }

  public async get(vaultId: string): Promise<VaultDto> {
    return axiosAuth.get(`/vaults/${vaultId}`)
      .then(response => {
        let dateString = response.data.creationTime;
        response.data.creationTime = new Date(dateString);
        return response.data;
      })
      .catch((error) => rethrowAndConvertIfExpected(error, 404));
  }

  public async getMembers(vaultId: string, vaultKeys: VaultKeys): Promise<(UserDto | GroupDto)[]> {
    let vaultAdminAuthorizationJWT = await this.buildVaultAdminAuthorizationJWT(vaultId, vaultKeys);
    return axiosAuth.get<(UserDto | GroupDto)[]>(`/vaults/${vaultId}/members`, { headers: { 'Cryptomator-Vault-Admin-Authorization': vaultAdminAuthorizationJWT } }).then(response => {
      return response.data.map(authority => {
        if (UserDto.typeOf(authority)) {
          return UserDto.copy(authority);
        } else if (GroupDto.typeOf(authority)) {
          return GroupDto.copy(authority);
        } else {
          throw new Error('Provided data is not of type UserDTO or GroupDTO');
        }
      });
    }).catch(err => rethrowAndConvertIfExpected(err, 403));
  }

  public async addUser(vaultId: string, userId: string, vaultKeys: VaultKeys): Promise<AxiosResponse<void>> {
    let vaultAdminAuthorizationJWT = await this.buildVaultAdminAuthorizationJWT(vaultId, vaultKeys);
    return axiosAuth.put(`/vaults/${vaultId}/users/${userId}`, null, { headers: { 'Cryptomator-Vault-Admin-Authorization': vaultAdminAuthorizationJWT } })
      .catch((error) => rethrowAndConvertIfExpected(error, 404, 409));
  }

  public async addGroup(vaultId: string, groupId: string, vaultKeys: VaultKeys): Promise<AxiosResponse<void>> {
    let vaultAdminAuthorizationJWT = await this.buildVaultAdminAuthorizationJWT(vaultId, vaultKeys);
    return axiosAuth.put(`/vaults/${vaultId}/groups/${groupId}`, null, { headers: { 'Cryptomator-Vault-Admin-Authorization': vaultAdminAuthorizationJWT } })
      .catch((error) => rethrowAndConvertIfExpected(error, 404, 409));
  }

  public async getDevicesRequiringAccessGrant(vaultId: string, vaultKeys: VaultKeys): Promise<DeviceDto[]> {
    let vaultAdminAuthorizationJWT = await this.buildVaultAdminAuthorizationJWT(vaultId, vaultKeys);
    return axiosAuth.get(`/vaults/${vaultId}/devices-requiring-access-grant`, { headers: { 'Cryptomator-Vault-Admin-Authorization': vaultAdminAuthorizationJWT } })
      .then(response => response.data).catch(err => rethrowAndConvertIfExpected(err, 403));
  }

  public async createOrUpdateVault(vaultId: string, name: string, description: string, archived: boolean, masterkey: string, iterations: number, salt: string, signPubKey: string, signPrvKey: string): Promise<VaultDto> {
    const body: VaultDto = { id: vaultId, name: name, description: description, archived: archived, creationTime: new Date(), masterkey: masterkey, iterations: iterations, salt: salt, authPublicKey: signPubKey, authPrivateKey: signPrvKey };
    return axiosAuth.put(`/vaults/${vaultId}`, body)
      .then(response  => response.data)
      .catch((error) => rethrowAndConvertIfExpected(error, 404));
  }

  public async grantAccess(vaultId: string, deviceId: string, jwe: string, vaultKeys: VaultKeys) {
    let vaultAdminAuthorizationJWT = await this.buildVaultAdminAuthorizationJWT(vaultId, vaultKeys);
    await axiosAuth.put(`/vaults/${vaultId}/keys/${deviceId}`, jwe, { headers: { 'Content-Type': 'text/plain', 'Cryptomator-Vault-Admin-Authorization': vaultAdminAuthorizationJWT } })
      .catch((error) => rethrowAndConvertIfExpected(error, 404, 409));
  }

  public async revokeUserAccess(vaultId: string, userId: string, vaultKeys: VaultKeys) {
    let vaultAdminAuthorizationJWT = await this.buildVaultAdminAuthorizationJWT(vaultId, vaultKeys);
    await axiosAuth.delete(`/vaults/${vaultId}/users/${userId}`, { headers: { 'Cryptomator-Vault-Admin-Authorization': vaultAdminAuthorizationJWT } })
      .catch((error) => rethrowAndConvertIfExpected(error, 404));
  }

  private async buildVaultAdminAuthorizationJWT(vaultId: string, vaultKeys: VaultKeys): Promise<string> {
    let vaultIdHeader: VaultIdHeader = { alg: 'ES384', b64: true, typ: 'JWT', vaultId: vaultId };
    let jwtPayload = { iat: this.secondsSinceEpoch() + config.serverTimeDiff };
    return vaultKeys.signVaultEditRequest(vaultIdHeader, jwtPayload);
  }

  private secondsSinceEpoch(): number {
    return Math.floor(Date.now() / 1000);
  }
}
class DeviceService {
  public async listSome(deviceIds: string[]): Promise<DeviceDto[]> {
    const query = `ids=${deviceIds.join('&ids=')}`;
    return axiosAuth.get<DeviceDto[]>(`/devices?${query}`).then(response => response.data);
  }

  public async removeDevice(deviceId: string): Promise<AxiosResponse<any>> {
    return axiosAuth.delete(`/devices/${deviceId}`)
      .catch((error) => rethrowAndConvertIfExpected(error, 404));
  }
}

class UserService {
  public async syncMe(): Promise<void> {
    return axiosAuth.put('/users/me');
  }

  public async me(withDevices: boolean = false, withAccessibleVaults: boolean = false): Promise<UserDto> {
    return axiosAuth.get<UserDto>(`/users/me?withDevices=${withDevices}&withAccessibleVaults=${withAccessibleVaults}`).then(response => UserDto.copy(response.data));
  }

  public async listAll(): Promise<UserDto[]> {
    return axiosAuth.get<UserDto[]>('/users/').then(response => {
      return response.data.map(dto => UserDto.copy(dto));
    });
  }
}

class AuthorityService {
  public async search(query: string): Promise<(UserDto | GroupDto)[]> {
    return axiosAuth.get<(UserDto | GroupDto)[]>(`/authorities/search?query=${query}`).then(response => {
      return response.data.map(authority => {
        if (UserDto.typeOf(authority)) {
          return UserDto.copy(authority);
        } else if (GroupDto.typeOf(authority)) {
          return GroupDto.copy(authority);
        } else {
          throw new Error('Provided data is not of type UserDTO or GroupDTO');
        }
      });
    });
  }

  public async listSome(authorityIds: string[]): Promise<AuthorityDto[]> {
    const query = `ids=${authorityIds.join('&ids=')}`;
    return axiosAuth.get<AuthorityDto[]>(`/authorities?${query}`).then(response => response.data);
  }
}

class BillingService {
  public async get(): Promise<BillingDto> {
    return axiosAuth.get('/billing').then(response => {
      response.data.issuedAt = new Date(response.data.issuedAt);
      response.data.expiresAt = new Date(response.data.expiresAt);
      return response.data;
    });
  }

  public async setToken(token: string): Promise<void> {
    return axiosAuth.put('/billing/token', token, { headers: { 'Content-Type': 'text/plain' } });
  }
}

class VersionService {
  public async get(): Promise<VersionDto> {
    return axiosAuth.get<VersionDto>('/version').then(response => response.data);
  }
}

class AuditLogService {
  public async getAllEvents(startDate: Date, endDate: Date, paginationId: number, order: string, pageSize: number): Promise<AuditEventDto[]> {
    return axiosAuth.get<AuditEventDto[]>(`/auditlog?startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}&paginationId=${paginationId}&order=${order}&pageSize=${pageSize}`)
      .then(response => response.data.map(dto => {
        dto.timestamp = new Date(dto.timestamp);
        return dto;
      }));
  }
}

/**
 * Note: Each service can thrown an {@link UnauthorizedError} when the access token is expired!
 */
const services = {
  vaults: new VaultService(),
  users: new UserService(),
  authorities: new AuthorityService(),
  devices: new DeviceService(),
  billing: new BillingService(),
  version: new VersionService(),
  auditLogs: new AuditLogService()
};

function convertExpectedToBackendError(status: number): BackendError {
  switch (status) {
    case 403:
      return new ForbiddenError();
    case 404:
      return new NotFoundError();
    case 409:
      return new ConflictError();
    default:
      return new BackendError('Status Code ${status} not mapped');
  }
}

/**
 * Rethrows the error object or, if 'error' is an response with an expected http status code, it is converted to an BackendError and then rethrown.
 * @param error A thrown object
 * @param expectedStatusCodes The expected http status codes of the backend call
 */
function rethrowAndConvertIfExpected(error: unknown, ...expectedStatusCodes: number[]): Promise<any> {
  if (AxiosStatic.isAxiosError(error) && error.response != null && expectedStatusCodes.includes(error.response.status)) {
    throw convertExpectedToBackendError(error.response.status);
  } else {
    throw error;
  }
}

export default services;

//-- Error thrown by this module --
export class BackendError extends Error {
  constructor(msg: string) {
    super(msg);
  }
}

export class UnauthorizedError extends BackendError {
  constructor() {
    super('Unauthorized');
  }
}

export class ForbiddenError extends BackendError {
  constructor() {
    super('Not authorized to access resource');
  }
}

export class NotFoundError extends BackendError {
  constructor() {
    super('Requested resource not found');
  }
}

export class ConflictError extends BackendError {
  constructor() {
    super('Resource already exists');
  }
}
