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
// The single best-effort cadence knob. After a poll that returned (possibly ungrantable) work, we wait this long before
// re-polling, so we neither hammer the backend nor re-decrypt vaults on a tight loop. It doubles as the "forget"
// mechanism: there is no blocklist — every cycle re-evaluates all pending candidates from scratch — so a candidate ruled
// out earlier (e.g. not yet trusted, or on a vault whose policy was disabled) is simply retried on the next cycle, which
// is how a later-established trust or changed vault policy gets picked up.
const RETRY_INTERVAL_MS = 120_000;

let running = false;

// A vault's auto-grant eligibility is fixed for its lifetime — a legacy (non-UVF) vault never qualifies, and the
// `enabled` flag lives in the vault's immutable, encrypted UVF metadata — so once ruled out we never re-examine it,
// sparing a fetch + decrypt every cycle. This is unlike user trust, which can change and is therefore re-evaluated
// every cycle. Session-scoped (not persisted): a reload re-checks, which is how a newly-created qualifying vault is
// eventually picked up.
const disqualifiedVaults = new Set<string>();

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
  while (running) {
    try {
      // The endpoint returns pending grants on vaults this user can decrypt, blocking up to POLL_WAIT_SECONDS while
      // there is nothing to return. It cannot evaluate a vault's (encrypted) policy or the Web of Trust, so some
      // returned candidates may turn out to be ungrantable; we just skip those and let the next cycle retry them.
      const pending = await backend.vaults.listPendingAccessGrants(POLL_WAIT_SECONDS);
      const grantedSomething = await processPending(pending);
      // Throttle only when a poll returned candidates but we granted none of them: the server would keep returning those
      // ungrantable candidates immediately, so wait before retrying instead of tight-looping. If we granted something we
      // re-poll at once (more may have become grantable), and when idle we re-enter the long-poll at once so genuinely
      // new members are still picked up promptly.
      if (running && Object.keys(pending).length > 0 && !grantedSomething) {
        await sleep(RETRY_INTERVAL_MS);
      }
    } catch (error) {
      if (!running) {
        return;
      }
      console.warn('Automatic access grant cycle failed; retrying later.', error);
      await sleep(RETRY_INTERVAL_MS);
    }
  }
}

/** Evaluates one poll's pending grants, granting what it can. Returns whether any access was granted. */
async function processPending(pending: PendingAccessGrants): Promise<boolean> {
  let grantedSomething = false;
  for (const [vaultId, candidateUserIds] of Object.entries(pending)) {
    if (!running) {
      break;
    }
    if (disqualifiedVaults.has(vaultId)) {
      continue; // permanently ineligible (see disqualifiedVaults) — skip without re-fetching/decrypting
    }
    try {
      grantedSomething = await processVault(vaultId, candidateUserIds) || grantedSomething;
    } catch (error) {
      // e.g. this user holds no token for the vault (so cannot share its key) — skip it, keep processing others.
      console.warn(`Automatic access grant for vault ${vaultId} failed; skipping.`, error);
    }
  }
  return grantedSomething;
}

async function processVault(vaultId: string, candidateUserIds: string[]): Promise<boolean> {
  const me = await userdata.me;
  const vault = await backend.vaults.get(vaultId);
  if (!isUvfVault(vault)) {
    disqualifiedVaults.add(vaultId); // legacy vaults carry no auto-grant policy and are never auto-granted
    return false;
  }

  const vaultKeys: UniversalVaultFormat = await unwrapVaultKeys(vault);
  const { enabled, maxWotDepth } = vaultKeys.metadata.automaticAccessGrant;
  if (!enabled) {
    disqualifiedVaults.add(vaultId); // this vault has opted out of automatic access grant
    return false;
  }

  const candidates = await backend.authorities.listSome(candidateUserIds, false);
  const grants: AccessGrant[] = [];
  for (const candidate of candidates) {
    if (candidate.id === me.id) {
      continue; // never grant to self
    }
    if (candidate.type !== 'USER' || !candidate.ecdhPublicKey || !candidate.ecdsaPublicKey) {
      continue; // groups / not-yet-set-up users can't receive a key
    }
    if (!await isTrusted(candidate.id, candidate.ecdhPublicKey, candidate.ecdsaPublicKey, maxWotDepth)) {
      continue; // no usable / sufficiently-short trust path to this user (yet) — retried next cycle
    }
    const publicKey = base64.decode(candidate.ecdhPublicKey) as Uint8Array<ArrayBuffer>;
    const token = await vaultKeys.encryptForUser(publicKey); // member-level access; recovery keys are never auto-shared
    grants.push({ userId: candidate.id, token });
  }
  if (grants.length === 0) {
    return false;
  }
  await submitGrants(vaultId, grants);
  return true;
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
