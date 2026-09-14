import { DriveStep, driver } from 'driver.js';
import { createApp, ref } from 'vue';
import type { App, Component } from 'vue';
import GetAppStep from '../components/onboarding/GetAppStep.vue';
import ProfileVignette from '../components/onboarding/ProfileVignette.vue';
import TourPopoverContent, { TourPopoverProps } from '../components/onboarding/TourPopoverContent.vue';
import VaultListVignette from '../components/onboarding/VaultListVignette.vue';
import WelcomeVignette from '../components/onboarding/WelcomeVignette.vue';
import i18n from '../i18n';
import auth from './auth';
import { isFlagSet, setFlag } from './util';
import 'driver.js/dist/driver.css';

type TourStep = {
  target?: string;
  showWithoutTarget?: boolean;
  titleKey: string;
  descriptionKey?: string;
  vignette?: Component;
};

const vaultListTarget = '[data-tour="vaultList"]';

let activeTour: ReturnType<typeof driver> | undefined;

// driver.js rebuilds the popover per step, so each render unmounts the previous step's content app;
// module scope lets a replayed tour clean up a predecessor whose onDestroyed hook driver.js skipped
let contentApp: App | undefined;

function unmountContent() {
  contentApp?.unmount();
  contentApp = undefined;
}

// addVault must be gated explicitly because the button is always rendered, merely disabled;
// the admin step needs no role check: its target only exists for admins and invisible steps are skipped
/** exported for testing */
export function tourSteps(canCreateVaults: boolean): TourStep[] {
  return [
    { titleKey: 'onboarding.welcome.title', descriptionKey: 'onboarding.welcome.description', vignette: WelcomeVignette },
    { target: vaultListTarget, titleKey: 'onboarding.vaultList.title', descriptionKey: canCreateVaults ? 'onboarding.vaultList.owner.description' : 'onboarding.vaultList.member.description', vignette: VaultListVignette },
    canCreateVaults
      ? { target: '[data-tour="addVault"]', titleKey: 'onboarding.addVault.title', descriptionKey: 'onboarding.addVault.description' }
      : { titleKey: 'onboarding.useVault.title', descriptionKey: 'onboarding.useVault.description' },
    { target: '[data-tour="adminNav"]', titleKey: 'onboarding.adminNav.title', descriptionKey: 'onboarding.adminNav.description' },
    // the account step must reach everyone, so it falls back to a floating card when the sidebar is hidden
    { target: '[data-tour="profile"]', showWithoutTarget: true, titleKey: 'onboarding.profile.title', descriptionKey: 'onboarding.profile.description', vignette: ProfileVignette },
    { titleKey: 'onboarding.getApp.title', vignette: GetAppStep }
  ];
}

function onboardingKey(userId: string): string {
  return `hub.onboardingCompleted.${userId}`;
}

// bumped on completion, so long-lived components like the sidebar react without a reload
const completionVersion = ref(0);

/** also gates UI that should only appear once the tour is over, e.g. the sidebar app hint */
export function isOnboardingCompleted(userId: string): boolean {
  void completionVersion.value; // subscribes reactive callers to tour completions
  return isFlagSet(onboardingKey(userId));
}

function completeOnboarding(userId: string) {
  setFlag(onboardingKey(userId));
  completionVersion.value++;
}

function findVisibleElement(selector: string): Element | undefined {
  return Array.from(document.querySelectorAll(selector)).find(element => element.checkVisibility() && !element.matches(':disabled'));
}

// driver.js' own waitForElement cannot be used here: the steps are filtered and their duplicate
// anchors resolved to the visible instance before the tour is built
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
  await driveTour(tourSteps((await auth).hasRole('create-vaults')), () => completeOnboarding(userId));
}

/**
 * Stops a currently running tour without marking it as completed,
 * e.g. when the view the tour is anchored to unmounts.
 */
export function stopOnboarding() {
  const tour = activeTour;
  activeTour = undefined;
  tour?.destroy();
  // driver.js skips onDestroyed during its highlight transition, so do not rely on it for cleanup
  unmountContent();
}

async function driveTour(includedSteps: TourStep[], onTourDestroyed: () => void) {
  const { t } = i18n.global;
  if (!await waitForVisibleElement(vaultListTarget)) {
    console.warn('Onboarding tour not started because the vault list did not become visible');
    return;
  }
  // concurrent starts (e.g. a replay while the auto-start is still waiting) converge on the last caller
  stopOnboarding();

  const steps: DriveStep[] = [];
  const titles: string[] = [];
  const contents: TourPopoverProps[] = [];
  for (const step of includedSteps) {
    const element = step.target ? findVisibleElement(step.target) : undefined;
    if (step.target && !element && !step.showWithoutTarget) {
      continue;
    }
    // blank placeholders keep driver.js from hiding the title and description elements it hands to onPopoverRender
    steps.push({ element, popover: { title: ' ', description: ' ' } });
    titles.push(t(step.titleKey));
    contents.push({ vignette: step.vignette, text: step.descriptionKey ? t(step.descriptionKey) : undefined });
  }

  const tour = driver({
    popoverClass: 'onboarding-popover',
    overlayColor: 'var(--color-gray-500, #6b7280)',
    overlayOpacity: 0.75,
    stagePadding: 8,
    stageRadius: 8,
    showProgress: true,
    onPopoverRender: (popover, { state }) => {
      unmountContent();
      const index = state.activeIndex;
      const content = index !== undefined ? contents[index] : undefined;
      if (index === undefined || !content) {
        return;
      }
      // driver.js assigns all of these via innerHTML, so set them as text to keep translations inert
      popover.title.textContent = titles[index];
      popover.progress.textContent = t('onboarding.progress', [index + 1, steps.length]);
      popover.previousButton.textContent = t('common.previous');
      popover.nextButton.textContent = index === steps.length - 1 ? t('onboarding.done') : t('common.next');
      popover.closeButton.setAttribute('aria-label', t('common.close'));
      contentApp = createApp(TourPopoverContent, content);
      contentApp.mount(popover.description);
    },
    onDestroyed: () => {
      unmountContent();
      // a tour cancelled via stopOnboarding() has already been detached and must not count as completed
      if (activeTour === tour) {
        activeTour = undefined;
        onTourDestroyed();
      }
    },
    steps
  });
  activeTour = tour;
  tour.drive();
}
