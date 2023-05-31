import AxiosStatic, { AxiosHeaders, AxiosRequestConfig, AxiosResponse } from 'axios';
import { JdenticonConfig, toSvg } from 'jdenticon';
import { base64 } from 'rfc4648';
import authPromise from './auth';
import config, { backendBaseURL } from './config';
import { VaultKeys } from './crypto';
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
const axiosAuth = AxiosStatic.create(axiosBaseCfg);
axiosAuth.interceptors.request.use(async request => {
  try {
    const token = await authPromise.then(auth => auth.bearerToken());
    if (request.headers) {
      request.headers.setAuthorization(`Bearer ${token}`);
    } else {
      request.headers = new AxiosHeaders({ 'Authorization': `Bearer ${token}` });
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
  publicKey: string; // note: base64url-encoded for historic reasons
  userKeyJwe: string;
  creationTime: Date;
  lastSeenTime: Date;
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
    public publicKey?: string, public recoveryJwe?: string, public recoveryPbkdf2?: string, public recoverySalt?: string, public recoveryIterations?: number) {
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
    return new UserDto(obj.id, obj.name, obj.type, obj.email, obj.devices, obj.accessibleVaults, obj.pictureUrl, obj.publicKey, obj.recoveryJwe, obj.recoveryPbkdf2, obj.recoverySalt, obj.recoveryIterations);
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

/* Services */

export interface VaultIdHeader extends JWTHeader {
  vaultId: string;
}

class VaultService {
  public async listAccessible(): Promise<VaultDto[]> {
    return axiosAuth.get('/vaults').then(response => response.data);
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

  public async getUsersRequiringAccessGrant(vaultId: string, vaultKeys: VaultKeys): Promise<UserDto[]> {
    let vaultAdminAuthorizationJWT = await this.buildVaultAdminAuthorizationJWT(vaultId, vaultKeys);
    return axiosAuth.get(`/vaults/${vaultId}/users-requiring-access-grant`, { headers: { 'Cryptomator-Vault-Admin-Authorization': vaultAdminAuthorizationJWT } })
      .then(response => response.data).catch(err => rethrowAndConvertIfExpected(err, 403));
  }

  public async createVault(vaultId: string, name: string, description: string, masterkey: string, iterations: number, salt: string, signPubKey: string, signPrvKey: string): Promise<AxiosResponse<any>> {
    const body: VaultDto = { id: vaultId, name: name, description: description, creationTime: new Date(), masterkey: masterkey, iterations: iterations, salt: salt, authPublicKey: signPubKey, authPrivateKey: signPrvKey };
    return axiosAuth.put(`/vaults/${vaultId}`, body)
      .catch((error) => rethrowAndConvertIfExpected(error, 404, 409));
  }

  public async grantAccess(vaultId: string, userId: string, jwe: string, vaultKeys: VaultKeys) {
    let vaultAdminAuthorizationJWT = await this.buildVaultAdminAuthorizationJWT(vaultId, vaultKeys);
    await axiosAuth.put(`/vaults/${vaultId}/user-tokens/${userId}`, jwe, { headers: { 'Content-Type': 'text/plain', 'Cryptomator-Vault-Admin-Authorization': vaultAdminAuthorizationJWT } })
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
