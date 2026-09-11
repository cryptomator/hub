import { DriveStep, driver } from 'driver.js';
import { createApp } from 'vue';
import type { App, Component } from 'vue';
import GetAppStep from '../components/onboarding/GetAppStep.vue';
import ProfileVignette from '../components/onboarding/ProfileVignette.vue';
import TourPopoverContent, { TourPopoverProps } from '../components/onboarding/TourPopoverContent.vue';
import VaultListVignette from '../components/onboarding/VaultListVignette.vue';
import WelcomeVignette from '../components/onboarding/WelcomeVignette.vue';
import i18n from '../i18n';
import auth from './auth';
import 'driver.js/dist/driver.css';

type TourStep = {
  target?: string;
  titleKey: string;
  descriptionKey: string;
  vignette?: Component;
};

const vaultListTarget = '[data-tour="vaultList"]';
const adminNavTarget = '[data-tour="adminNav"]';

export const previewVariants = ['admin', 'create-vaults', 'user'] as const;
export type PreviewVariant = typeof previewVariants[number];

// addVault must be gated explicitly because the button is always rendered, merely disabled;
// the admin step needs no role check: its target only exists for admins and invisible steps are skipped
export function tourSteps(canCreateVaults: boolean, includeAdminNav = true): TourStep[] {
  return [
    { titleKey: 'onboarding.welcome.title', descriptionKey: 'onboarding.welcome.description', vignette: WelcomeVignette },
    { target: vaultListTarget, titleKey: 'onboarding.vaultList.title', descriptionKey: canCreateVaults ? 'onboarding.vaultList.description' : 'onboarding.vaultList.description.shared', vignette: VaultListVignette },
    canCreateVaults
      ? { target: '[data-tour="addVault"]', titleKey: 'onboarding.addVault.title', descriptionKey: 'onboarding.addVault.description' }
      : { target: vaultListTarget, titleKey: 'onboarding.useVault.title', descriptionKey: 'onboarding.useVault.description' },
    ...(includeAdminNav ? [{ target: adminNavTarget, titleKey: 'onboarding.adminNav.title', descriptionKey: 'onboarding.adminNav.description' }] : []),
    { target: '[data-tour="profile"]', titleKey: 'onboarding.profile.title', descriptionKey: 'onboarding.profile.description', vignette: ProfileVignette }
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

// driver.js rebuilds the popover per step, so each render unmounts the previous step's content app;
// module scope lets a replayed tour clean up a predecessor whose onDestroyed hook driver.js skipped
let contentApp: App | undefined;

function unmountContent() {
  contentApp?.unmount();
  contentApp = undefined;
}

async function driveTour(includedSteps: TourStep[], onTourDestroyed?: () => void) {
  const { t } = i18n.global;
  await waitForVisibleElement(vaultListTarget);

  const steps: DriveStep[] = [];
  const contents: TourPopoverProps[] = [];
  for (const step of includedSteps) {
    const element = step.target ? findVisibleElement(step.target) : undefined;
    if (step.target && !element) {
      continue;
    }
    // the blank placeholder keeps driver.js from hiding the description element it hands to onPopoverRender
    steps.push({ element: element, popover: { title: t(step.titleKey), description: ' ' } });
    contents.push({ content: step.vignette, text: t(step.descriptionKey) });
  }
  steps.push({ popover: { title: t('onboarding.getApp.title'), description: ' ' } });
  contents.push({ content: GetAppStep });

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
    onPopoverRender: (popover, { state }) => {
      unmountContent();
      const content = state.activeIndex !== undefined ? contents[state.activeIndex] : undefined;
      if (!content) {
        return;
      }
      contentApp = createApp(TourPopoverContent, content);
      contentApp.mount(popover.description);
    },
    onDestroyed: () => {
      unmountContent();
      onTourDestroyed?.();
    },
    steps: steps
  }).drive();
}
