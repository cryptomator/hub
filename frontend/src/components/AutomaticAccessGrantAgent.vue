<template>
  <!--
    Effectively renderless: this agent performs the automatic access grant flow in the background while a set-up user is
    logged in. The hidden placeholder leaves room for future UI (e.g. a dialog asking the user to confirm proposed
    grants), which would be surfaced from `submitGrants`.
  -->
  <span class="hidden" aria-hidden="true" />
</template>

<script setup lang="ts">
import { base64 } from '@scure/base';
import { onBeforeUnmount, onMounted } from 'vue';
import backend, { AccessGrant, PendingAccessGrants, isUvfVault } from '../common/backend';
import { UniversalVaultFormat } from '../common/universalVaultFormat';
import userdata from '../common/userdata';
import { unwrapVaultKeys } from '../common/vaultKeys';
import wot from '../common/wot';

/*
 * This component is the reference implementation of the automatic access grant client protocol. Alternative clients
 * (desktop, mobile, CLI) can be modelled after the flow below.
 *
 * Security model — every input to the grant decision comes from a source the server cannot forge:
 *  - The trust threshold (`maxWotDepth`) and the on/off switch (`enabled`) are read from the vault's encrypted,
 *    tamper-proof UVF metadata, NOT from /api/settings. An evil DB admin therefore cannot lower the bar.
 *  - The trust path is cryptographically verified with `wot.verify`, which also confirms that the chain attests the
 *    recipient's actual public keys — so the server cannot substitute an attacker-controlled key.
 *  - The vault key is only ever available because *this* user is already a member and could share it manually anyway.
 */

const POLL_WAIT_SECONDS = 25;
const ERROR_BACKOFF_MS = 30_000;
// Incremental backoff applied when a poll returns only candidates we have already ruled out (see blocklists below).
const BLOCKED_BACKOFF_BASE_MS = 10_000;
const BLOCKED_BACKOFF_MAX_MS = 600_000;

let running = false;

// Session-scoped blocklists, so we neither re-decrypt vaults nor re-evaluate candidates we have already ruled out. They
// are intentionally not persisted: a fresh page load (or re-login) re-evaluates everything, which is how a vault that
// later enables auto-grant, or a candidate whose trust is later established, is eventually picked up.
const disqualifiedVaults = new Set<string>(); // vaults that don't qualify for auto grant (not UVF, or auto-grant disabled)
// Untrusted user ids. Trust is a property of the (current user, candidate) relationship, not of a vault, so a single
// set suffices: a user we won't grant on one vault, we won't grant on another. (With per-vault trust-threshold
// overrides this can over-block — failing a stricter vault blocklists the user for a more lenient one too — which is an
// acceptable conservative trade-off for a best-effort flow.)
const untrustedCandidates = new Set<string>();

/**
 * Filters the pending grants down to the work not already ruled out: drops disqualified vaults and untrusted candidates,
 * and omits vaults left with no candidates. The result is the set of grants actually worth evaluating this cycle.
 */
function freshWork(pending: PendingAccessGrants): Map<string, string[]> {
  const work = new Map<string, string[]>();
  for (const [vaultId, userIds] of Object.entries(pending)) {
    if (disqualifiedVaults.has(vaultId)) {
      continue;
    }
    const freshUserIds = userIds.filter(userId => !untrustedCandidates.has(userId));
    if (freshUserIds.length > 0) {
      work.set(vaultId, freshUserIds);
    }
  }
  return work;
}

onMounted(async () => {
  // Only fully set-up users can grant: we need their private keys to unwrap and re-wrap vault keys.
  const me = await userdata.me;
  if (!me.ecdhPublicKey) {
    return;
  }
  running = true;
  void loop();
});

onBeforeUnmount(() => {
  running = false;
});

async function loop(): Promise<void> {
  let backoffMillis = BLOCKED_BACKOFF_BASE_MS;
  while (running) {
    try {
      // The endpoint only returns candidates this user can be expected to grant (vaults they can decrypt, recipients
      // they have a Web-of-Trust path to). It blocks up to POLL_WAIT_SECONDS while there is nothing to return.
      const pending = await backend.vaults.listPendingAccessGrants(POLL_WAIT_SECONDS);

      // The server cannot see a vault's (encrypted) enabled flag or exact trust threshold, so it may still return items
      // we have already ruled out locally. If nothing pending is still worth evaluating, there is nothing to do — apply
      // an incremental backoff instead of re-fetching/re-decrypting on a tight loop.
      const work = freshWork(pending);
      if (work.size === 0) {
        await sleep(backoffMillis);
        backoffMillis = Math.min(backoffMillis * 2, BLOCKED_BACKOFF_MAX_MS);
        continue;
      }
      backoffMillis = BLOCKED_BACKOFF_BASE_MS; // there is something new to evaluate; reset the backoff

      for (const [vaultId, candidateUserIds] of work) {
        if (!running) {
          return;
        }
        try {
          await processVault(vaultId, candidateUserIds);
        } catch (error) {
          // e.g. this user holds no token for the vault (so cannot share its key) — skip it, keep processing others.
          console.warn(`Automatic access grant for vault ${vaultId} failed; skipping.`, error);
        }
      }
    } catch (error) {
      if (!running) {
        return;
      }
      console.warn('Automatic access grant cycle failed; backing off.', error);
      await sleep(ERROR_BACKOFF_MS);
    }
  }
}

async function processVault(vaultId: string, candidateUserIds: string[]): Promise<void> {
  const me = await userdata.me;
  const vault = await backend.vaults.get(vaultId);
  if (!isUvfVault(vault)) {
    disqualifiedVaults.add(vaultId); // legacy vaults carry no auto-grant policy and are never auto-granted
    return;
  }

  const vaultKeys: UniversalVaultFormat = await unwrapVaultKeys(vault);
  const { enabled, maxWotDepth } = vaultKeys.metadata.automaticAccessGrant;
  if (!enabled) {
    disqualifiedVaults.add(vaultId); // this vault has opted out of automatic access grant
    return;
  }

  const candidates = await backend.authorities.listSome(candidateUserIds, false);
  const grants: AccessGrant[] = [];
  for (const candidate of candidates) {
    if (candidate.id === me.id) {
      continue; // never grant to self
    }
    if (candidate.type !== 'USER' || !candidate.ecdhPublicKey || !candidate.ecdsaPublicKey) {
      untrustedCandidates.add(candidate.id); // groups / not-yet-set-up users can't receive a key
      continue;
    }
    if (!await isTrusted(candidate.id, candidate.ecdhPublicKey, candidate.ecdsaPublicKey, maxWotDepth)) {
      untrustedCandidates.add(candidate.id); // no usable / sufficiently-short trust path to this user
      continue;
    }
    const publicKey = base64.decode(candidate.ecdhPublicKey) as Uint8Array<ArrayBuffer>;
    const token = await vaultKeys.encryptForUser(publicKey); // member-level access; recovery keys are never auto-shared
    grants.push({ userId: candidate.id, token });
  }
  if (grants.length > 0) {
    await submitGrants(vaultId, grants);
  }
}

/**
 * Decides whether a candidate is trusted enough to be granted access automatically, using only tamper-proof inputs:
 * the threshold comes from the vault's UVF metadata, and the trust chain (provided by the server) is cryptographically
 * verified — including that it attests the candidate's actual public keys.
 *
 * @param userId the candidate's id
 * @param ecdhPublicKey the candidate's ECDH public key (base64), as the server reports it
 * @param ecdsaPublicKey the candidate's ECDSA public key (base64), as the server reports it
 * @param maxWotDepth the vault's trust threshold: -1 disables the check, otherwise the maximum signature-chain length
 */
async function isTrusted(userId: string, ecdhPublicKey: string, ecdsaPublicKey: string, maxWotDepth: number): Promise<boolean> {
  if (maxWotDepth === -1) {
    return true; // trust check disabled by the vault's policy
  }
  const trust = await backend.trust.get(userId);
  if (!trust || trust.signatureChain.length > maxWotDepth) {
    return false; // no trust path, or the candidate is too distant
  }
  try {
    // Verifies the chain starts at my key and ends at the candidate's reported keys; throws on any mismatch, which
    // would mean the server tried to feed us a forged chain or substitute a key.
    await wot.verify(trust.signatureChain, { ecdhPublicKey, ecdsaPublicKey });
    return true;
  } catch (error) {
    console.warn(`Refusing automatic grant to ${userId}: trust chain verification failed.`, error);
    return false;
  }
}

/**
 * Submits the computed grants. This is the seam for future UI: instead of granting immediately, the proposals could be
 * surfaced in the template and only submitted here after the user confirms them.
 */
async function submitGrants(vaultId: string, grants: AccessGrant[]): Promise<void> {
  await backend.vaults.autoGrantAccess(vaultId, ...grants);
}

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}
</script>
