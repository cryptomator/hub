import { base64 } from '@scure/base';
import AxiosStatic, { AxiosError, AxiosHeaders, AxiosRequestConfig, AxiosResponse } from 'axios';
import { JdenticonConfig, toSvg } from 'jdenticon';
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

export function isAxiosError(error: unknown): error is AxiosError {
  return AxiosStatic.isAxiosError(error);
}

// #region DTOs

export type VaultDto = {
  id: string;
  name: string;
  creationTime: Date;
  description?: string;
  archived: boolean;
  requiredEmergencyKeyShares: number;
  emergencyKeyShares: Record<string, string>; // <memberId, encryptedKeyShare>
  
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

export type RealmRole = 'user' | 'admin' | 'create-vaults';
export type SelectableRealmRole = Exclude<RealmRole, 'user'>;
export function isSelectableRealmRole(role: RealmRole): role is SelectableRealmRole {
  return role !== 'user';
}

export type AccessGrant = {
  userId: string,
  token: string
};

export type UserDto = {
  type: 'USER';
  id: string;
  name: string;
  pictureUrl?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  realmRoles: RealmRole[];
  enabled: boolean;
  language?: string;
  devices: DeviceDto[];
  accessibleVaults: VaultDtoWithRole[];
  ecdhPublicKey?: string;
  ecdsaPublicKey?: string;
  privateKeys?: string;
  setupCode?: string;
}

export type UserDtoWithCounts = UserDto & {
  groupsCount?: number;
  devicesCount?: number;
  accessibleVaultCount?: number;
}

export type UserDtoWithDetails = UserDto & {
  groups: GroupDto[];
  devices: DeviceDto[];
  legacyDevices: DeviceDto[];
}

/**
 * Represents a user who generated key pairs during the setup process.
 */
export type ActivatedUser = UserDto & {
  ecdhPublicKey: string;
  ecdsaPublicKey: string;
}

export function didCompleteSetup(user: UserDto): user is ActivatedUser {
  return user.ecdhPublicKey !== undefined && user.ecdsaPublicKey !== undefined;
}

export type GroupDto = {
  type: 'GROUP';
  id: string;
  name: string;
  pictureUrl?: string;
  memberSize?: number;
  vaultCount?: number;
}

export type AuthorityDto = UserDto | GroupDto;

export type MemberDto = AuthorityDto & {
  vaultRole: VaultRole
}

export type TrustDto = {
  trustedUserId: string,
  signatureChain: string[]
}

export type CreateUserDto = Pick<UserDto, 'name' | 'email' | 'firstName' | 'lastName' | 'pictureUrl' | 'realmRoles'> & {
  password: string;
};

export type UpdateUserDto = Pick<UserDto, 'email' | 'firstName' | 'lastName' | 'pictureUrl' | 'realmRoles'> & {
  password?: string;
};

export type CreateGroupDto = Pick<GroupDto, 'name' | 'pictureUrl'>;

export type UpdateGroupDto = CreateGroupDto;

export type VaultDtoWithRole = VaultDto & {
  role: VaultRole;
}

export type GroupDtoWithDetails = GroupDto & {
  members: AuthorityDto[];
  vaults: VaultDtoWithRole[];
}

export type BillingDto = {
  hubId: string;
  email: string;
  licensedSeats: number;
  usedSeats: number;
  issuedAt: Date;
  expiresAt: Date;
  managedInstance: boolean;
  licenseKey: string;
}

export type VersionDto = {
  hubVersion: string;
  keycloakVersion?: string;
}

export type SettingsDto = {
  hubId: string,
  wotMaxDepth: number,
  wotIdVerifyLen: number,
  defaultRequiredEmergencyKeyShares: number,
  defaultMinMembers: number,
  allowChoosingEmergencyCouncil: boolean,
  emergencyCouncilMemberIds: string[],
  enableEmergencyAccess: boolean
}

export type RecoveryProcessSetNewOwner = {
  type: 'CHANGE_PERMISSIONS',
  details: {
    newOwnerIds: string[];
    newMemberIds: string[];
  }
}

export type RecoveryProcessChangeCouncil = {
  type: 'COUNCIL_CHANGE',
  details: {
    newCouncilMemberIds: string[];
    newRequiredKeyShares: number;
  }
}

export type RecoveredKeyShareDto = {
  processPrivateKey: string;
  unrecoveredKeyShare: string;
  recoveredKeyShare?: string;
  signedProcessInfo?: string;
};

export type RecoveryProcessDto = (RecoveryProcessSetNewOwner | RecoveryProcessChangeCouncil) & {
  id: string;
  vaultId: string;
  requiredKeyShares: number;
  processPublicKey: string;
  recoveredKeyShares: {
    [councilMemberId: string]: RecoveredKeyShareDto
  }
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
    return this.licensedSeats == 0 || this.usedSeats > this.licensedSeats;
  }
}

export interface VaultIdHeader extends JWTHeader {
  vaultId: string;
}

function fillInMissingPicture<T extends AuthorityDto>(authority: T): T & { pictureUrl: string } {
  if (authority.pictureUrl) {
    return {
      ...authority,
      pictureUrl: authority.pictureUrl
    };
  } else {
    return {
      ...authority,
      pictureUrl: generateFallbackPictureUrl(authority.type, authority.id)
    };
  }
}

export function generateFallbackPictureUrl(type: 'USER' | 'GROUP', authorityId: string): string {
  const cfg = getJdenticonConfig(type);
  const svg = toSvg(authorityId, 100, cfg);
  const bytes = UTF8.encode(svg);
  return `data:image/svg+xml;base64,${base64.encode(bytes)}`;
}

function getJdenticonConfig(type: 'USER' | 'GROUP'): JdenticonConfig {
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
        hues: [6, 28, 48, 121, 283],
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

// #endregion DTOs
// #region Services

class VaultService {
  public async listAccessible(role?: 'MEMBER' | 'OWNER'): Promise<VaultDto[]> {
    const queryParams = role ? { role: role } : {};
    return axiosAuth.get('/vaults/accessible', { params: queryParams }).then(response => response.data);
  }

  public async listRecoverable(): Promise<VaultDto[]> {
    return axiosAuth.get('/vaults/recoverable').then(response => response.data);
  }

  public async listSome(vaultIds: string[]): Promise<VaultDto[]> {
    return axiosAuth.get('/vaults/some', {
      params: {
        ids: vaultIds
      },
      paramsSerializer: {
        indexes: null, // disable array indices in query params (e.g. ids[0]=...&ids[1]=...)
      }
    }).then(response => response.data);
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

  public async getMembers(vaultId: string, addFallbackPictures: boolean = true): Promise<MemberDto[]> {
    const members = await axiosAuth.get<MemberDto[]>(`/vaults/${vaultId}/members`).then(response => response.data).catch(err => rethrowAndConvertIfExpected(err, 403));
    return addFallbackPictures ? members.map(fillInMissingPicture) : members;
  }

  public async setMembersWithRole(vaultId: string, members: Record<string, VaultRole>): Promise<void> {
    await axiosAuth.put(`/vaults/${vaultId}/members`, members)
      .catch((error) => rethrowAndConvertIfExpected(error, 403, 404));
  }

  public async addUser(vaultId: string, userId: string, role?: VaultRole): Promise<AxiosResponse<void>> {
    const queryParams = role ? { role: role } : {};
    return axiosAuth.put(`/vaults/${vaultId}/users/${userId}`, null, { params: queryParams })
      .catch((error) => rethrowAndConvertIfExpected(error, 402, 404, 409));
  }

  public async addGroup(vaultId: string, groupId: string, role?: VaultRole): Promise<AxiosResponse<void>> {
    const queryParams = role ? { role: role } : {};
    return axiosAuth.put(`/vaults/${vaultId}/groups/${groupId}`, null, { params: queryParams })
      .catch((error) => rethrowAndConvertIfExpected(error, 402, 404, 409));
  }

  public async getUsersRequiringAccessGrant(vaultId: string, addFallbackPictures: boolean = true): Promise<UserDto[]> {
    const users = await axiosAuth.get<UserDto[]>(`/vaults/${vaultId}/users-requiring-access-grant`).then(response => response.data).catch(err => rethrowAndConvertIfExpected(err, 403));
    return addFallbackPictures ? users.map(fillInMissingPicture) : users;
  }

  public async setArchived(vaultId: string, archived: boolean): Promise<VaultDto> {
    return axiosAuth.put<VaultDto>(`/vaults/${vaultId}/archived`, String(archived), { headers: { 'Content-Type': 'text/plain' } })
      .then(response => {
        response.data.creationTime = new Date(response.data.creationTime);
        return response.data;
      })
      .catch((error) => rethrowAndConvertIfExpected(error, 403, 404));
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
    return axiosAuth.get<DeviceDto[]>('/devices/legacy-devices', {
      params: {
        ids: deviceIds
      },
      paramsSerializer: {
        indexes: null, // disable array indices in query params (e.g. ids[0]=...&ids[1]=...)
      }
    }).then(response => response.data);
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

class GroupService {
  public async listAll(addFallbackPictures: boolean = true): Promise<GroupDto[]> {
    const groups = await axiosAuth.get<GroupDto[]>('/groups/').then(response => response.data);
    return addFallbackPictures ? groups.map(fillInMissingPicture) : groups;
  }

  public async getGroup(groupId: string, addFallbackPictures: boolean = true): Promise<GroupDtoWithDetails> {
    const group = await axiosAuth.get<GroupDtoWithDetails>(`/groups/${groupId}`).then(response => response.data).catch((error) => rethrowAndConvertIfExpected(error, 404));
    if (addFallbackPictures) {
      return {
        ...fillInMissingPicture(group),
        members: group.members.map(m => fillInMissingPicture(m))
      };
    } else {
      return group;
    }
  }

  public async createGroup(dto: CreateGroupDto, addFallbackPictures: boolean = true): Promise<GroupDto> {
    const group = await axiosAuth.post<GroupDto>('/groups/', dto).then(response => response.data);
    return addFallbackPictures ? fillInMissingPicture(group) : group;
  }

  public async updateGroup(groupId: string, dto: UpdateGroupDto, addFallbackPictures: boolean = true): Promise<GroupDto> {
    const group = await axiosAuth.put<GroupDto>(`/groups/${groupId}`, dto).then(response => response.data).catch((error) => rethrowAndConvertIfExpected(error, 404));
    return addFallbackPictures ? fillInMissingPicture(group) : group;
  }

  public async removeGroup(groupId: string): Promise<void> {
    return axiosAuth.delete(`/groups/${groupId}`)
      .then(() => { })
      .catch((error) => rethrowAndConvertIfExpected(error, 404));
  }

  public async getEffectiveMembers(groupId: string, addFallbackPictures: boolean = true): Promise<UserDto[]> {
    const members = await axiosAuth.get<UserDto[]>(`/groups/${groupId}/effective-members`).then(response => response.data).catch((error) => rethrowAndConvertIfExpected(error, 404));
    return addFallbackPictures ? members.map(fillInMissingPicture) : members;
  }

  public async addMember(groupId: string, userId: string): Promise<void> {
    await axiosAuth.post(`/groups/${groupId}/members/${userId}`).catch((error) => rethrowAndConvertIfExpected(error, 404));
  }

  public async removeMember(groupId: string, userId: string): Promise<void> {
    await axiosAuth.delete(`/groups/${groupId}/members/${userId}`).catch((error) => rethrowAndConvertIfExpected(error, 404));
  }
}

class UserService {
  public async putMe(dto?: UserDto): Promise<void> {
    return axiosAuth.put('/users/me', dto);
  }

  public async me(withDevices: boolean = false, withLastAccess: boolean = false, addFallbackPictures: boolean = true): Promise<UserDto> {
    const user = await axiosAuth.get<UserDto>('/users/me', {
      params: {
        withDevices: withDevices,
        withLastAccess: withLastAccess
      }
    }).then(response => response.data);
    return addFallbackPictures ? fillInMissingPicture(user) : user;
  }

  /** @deprecated since version 1.3.0, to be removed in https://github.com/cryptomator/hub/issues/333 */
  public async meWithLegacyDevicesAndAccess(): Promise<UserDto> {
    return axiosAuth.get<UserDto>('/users/me-with-legacy-devices-and-access').then(response => fillInMissingPicture(response.data));
  }

  public async removeUser(userId: string): Promise<void> {
    return axiosAuth.delete(`/users/${userId}`)
      .then(() => { })
      .catch((error) => rethrowAndConvertIfExpected(error, 404));
  }

  public async resetMe(): Promise<void> {
    return axiosAuth.post('/users/me/reset');
  }

  public async listAll(addFallbackPictures: boolean = true): Promise<UserDtoWithCounts[]> {
    const users = await axiosAuth.get<UserDtoWithCounts[]>('/users/').then(response => response.data);
    return addFallbackPictures ? users.map(fillInMissingPicture) : users;
  }

  public async createUser(dto: CreateUserDto, addFallbackPictures: boolean = true): Promise<UserDto> {
    const user = await axiosAuth.post<UserDto>('/users/', dto).then(response => response.data);
    return addFallbackPictures ? fillInMissingPicture(user) : user;
  }

  public async getUser(userId: string, addFallbackPictures: boolean = true): Promise<UserDtoWithDetails> {
    const user = await axiosAuth.get<UserDtoWithDetails>(`/users/${userId}`).then(response => response.data).catch((error) => rethrowAndConvertIfExpected(error, 404));
    if (addFallbackPictures) {
      return {
        ...fillInMissingPicture(user),
        groups: user.groups.map(g => fillInMissingPicture(g))
      };
    } else {
      return user;
    }
  }

  public async setUserEnabled(userId: string, enabled: boolean): Promise<void> {
    await axiosAuth.put(`/users/${userId}/enabled`, enabled, { headers: { 'Content-Type': 'text/plain' } });
  }

  public async updateUser(userId: string, dto: UpdateUserDto, addFallbackPictures: boolean = true): Promise<UserDto> {
    const user = await axiosAuth.put<UserDto>(`/users/${userId}`, dto).then(response => response.data).catch((error) => rethrowAndConvertIfExpected(error, 404));
    return addFallbackPictures ? fillInMissingPicture(user) : user;
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
  public async search(query: string, withMemberSize: boolean = false, addFallbackPictures: boolean = true): Promise<AuthorityDto[]> {
    const authorities = await axiosAuth.get<AuthorityDto[]>('/authorities/search', {
      params: {
        query: query,
        withMemberSize: withMemberSize
      }
    }).then(response => response.data);
    return addFallbackPictures ? authorities.map(fillInMissingPicture) : authorities;
  }

  public async listSome(authorityIds: string[], addFallbackPictures: boolean = true): Promise<AuthorityDto[]> {
    if (authorityIds.length === 0) {
      // safe roundtrip for empty list
      return [];
    }
    const authorities = await axiosAuth.get<AuthorityDto[]>('/authorities', {
      params: {
        ids: authorityIds
      },
      paramsSerializer: {
        indexes: null, // disable array indices in query params (e.g. ids[0]=...&ids[1]=...)
      }
    }).then(response => response.data);
    return addFallbackPictures ? authorities.map(fillInMissingPicture) : authorities;
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

  public async refresh(): Promise<void> {
    return axiosAuth.post('/license/refresh');
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

class EmergencyAccessService {
  public async findProcessesForVault(vaultId: string): Promise<RecoveryProcessDto[]> {
    return axiosAuth.get<RecoveryProcessDto[]>(`/emergency-access/${vaultId}`).then(response => response.data);
  }

  public async startRecovery(recoveryProcess: RecoveryProcessDto): Promise<void> {
    return axiosAuth.put(`/emergency-access/${recoveryProcess.id}`, recoveryProcess);
  }

  public async addMyShare(recoveryProcessId: string, recoveredKeyShare: RecoveredKeyShareDto): Promise<void> {
    return axiosAuth.post(`/emergency-access/${recoveryProcessId}/recovered-key-shares`, recoveredKeyShare);
  }

  public async complete(recoveryProcessId: string): Promise<void> {
    return axiosAuth.delete(`/emergency-access/${recoveryProcessId}/complete`);
  }

  public async abort(recoveryProcessId: string): Promise<void> {
    return axiosAuth.delete(`/emergency-access/${recoveryProcessId}/abort`);
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
  settings: new SettingsService(),
  groups: new GroupService(),
  emergencyAccess: new EmergencyAccessService(),
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
