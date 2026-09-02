import { DriveStep, driver } from 'driver.js';
import auth from './auth';
import i18n from '../i18n';
import 'driver.js/dist/driver.css';

type TourStep = {
  target?: string;
  titleKey: string;
  descriptionKey: string;
};

const vaultListTarget = '[data-tour="vaultList"]';

// adminNav needs no role check: its target only exists for admins and invisible steps are skipped
// addVault must be gated explicitly: the button is always rendered, merely disabled
export function tourSteps(canCreateVaults: boolean): TourStep[] {
  return [
    { titleKey: 'onboarding.welcome.title', descriptionKey: 'onboarding.welcome.description' },
    { target: vaultListTarget, titleKey: 'onboarding.vaultList.title', descriptionKey: canCreateVaults ? 'onboarding.vaultList.description' : 'onboarding.vaultList.description.shared' },
    { target: '[data-tour="appCard"]', titleKey: 'onboarding.appCard.title', descriptionKey: 'onboarding.appCard.description' },
    { target: '[data-tour="adminNav"]', titleKey: 'onboarding.adminNav.title', descriptionKey: 'onboarding.adminNav.description' },
    { target: '[data-tour="profile"]', titleKey: 'onboarding.profile.title', descriptionKey: 'onboarding.profile.description' },
    ...(canCreateVaults ? [{ target: '[data-tour="addVault"]', titleKey: 'onboarding.addVault.title', descriptionKey: 'onboarding.addVault.description' }] : [])
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
  const { t } = i18n.global;
  await waitForVisibleElement(vaultListTarget);

  const includedSteps = tourSteps((await auth).hasRole('create-vaults'));

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
        description: t(step.descriptionKey)
      }
    });
  }

  const qrBlock = isMobile() ? '' : `
        <p class="onboarding-app-qr-label">${t('onboarding.getApp.qrCode')}</p>
        <img src="/download-qr.svg" alt="https://cryptomator.org/downloads/" class="onboarding-app-qr" />`;
  steps.push({
    popover: {
      title: t('onboarding.getApp.title'),
      description: `<div class="onboarding-app-step">
        <img src="/cryptomator.svg" alt="Cryptomator" class="onboarding-app-logo" />
        <p>${t('onboarding.getApp.description')}</p>
        <a href="${appDownloadUrl()}" target="_blank" rel="noopener noreferrer" class="onboarding-app-download">${t('onboarding.getApp.download')}</a>${qrBlock}
        <p class="onboarding-app-hint">${t('onboarding.getApp.alreadyInstalled')}</p>
      </div>`
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
    onDestroyed: () => setOnboardingCompleted(userId),
    steps: steps
  }).drive();
}
