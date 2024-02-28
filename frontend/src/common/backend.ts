import AxiosStatic, { AxiosHeaders, AxiosRequestConfig, AxiosResponse } from 'axios';
import { JdenticonConfig, toSvg } from 'jdenticon';
import { base64 } from 'rfc4648';
import authPromise from './auth';
import { backendBaseURL } from './config';
import { JWTHeader } from './jwt';

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
export const axiosAuth = AxiosStatic.create(axiosBaseCfg);
axiosAuth.interceptors.request.use(async request => {
  try {
    const token = await authPromise.then(auth => auth.bearerToken());
    if (request.headers) {
      request.headers.setAuthorization(`Bearer ${token}`);
    } else {
      request.headers = AxiosHeaders.from({ 'Authorization': `Bearer ${token}` });
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
  description?: string;
  archived: boolean;
  creationTime: Date;
  masterkey?: string;
  iterations?: number;
  salt?: string;
  authPublicKey?: string;
  authPrivateKey?: string;
  metadata: string;
};

export type DeviceDto = {
  id: string;
  name: string;
  type: 'BROWSER' | 'DESKTOP' | 'MOBILE';
  publicKey: string;
  userPrivateKey: string;
  creationTime: Date;
};

export type VaultRole = 'MEMBER' | 'OWNER';

export type AccessGrant = {
  userId: string,
  token: string
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
  constructor(public id: string, public name: string, public type: AuthorityType, public email: string, public devices: DeviceDto[], public accessibleVaults: VaultDto[], pictureUrl?: string,
    public publicKey?: string, public privateKey?: string, public setupCode?: string) {
    super(id, name, type, pictureUrl);
  }

  static getIdenticonConfig(): JdenticonConfig {
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

  getIdenticonConfig(): JdenticonConfig {
    return UserDto.getIdenticonConfig();
  }

  static typeOf(obj: any): obj is UserDto {
    const userDto = obj as UserDto;
    return typeof userDto.id === 'string'
      && typeof userDto.name === 'string'
      && (typeof userDto.pictureUrl === 'string' || userDto.pictureUrl === null)
      && userDto.type === AuthorityType.User;
  }

  static copy(obj: UserDto): UserDto {
    return new UserDto(obj.id, obj.name, obj.type, obj.email, obj.devices, obj.accessibleVaults, obj.pictureUrl, obj.publicKey, obj.privateKey, obj.setupCode);
  }
}

export class GroupDto extends AuthorityDto {
  constructor(public id: string, public name: string, public type: AuthorityType, pictureUrl: string) {
    super(id, name, type, pictureUrl);
  }

  static getIdenticonConfig(): JdenticonConfig {
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

  getIdenticonConfig(): JdenticonConfig {
    return GroupDto.getIdenticonConfig();
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

export class MemberDto extends AuthorityDto {
  constructor(public id: string, public name: string, public type: AuthorityType, public role: VaultRole, pictureUrl: string) {
    super(id, name, type, pictureUrl);
  }

  getIdenticonConfig(): JdenticonConfig {
    switch (this.type) {
      case AuthorityType.User:
        return UserDto.getIdenticonConfig();
      case AuthorityType.Group:
        return GroupDto.getIdenticonConfig();
    }
  }
}

export type BillingDto = {
  hubId: string;
  hasLicense: boolean;
  email: string;
  licensedSeats: number;
  usedSeats: number;
  issuedAt: Date;
  expiresAt: Date;
  managedInstance: boolean;
}

export type VersionDto = {
  hubVersion: string;
  keycloakVersion: string;
}

export type ConfigDto = {
    keycloakUrl: string;
    keycloakRealm: string;
    keycloakClientIdHub: string;
    keycloakClientIdCryptomator: string;
    keycloakAuthEndpoint: string;
    keycloakTokenEndpoint: string;
    serverTime: string;
    apiLevel: number;
    uuid: string;
}

/* Services */

export interface VaultIdHeader extends JWTHeader {
  vaultId: string;
}

class VaultService {
  public async listAccessible(role?: 'MEMBER' | 'OWNER'): Promise<VaultDto[]> {
    const queryParams = role ? { role: role } : {};
    return axiosAuth.get('/vaults/accessible', { params: queryParams }).then(response => response.data);
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

  public async getMembers(vaultId: string): Promise<MemberDto[]> {
    return axiosAuth.get<MemberDto[]>(`/vaults/${vaultId}/members`).then(response => {
      return response.data.map(member => new MemberDto(member.id, member.name, member.type, member.role, member.pictureUrl));
    }).catch(err => rethrowAndConvertIfExpected(err, 403));
  }

  public async addUser(vaultId: string, userId: string, role?: VaultRole): Promise<AxiosResponse<void>> {
    return axiosAuth.put(`/vaults/${vaultId}/users/${userId}` + (role ? `?role=${role}` : ''))
      .catch((error) => rethrowAndConvertIfExpected(error, 402, 404, 409));
  }

  public async addGroup(vaultId: string, groupId: string, role?: VaultRole): Promise<AxiosResponse<void>> {
    return axiosAuth.put(`/vaults/${vaultId}/groups/${groupId}` + (role ? `?role=${role}` : ''))
      .catch((error) => rethrowAndConvertIfExpected(error, 402, 404, 409));
  }

  public async getUsersRequiringAccessGrant(vaultId: string): Promise<UserDto[]> {
    return axiosAuth.get<UserDto[]>(`/vaults/${vaultId}/users-requiring-access-grant`)
      .then(response => {
        return response.data.map(dto => UserDto.copy(dto));
      })
      .catch(err => rethrowAndConvertIfExpected(err, 403));
  }

  public async createOrUpdateVault(vaultId: string, name: string, archived: boolean
  , metadata: string
  , description?: string): Promise<VaultDto> {
    const body: VaultDto = { id: vaultId, name: name, description: description, archived: archived, creationTime: new Date()
    , metadata: metadata
    };
    return axiosAuth.put(`/vaults/${vaultId}`, body)
      .then(response => response.data)
      .catch((error) => rethrowAndConvertIfExpected(error, 402, 404));
  }

  public async claimOwnership(vaultId: string, proof: string): Promise<VaultDto> {
    const params = new URLSearchParams({ proof: proof });
    return axiosAuth.post(`/vaults/${vaultId}/claim-ownership`, params, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
      .then(response => response.data)
      .catch((error) => rethrowAndConvertIfExpected(error, 400, 404, 409));
  }

  public async accessToken(vaultId: string, evenIfArchived = false): Promise<string> {
    return axiosAuth.get(`/vaults/${vaultId}/access-token?evenIfArchived=${evenIfArchived}`, { headers: { 'Content-Type': 'text/plain' } })
      .then(response => response.data)
      .catch((error) => rethrowAndConvertIfExpected(error, 402, 403));
  }

  public async grantAccess(vaultId: string, ...grants: AccessGrant[]) {
    const body = grants.reduce<Record<string, string>>((accumulator, curr) => {
      accumulator[curr.userId] = curr.token;
      return accumulator;
    }, {});
    await axiosAuth.post(`/vaults/${vaultId}/access-tokens`, body)
      .catch((error) => rethrowAndConvertIfExpected(error, 402, 403, 404, 409));
  }

  public async removeAuthority(vaultId: string, authorityId: string) {
    await axiosAuth.delete(`/vaults/${vaultId}/authority/${authorityId}`)
      .catch((error) => rethrowAndConvertIfExpected(error, 404));
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

  public async putDevice(device: DeviceDto): Promise<AxiosResponse<any>> {
    return axiosAuth.put(`/devices/${device.id}`, device);
  }
}

class UserService {
  public async putMe(dto?: UserDto): Promise<void> {
    return axiosAuth.put('/users/me', dto);
  }

  public async me(withDevices: boolean = false): Promise<UserDto> {
    return axiosAuth.get<UserDto>(`/users/me?withDevices=${withDevices}`).then(response => UserDto.copy(response.data));
  }

  public async resetMe(): Promise<void> {
    return axiosAuth.post('/users/me/reset');
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

/**
 * Note: Each service can thrown an {@link UnauthorizedError} when the access token is expired!
 */
const services = {
  vaults: new VaultService(),
  users: new UserService(),
  authorities: new AuthorityService(),
  devices: new DeviceService(),
  billing: new BillingService(),
  version: new VersionService()
};

function convertExpectedToBackendError(status: number): BackendError {
  switch (status) {
    case 402:
      return new PaymentRequiredError();
    case 403:
      return new ForbiddenError();
    case 404:
      return new NotFoundError();
    case 409:
      return new ConflictError();
    default:
      return new BackendError(`Status code ${status} not mapped`);
  }
}

/**
 * Rethrows the error object or, if 'error' is an response with an expected http status code, it is converted to an BackendError and then rethrown.
 * @param error A thrown object
 * @param expectedStatusCodes The expected http status codes of the backend call
 */
export function rethrowAndConvertIfExpected(error: unknown, ...expectedStatusCodes: number[]): Promise<any> {
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
    super('Unauthorized to access resource');
  }
}

export class PaymentRequiredError extends BackendError {
  constructor() {
    super('Payment required to access resource');
  }
}

export class ForbiddenError extends BackendError {
  constructor() {
    super('Insufficient rights to access resource');
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

export type VaultMetadataJWEAutomaticAccessGrantDto = {
    enabled: boolean,
    maxWotDepth: number
}

export type VaultMetadataJWEDto = {
    fileFormat: string;
    nameFormat: string;
    keys: Record<string,string>;
    latestFileKey: string;
    nameKey: string;
    kdf: string;
    automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto;
}