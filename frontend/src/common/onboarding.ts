import { DriveStep, driver } from 'driver.js';
import auth from './auth';
import i18n from '../i18n';
import 'driver.js/dist/driver.css';

type TourStep = {
  target?: string;
  titleKey: string;
  descriptionKey: string;
  vignette?: () => string;
};

const vaultListTarget = '[data-tour="vaultList"]';
const adminNavTarget = '[data-tour="adminNav"]';

export const previewVariants = ['admin', 'create-vaults', 'user'] as const;
export type PreviewVariant = typeof previewVariants[number];

// outline paths copied from @heroicons/vue: driver.js popovers take HTML strings, not components
const iconPaths = {
  lock: 'M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z',
  key: 'M15.75 5.25a3 3 0 0 1 3 3m3 0a6 6 0 0 1-7.029 5.912c-.563-.097-1.159.026-1.563.43L10.5 17.25H8.25v2.25H6v2.25H2.25v-2.818c0-.597.237-1.17.659-1.591l6.499-6.499c.404-.404.527-1 .43-1.563A6 6 0 1 1 21.75 8.25Z',
  chevronRight: 'm8.25 4.5 7.5 7.5-7.5 7.5',
  copy: 'M15.75 17.25v3.375c0 .621-.504 1.125-1.125 1.125h-9.75a1.125 1.125 0 0 1-1.125-1.125V7.875c0-.621.504-1.125 1.125-1.125H6.75a9.06 9.06 0 0 1 1.5.124m7.5 10.376h3.375c.621 0 1.125-.504 1.125-1.125V11.25c0-4.46-3.243-8.161-7.5-8.876a9.06 9.06 0 0 0-1.5-.124H9.375c-.621 0-1.125.504-1.125 1.125v3.5m7.5 10.375H9.375a1.125 1.125 0 0 1-1.125-1.125v-9.25m12 6.625v-1.875a3.375 3.375 0 0 0-3.375-3.375h-1.5a1.125 1.125 0 0 1-1.125-1.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H9.75',
  device: 'M9 17.25v1.007a3 3 0 0 1-.879 2.122L7.5 21h9l-.621-.621A3 3 0 0 1 15 18.257V17.25m6-12V15a2.25 2.25 0 0 1-2.25 2.25H5.25A2.25 2.25 0 0 1 3 15V5.25m18 0A2.25 2.25 0 0 0 18.75 3H5.25A2.25 2.25 0 0 0 3 5.25m18 0V12a2.25 2.25 0 0 1-2.25 2.25H5.25A2.25 2.25 0 0 1 3 12V5.25',
  phone: 'M10.5 1.5H8.25A2.25 2.25 0 0 0 6 3.75v16.5a2.25 2.25 0 0 0 2.25 2.25h7.5A2.25 2.25 0 0 0 18 20.25V3.75a2.25 2.25 0 0 0-2.25-2.25H13.5m-3 0V3h3V1.5m-3 0h3m-3 18.75h3',
  download: 'M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3',
  folderOpen: 'M3.75 9.776c.112-.017.227-.026.344-.026h15.812c.117 0 .232.009.344.026m-16.5 0a2.25 2.25 0 0 0-1.883 2.542l.857 6a2.25 2.25 0 0 0 2.227 1.932H19.05a2.25 2.25 0 0 0 2.227-1.932l.857-6a2.25 2.25 0 0 0-1.883-2.542m-16.5 0V6A2.25 2.25 0 0 1 6 3.75h3.879a1.5 1.5 0 0 1 1.06.44l2.122 2.12a1.5 1.5 0 0 0 1.06.44H18A2.25 2.25 0 0 1 20.25 9v.776'
};

// simplified team avatars from cryptomator.org
const teamAvatars = [
  { background: '#e8eef4', shirt: '#49b04a', skin: '#e0ac69', hair: '#1e2b33', bun: false },
  { background: '#eef4f0', shirt: '#008a7b', skin: '#eab68a', hair: '#233042', bun: true },
  { background: '#f1f3f5', shirt: '#354960', skin: '#c68642', hair: '#14202b', bun: false }
];

type AvatarPreset = typeof teamAvatars[number];
type BarWidth = 40 | 48 | 56 | 80;

function icon(path: string, size: number, colorClass: string): string {
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" aria-hidden="true" class="onboarding-vignette-icon ${colorClass}"><path stroke-linecap="round" stroke-linejoin="round" d="${path}"/></svg>`;
}

// each width needs a matching class because the CSP forbids inline style attributes
function bar(width: BarWidth, strong = false): string {
  return `<span class="onboarding-vignette-bar onboarding-vignette-bar-w${width}${strong ? ' onboarding-vignette-bar-strong' : ''}"></span>`;
}

// pass decorative=false only when a vignette carries information its step description does not
function vignette(content: string, decorative = true): string {
  return `<div class="onboarding-vignette"${decorative ? ' aria-hidden="true"' : ''}>${content}</div>`;
}

// the body path overruns the viewBox on purpose; .onboarding-vignette-avatar crops it to a circle
function avatar(preset: AvatarPreset, size = 20): string {
  return `<svg width="${size}" height="${size}" viewBox="0 0 64 64" aria-hidden="true" class="onboarding-vignette-avatar">`
    + `<rect width="64" height="64" fill="${preset.background}"/>`
    + `<path d="M8 66 Q9 41 32 41 Q55 41 55 66 Z" fill="${preset.shirt}"/>`
    + `<rect x="28.5" y="32" width="7" height="10" rx="2.5" fill="${preset.skin}"/>`
    + `<circle cx="32" cy="27.5" r="9.5" fill="${preset.skin}"/>`
    + `<path d="M21.2 27 Q21.2 15.8 32 15.8 Q42.8 15.8 42.8 27 Q42.8 21.5 32 21.5 Q21.2 21.5 21.2 27 Z" fill="${preset.hair}"/>`
    + `${preset.bun ? `<circle cx="32" cy="13.5" r="4.5" fill="${preset.hair}"/>` : ''}</svg>`;
}

// the key travels between the nodes via the onboarding-key-travel keyframes in index.css
function welcomeVignette(): string {
  const { t } = i18n.global;
  return vignette(`<div class="onboarding-hub-story">
    <div class="onboarding-hub-node">
      <span class="onboarding-vignette-avatars">${teamAvatars.map(preset => avatar(preset, 28)).join('')}</span>
      <span class="onboarding-vignette-caption">${t('onboarding.welcome.team')}</span>
    </div>
    <div class="onboarding-hub-link">
      <span class="onboarding-hub-link-line"></span>
      <span class="onboarding-hub-link-key">${icon(iconPaths.key, 15, 'onboarding-vignette-icon-primary')}</span>
    </div>
    <div class="onboarding-hub-node">
      <img src="/logo.svg" width="48" height="47" alt="" />
      <span class="onboarding-vignette-caption">Hub</span>
    </div>
  </div>`);
}

function vaultListVignette(): string {
  const { t } = i18n.global;
  const vaultRow = (name: string, avatars: string, descWidth: BarWidth) =>
    `<div class="onboarding-mini-list-row">
      <span class="onboarding-vignette-chip">${icon(iconPaths.lock, 16, 'onboarding-vignette-icon-primary')}</span>
      <span class="onboarding-mini-list-body">
        <span class="onboarding-mini-list-title">${name}</span>
        ${bar(descWidth)}
      </span>
      <span class="onboarding-vignette-avatars">${avatars}</span>
      ${icon(iconPaths.chevronRight, 11, 'onboarding-vignette-icon-gray')}
    </div>`;
  return vignette(`<div class="onboarding-mini-list">
    ${vaultRow(t('onboarding.vaultList.example1'), avatar(teamAvatars[0]) + avatar(teamAvatars[1]), 80)}
    ${vaultRow(t('onboarding.vaultList.example2'), avatar(teamAvatars[2]), 56)}
  </div>`);
}

function profileVignette(): string {
  return vignette(`<div class="onboarding-vignette-stack">
    <div class="onboarding-vignette-row">
      ${icon(iconPaths.key, 16, 'onboarding-vignette-icon-primary')}
      <span class="onboarding-vignette-mono">a1b2‑••••‑••••‑e5f6</span>
      ${icon(iconPaths.copy, 15, 'onboarding-vignette-icon-gray')}
    </div>
    <div class="onboarding-vignette-pair">
      <div class="onboarding-vignette-row">${icon(iconPaths.device, 15, 'onboarding-vignette-icon-gray')}<span class="onboarding-vignette-bars">${bar(48, true)}</span><span class="onboarding-vignette-dot"></span></div>
      <div class="onboarding-vignette-row">${icon(iconPaths.phone, 15, 'onboarding-vignette-icon-gray')}<span class="onboarding-vignette-bars">${bar(40, true)}</span><span class="onboarding-vignette-dot"></span></div>
    </div>
  </div>`);
}

function getAppDescription(): string {
  const { t } = i18n.global;
  const appStep = (path: string, labelKey: string) =>
    `<li class="onboarding-vignette-row"><span class="onboarding-vignette-chip">${icon(path, 16, 'onboarding-vignette-icon-primary')}</span><span class="onboarding-vignette-row-label">${t(labelKey)}</span></li>`;
  const qrRow = isMobile() ? '' : `
        <div class="onboarding-app-qr-row">
          <img src="/download-qr.svg" alt="https://cryptomator.org/downloads/" class="onboarding-app-qr" />
          <span class="onboarding-app-qr-caption">${t('onboarding.getApp.qrCode')}</span>
        </div>`;
  return `${vignette(`<ol class="onboarding-vignette-stack">
        ${appStep(iconPaths.download, 'onboarding.getApp.step1')}
        ${appStep(iconPaths.folderOpen, 'onboarding.getApp.step2')}
        ${appStep(iconPaths.key, 'onboarding.getApp.step3')}
      </ol>`, false)}
      <p>${t('onboarding.getApp.description')}</p>
      <a href="${appDownloadUrl()}" target="_blank" rel="noopener noreferrer" class="onboarding-app-download">${t('onboarding.getApp.download')}</a>${qrRow}`;
}

// addVault must be gated explicitly because the button is always rendered, merely disabled;
// the admin step needs no role check: its target only exists for admins and invisible steps are skipped
export function tourSteps(canCreateVaults: boolean, includeAdminNav = true): TourStep[] {
  return [
    { titleKey: 'onboarding.welcome.title', descriptionKey: 'onboarding.welcome.description', vignette: welcomeVignette },
    { target: vaultListTarget, titleKey: 'onboarding.vaultList.title', descriptionKey: canCreateVaults ? 'onboarding.vaultList.description' : 'onboarding.vaultList.description.shared', vignette: vaultListVignette },
    canCreateVaults
      ? { target: '[data-tour="addVault"]', titleKey: 'onboarding.addVault.title', descriptionKey: 'onboarding.addVault.description' }
      : { target: vaultListTarget, titleKey: 'onboarding.useVault.title', descriptionKey: 'onboarding.useVault.description' },
    ...(includeAdminNav ? [{ target: adminNavTarget, titleKey: 'onboarding.adminNav.title', descriptionKey: 'onboarding.adminNav.description' }] : []),
    { target: '[data-tour="profile"]', titleKey: 'onboarding.profile.title', descriptionKey: 'onboarding.profile.description', vignette: profileVignette }
  ];
}

function storageKey(userId: string): string {
  return `hub.onboardingCompleted.${userId}`;
}

function isOnboardingCompleted(userId: string): boolean {
  try {
    return localStorage.getItem(storageKey(userId)) != null;
  } catch {
    return true;
  }
}

function setOnboardingCompleted(userId: string) {
  try {
    localStorage.setItem(storageKey(userId), new Date().toISOString());
  } catch {
    // if localStorage is unavailable, the tour may show again next time
  }
}

function isMobile(): boolean {
  return /android|iphone|ipad|ipod/i.test(navigator.userAgent);
}

export function appDownloadUrl(): string {
  const ua = navigator.userAgent;
  if (/android/i.test(ua)) {
    return 'https://play.google.com/store/apps/details?id=org.cryptomator.freemium';
  } else if (/iphone|ipad|ipod/i.test(ua)) {
    return 'https://apps.apple.com/app/cryptomator/id1560822163';
  } else if (/windows/i.test(ua)) {
    return 'https://cryptomator.org/downloads/win/thanks/';
  } else if (/mac/i.test(ua)) {
    return macDownloadUrl();
  } else if (/linux/i.test(ua)) {
    return 'https://cryptomator.org/downloads/linux/thanks/';
  }
  return 'https://cryptomator.org/downloads/';
}

// same WebGL renderer sniffing as cryptomator.org uses to tell Apple Silicon and Intel Macs apart
function macDownloadUrl(): string {
  try {
    const gl = document.createElement('canvas').getContext('webgl');
    const info = gl?.getExtension('WEBGL_debug_renderer_info');
    const renderer = info ? String(gl?.getParameter(info.UNMASKED_RENDERER_WEBGL)).toLowerCase() : '';
    if (renderer.includes('apple')) {
      return 'https://cryptomator.org/downloads/mac-arm64/thanks/';
    } else if (/intel|amd|radeon/.test(renderer)) {
      return 'https://cryptomator.org/downloads/mac/thanks/';
    }
  } catch {
    // detection is best-effort only
  }
  return 'https://cryptomator.org/downloads/#mac';
}

function appHintKey(userId: string): string {
  return `hub.appHintDismissed.${userId}`;
}

export function isAppHintDismissed(userId: string): boolean {
  try {
    return localStorage.getItem(appHintKey(userId)) != null;
  } catch {
    return true;
  }
}

export function dismissAppHint(userId: string) {
  try {
    localStorage.setItem(appHintKey(userId), new Date().toISOString());
  } catch {
    // if localStorage is unavailable, the hint may show again next time
  }
}

function findVisibleElement(selector: string): Element | undefined {
  return Array.from(document.querySelectorAll<HTMLElement>(selector)).find(element => element.offsetParent != null);
}

async function waitForVisibleElement(selector: string) {
  for (let attempt = 0; attempt < 80 && !findVisibleElement(selector); attempt++) {
    await new Promise(resolve => setTimeout(resolve, 50));
  }
}

/**
 * Starts the onboarding tour unless the user has already completed or dismissed it.
 * @param userId the id of the currently logged in user
 */
export async function startOnboardingIfNeeded(userId: string) {
  if (!isOnboardingCompleted(userId)) {
    await startOnboarding(userId);
  }
}

/**
 * Starts the onboarding tour on the vault list and marks it as completed once it is finished or dismissed.
 * Steps whose target element is not visible (e.g. the sidebar on mobile) are skipped.
 * @param userId the id of the currently logged in user
 */
export async function startOnboarding(userId: string) {
  await driveTour(tourSteps((await auth).hasRole('create-vaults')), () => setOnboardingCompleted(userId));
}

/**
 * Dev-only preview of a role variant that does not mark the tour as completed. Steps whose target the
 * current user cannot see are still skipped, so previewing the admin variant requires an admin session.
 */
export async function previewOnboarding(variant: PreviewVariant) {
  await driveTour(tourSteps(variant !== 'user', variant === 'admin'));
}

async function driveTour(includedSteps: TourStep[], onTourDestroyed?: () => void) {
  const { t } = i18n.global;
  await waitForVisibleElement(vaultListTarget);

  const steps: DriveStep[] = [];
  for (const step of includedSteps) {
    const element = step.target ? findVisibleElement(step.target) : undefined;
    if (step.target && !element) {
      continue;
    }
    steps.push({
      element: element,
      popover: {
        title: t(step.titleKey),
        description: `${step.vignette ? step.vignette() : ''}<p>${t(step.descriptionKey)}</p>`
      }
    });
  }
  steps.push({
    popover: {
      title: t('onboarding.getApp.title'),
      description: getAppDescription()
    }
  });

  driver({
    popoverClass: 'onboarding-popover',
    overlayColor: '#6b7280',
    overlayOpacity: 0.75,
    stagePadding: 8,
    stageRadius: 8,
    showProgress: true,
    progressText: t('onboarding.progress', ['{{current}}', '{{total}}']),
    nextBtnText: t('common.next'),
    prevBtnText: t('common.previous'),
    doneBtnText: t('onboarding.done'),
    onDestroyed: onTourDestroyed,
    steps: steps
  }).drive();
}
