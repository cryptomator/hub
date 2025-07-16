import AxiosStatic, { AxiosHeaders, AxiosRequestConfig, AxiosResponse } from 'axios';
import { JdenticonConfig, toSvg } from 'jdenticon';
import { base64 } from 'rfc4648';
import authPromise from './auth';
import { backendBaseURL } from './config';
import { JWTHeader } from './jwt';
import { UTF8 } from './util';

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
  } catch {
    // only things from auth module can throw errors here
    throw new UnauthorizedError();
  }
});

// #region DTOs

export type VaultDto = {
  id: string;
  name: string;
  creationTime: Date;
  description?: string;
  archived: boolean;
  requiredEmergencyKeyShares: number;
  emergencyKeyShares: Record<string, string>;
  
  // Legacy properties ("Vault Admin Password"):
  masterkey?: string;
  iterations?: number;
  salt?: string;
  authPublicKey?: string;
  authPrivateKey?: string;
};

export type DeviceDto = {
  id: string;
  name: string;
  type: 'BROWSER' | 'DESKTOP' | 'MOBILE';
  publicKey: string;
  userPrivateKey: string;
  creationTime: Date;
  lastIpAddress?: string;
  lastAccessTime?: Date;
  legacyDevice?: boolean;
};

export type VaultRole = 'MEMBER' | 'OWNER';

export type AccessGrant = {
  userId: string,
  token: string
};

export type UserDto = {
  type: 'USER';
  id: string;
  name: string;
  pictureUrl?: string;
  email: string;
  language?: string;
  devices: DeviceDto[];
  accessibleVaults: VaultDto[];
  ecdhPublicKey?: string;
  ecdsaPublicKey?: string;
  privateKeys?: string;
  setupCode?: string;
}

export type GroupDto = {
  type: 'GROUP';
  id: string;
  name: string;
  pictureUrl?: string;
  memberSize?: number;
}

export type AuthorityDto = UserDto | GroupDto;

export type MemberDto = AuthorityDto & {
  role: VaultRole
}

export type TrustDto = {
  trustedUserId: string,
  signatureChain: string[]
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

export type SettingsDto = {
  hubId: string,
  wotMaxDepth: number,
  wotIdVerifyLen: number,
  defaultRequiredEmergencyKeyShares: number,
  allowChoosingEmergencyCouncil: boolean,
  emergencyCouncilMemberIds: string[]
}

export class LicenseUserInfoDto {
  constructor(
    public licensedSeats: number,
    public usedSeats: number,
    public expiresAt: Date | null) {
  }

  public isExpired(): boolean {
    const now = new Date();
    return now > (this.expiresAt ?? now); //if expired is null, the license cannot expire
  }

  public isExceeded(): boolean {
    return this.usedSeats > this.licensedSeats;
  }
}

export interface VaultIdHeader extends JWTHeader {
  vaultId: string;
}

// #endregion DTOs
// #region Services

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
        const dateString = response.data.creationTime;
        response.data.creationTime = new Date(dateString);
        return response.data;
      })
      .catch((error) => rethrowAndConvertIfExpected(error, 404));
  }

  public async getMembers(vaultId: string): Promise<MemberDto[]> {
    return axiosAuth.get<MemberDto[]>(`/vaults/${vaultId}/members`).then(response => response.data.map(AuthorityService.fillInMissingPicture)).catch(err => rethrowAndConvertIfExpected(err, 403));
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
      .then(response => response.data.map(AuthorityService.fillInMissingPicture))
      .catch(err => rethrowAndConvertIfExpected(err, 403));
  }

  public async createOrUpdateVault(vaultId: string, name: string, archived: boolean, requiredEmergencyKeyShares: number, emergencyKeyShares: Record<string, string>, description?: string): Promise<VaultDto> {
    const body: VaultDto = {
      id: vaultId,
      name: name,
      creationTime: new Date(),
      description: description,
      archived: archived,
      requiredEmergencyKeyShares: requiredEmergencyKeyShares,
      emergencyKeyShares: emergencyKeyShares
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

  public async accessToken(vaultId: string, deviceId?: string, evenIfArchived = false): Promise<string> {
    const headers: Record<string, string> = { 'Content-Type': 'text/plain' };
    if (deviceId) {
      headers['Hub-Device-ID'] = deviceId;
    }
    return axiosAuth.get(`/vaults/${vaultId}/access-token?evenIfArchived=${evenIfArchived}`, { headers })
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

  /** @deprecated since version 1.3.0, to be removed in https://github.com/cryptomator/hub/issues/333 */
  public async listSomeLegacyDevices(deviceIds: string[]): Promise<DeviceDto[]> {
    const query = `ids=${deviceIds.join('&ids=')}`;
    return axiosAuth.get<DeviceDto[]>(`/devices/legacy-devices?${query}`).then(response => response.data);
  }

  public async removeDevice(deviceId: string): Promise<AxiosResponse<unknown>> {
    return axiosAuth.delete(`/devices/${deviceId}`)
      .catch((error) => rethrowAndConvertIfExpected(error, 404));
  }

  /** @deprecated since version 1.3.0, to be removed in https://github.com/cryptomator/hub/issues/333 */
  public async removeLegacyDevice(deviceId: string): Promise<AxiosResponse<unknown>> {
    return axiosAuth.delete(`/devices/${deviceId}/legacy-device`)
      .catch((error) => rethrowAndConvertIfExpected(error, 404));
  }

  public async putDevice(device: DeviceDto): Promise<AxiosResponse<unknown>> {
    return axiosAuth.put(`/devices/${device.id}`, device);
  }
}

class UserService {
  public async putMe(dto?: UserDto): Promise<void> {
    return axiosAuth.put('/users/me', dto);
  }

  public async me(withDevices: boolean = false, withLastAccess: boolean = false): Promise<UserDto> {
    return axiosAuth.get<UserDto>(`/users/me?withDevices=${withDevices}&withLastAccess=${withLastAccess}`).then(response => AuthorityService.fillInMissingPicture(response.data));
  }

  /** @deprecated since version 1.3.0, to be removed in https://github.com/cryptomator/hub/issues/333 */
  public async meWithLegacyDevicesAndAccess(): Promise<UserDto> {
    return axiosAuth.get<UserDto>('/users/me-with-legacy-devices-and-access').then(response => AuthorityService.fillInMissingPicture(response.data));
  }

  public async resetMe(): Promise<void> {
    return axiosAuth.post('/users/me/reset');
  }

  public async listAll(): Promise<UserDto[]> {
    return axiosAuth.get<UserDto[]>('/users/').then(response => response.data.map(AuthorityService.fillInMissingPicture));
  }
}

class TrustService {
  public async trustUser(userId: string, signature: string): Promise<void> {
    return axiosAuth.put(`/users/trusted/${userId}`, signature, { headers: { 'Content-Type': 'text/plain' } });
  }

  public async get(userId: string): Promise<TrustDto | undefined> {
    return axiosAuth.get<TrustDto>(`/users/trusted/${userId}`).then(response => response.data)
      .catch(e => {
        if (e.response.status === 404) return undefined;
        else throw e;
      });
  }

  public async listTrusted(): Promise<TrustDto[]> {
    return axiosAuth.get<TrustDto[]>('/users/trusted').then(response => response.data);
  }
}

class AuthorityService {
  public async search(query: string, withMemberSize: boolean = false): Promise<AuthorityDto[]> {
    return axiosAuth.get<AuthorityDto[]>(`/authorities/search?query=${query}&withMemberSize=${withMemberSize}`).then(response => response.data.map(AuthorityService.fillInMissingPicture));
  }

  public async listSome(authorityIds: string[]): Promise<AuthorityDto[]> {
    const query = `ids=${authorityIds.join('&ids=')}`;
    return axiosAuth.get<AuthorityDto[]>(`/authorities?${query}`).then(response => response.data.map(AuthorityService.fillInMissingPicture));
  }

  public static fillInMissingPicture<T extends AuthorityDto>(authority: T): T & { pictureUrl: string } {
    if (authority.pictureUrl) {
      return {
        ...authority,
        pictureUrl: authority.pictureUrl
      };
    } else {
      const cfg = AuthorityService.getJdenticonConfig(authority.type);
      const svg = toSvg(authority.id, 100, cfg);
      const bytes = UTF8.encode(svg);
      const url = `data:image/svg+xml;base64,${base64.stringify(bytes)}`;
      return {
        ...authority,
        pictureUrl: url
      };
    }
  }

  private static getJdenticonConfig(type: 'USER' | 'GROUP'): JdenticonConfig {
    switch (type) {
      case 'USER':
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
      case 'GROUP':
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

class LicenseService {
  public async getUserInfo(): Promise<LicenseUserInfoDto> {
    return axiosAuth.get('/license/user-info').then(response => {
      return new LicenseUserInfoDto(response.data.licensedSeats, response.data.usedSeats, response.data.expiresAt ? new Date(response.data.expiresAt) : null);
    });
  }
}

class VersionService {
  public async get(): Promise<VersionDto> {
    return axiosAuth.get<VersionDto>('/version').then(response => response.data);
  }
}

class SettingsService {
  public async get(): Promise<SettingsDto> {
    return axiosAuth.get<SettingsDto>('/settings').then(response => response.data);
  }

  public async put(settings: SettingsDto): Promise<void> {
    return axiosAuth.put('/settings', settings);
  }

  public async update(settings: Partial<SettingsDto>): Promise<void> {
    const originalSettings = await this.get();
    const updatedSettings = {
      ...originalSettings,
      ...settings
    };
    return axiosAuth.put('/settings', updatedSettings);
  }
}

/**
 * Note: Each service can thrown an {@link UnauthorizedError} when the access token is expired!
 */
const services = {
  vaults: new VaultService(),
  users: new UserService(),
  trust: new TrustService(),
  authorities: new AuthorityService(),
  devices: new DeviceService(),
  billing: new BillingService(),
  version: new VersionService(),
  license: new LicenseService(),
  settings: new SettingsService()
};

export default services;

// #endregion Services
// #region Error handling

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
export function rethrowAndConvertIfExpected(error: unknown, ...expectedStatusCodes: number[]): never {
  if (AxiosStatic.isAxiosError(error) && error.response != null && expectedStatusCodes.includes(error.response.status)) {
    throw convertExpectedToBackendError(error.response.status);
  } else {
    throw error;
  }
}

export class BackendError extends Error { }

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

// #endregion Error handling
