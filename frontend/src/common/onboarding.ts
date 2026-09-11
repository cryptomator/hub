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
  descriptionKey?: string;
  vignette?: Component;
};

const vaultListTarget = '[data-tour="vaultList"]';
const adminNavTarget = '[data-tour="adminNav"]';

// addVault must be gated explicitly because the button is always rendered, merely disabled;
// the admin step needs no role check: its target only exists for admins and invisible steps are skipped
/** exported for testing */
export function tourSteps(canCreateVaults: boolean): TourStep[] {
  return [
    { titleKey: 'onboarding.welcome.title', descriptionKey: 'onboarding.welcome.description', vignette: WelcomeVignette },
    { target: vaultListTarget, titleKey: 'onboarding.vaultList.title', descriptionKey: canCreateVaults ? 'onboarding.vaultList.description' : 'onboarding.vaultList.description.shared', vignette: VaultListVignette },
    canCreateVaults
      ? { target: '[data-tour="addVault"]', titleKey: 'onboarding.addVault.title', descriptionKey: 'onboarding.addVault.description' }
      : { target: vaultListTarget, titleKey: 'onboarding.useVault.title', descriptionKey: 'onboarding.useVault.description' },
    { target: adminNavTarget, titleKey: 'onboarding.adminNav.title', descriptionKey: 'onboarding.adminNav.description' },
    { target: '[data-tour="profile"]', titleKey: 'onboarding.profile.title', descriptionKey: 'onboarding.profile.description', vignette: ProfileVignette },
    { titleKey: 'onboarding.getApp.title', vignette: GetAppStep }
  ];
}

function onboardingKey(userId: string): string {
  return `hub.onboardingCompleted.${userId}`;
}

function isOnboardingCompleted(userId: string): boolean {
  try {
    return localStorage.getItem(onboardingKey(userId)) !== null;
  } catch {
    // without localStorage we cannot remember a completion, so rather skip the tour than repeat it on every visit
    return true;
  }
}

function setOnboardingCompleted(userId: string) {
  try {
    localStorage.setItem(onboardingKey(userId), new Date().toISOString());
  } catch {
    // if localStorage is unavailable, the tour may show again next time
  }
}

function findVisibleElement(selector: string): Element | undefined {
  return Array.from(document.querySelectorAll(selector)).find(element => element.checkVisibility());
}

async function waitForVisibleElement(selector: string): Promise<boolean> {
  for (let attempt = 0; attempt < 80 && !findVisibleElement(selector); attempt++) {
    await new Promise(resolve => setTimeout(resolve, 50));
  }
  return findVisibleElement(selector) !== undefined;
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
 * Starts the onboarding tour on the vault list; resolves once the tour is running.
 * Completion is recorded when the user finishes or dismisses the tour.
 * Steps whose target element is not visible (e.g. the sidebar on mobile) are skipped.
 * @param userId the id of the currently logged in user
 */
export async function startOnboarding(userId: string) {
  await driveTour(tourSteps((await auth).hasRole('create-vaults')), () => setOnboardingCompleted(userId));
}

/**
 * Stops a currently running tour without marking it as completed,
 * e.g. when the view the tour is anchored to unmounts.
 */
export function stopOnboarding() {
  const tour = activeTour;
  activeTour = undefined;
  tour?.destroy();
}

let activeTour: ReturnType<typeof driver> | undefined;

// driver.js rebuilds the popover per step, so each render unmounts the previous step's content app;
// module scope lets a replayed tour clean up a predecessor whose onDestroyed hook driver.js skipped
let contentApp: App | undefined;

function unmountContent() {
  contentApp?.unmount();
  contentApp = undefined;
}

async function driveTour(includedSteps: TourStep[], onTourDestroyed?: () => void) {
  const { t } = i18n.global;
  if (!await waitForVisibleElement(vaultListTarget)) {
    console.warn('Onboarding tour not started because the vault list did not become visible');
    return;
  }
  stopOnboarding();

  const steps: DriveStep[] = [];
  const contents: TourPopoverProps[] = [];
  for (const step of includedSteps) {
    const element = step.target ? findVisibleElement(step.target) : undefined;
    if (step.target && !element) {
      continue;
    }
    // the blank placeholder keeps driver.js from hiding the description element it hands to onPopoverRender
    steps.push({ element, popover: { title: t(step.titleKey), description: ' ' } });
    contents.push({ content: step.vignette, text: step.descriptionKey ? t(step.descriptionKey) : undefined });
  }

  activeTour = driver({
    popoverClass: 'onboarding-popover',
    overlayColor: 'var(--color-gray-500)',
    overlayOpacity: 0.75,
    stagePadding: 8,
    stageRadius: 8,
    showProgress: true,
    // driver.js substitutes {{current}} and {{total}} itself, vue-i18n only places them in the message
    progressText: t('onboarding.progress', ['{{current}}', '{{total}}']),
    nextBtnText: t('common.next'),
    prevBtnText: t('common.previous'),
    doneBtnText: t('onboarding.done'),
    onPopoverRender: (popover, { state }) => {
      popover.closeButton.setAttribute('aria-label', t('common.close'));
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
      // a tour cancelled via stopOnboarding() has already been detached and must not count as completed
      if (activeTour !== undefined) {
        activeTour = undefined;
        onTourDestroyed?.();
      }
    },
    steps
  });
  activeTour.drive();
}
