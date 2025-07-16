export type EmergencyKeyShareData =
  | {
      typ: 'ownership';
      newowner: string;
      timestamp: string;
      approved?: boolean;
    }
  | {
      typ: 'voteNewCouncilMembers';
      newCouncilMembers: string[];
      timestamp: string;
      comment?: string;
    };

export class EmergencyKeyShare {
  static create(data: EmergencyKeyShareData): string {
    return JSON.stringify(data);
  }

  static parse(json: string): EmergencyKeyShareData | null {
    try {
      const parsed = JSON.parse(json);
      if (parsed?.typ && typeof parsed.typ === 'string') {
        return parsed;
      }
      return null;
    } catch {
      return null;
    }
  }

  static toDisplayString(data: EmergencyKeyShareData): string {
    const date = new Date(data.timestamp);
    const timestamp = date.getUTCFullYear() + '-' + date.getUTCMonth() + '-' + date.getUTCDate();
    switch (data.typ) {
      case 'ownership':
        return `ownership: ${data.newowner}`;
      case 'voteNewCouncilMembers':
        return `voteNewCouncilMembers: ${data.newCouncilMembers.join(', ')}`;
      default:
        return 'Unknown' + '';
    }
  }
}
