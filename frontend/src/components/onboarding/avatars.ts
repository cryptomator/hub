export interface AvatarPreset {
  background: string
  shirt: string
  skin: string
  hair: string
  bun: boolean
}

// simplified team avatars from cryptomator.org
export const teamAvatars: AvatarPreset[] = [
  { background: '#e8eef4', shirt: '#49b04a', skin: '#e0ac69', hair: '#1e2b33', bun: false },
  { background: '#eef4f0', shirt: '#008a7b', skin: '#eab68a', hair: '#233042', bun: true },
  { background: '#f1f3f5', shirt: '#354960', skin: '#c68642', hair: '#14202b', bun: false }
];
