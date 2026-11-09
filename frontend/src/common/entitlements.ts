import backend, { EntitlementsDto } from './backend';

class LicenseEntitlements {

  #entitlements?: Promise<EntitlementsDto>;

  /**
   * Gets the entitlements of the license installed on this Hub instance.
   */
  public get(): Promise<EntitlementsDto> {
    if (!this.#entitlements) {
      this.#entitlements = this.#load();
    }
    return this.#entitlements;
  }

  /**
   * Invalidates the cached entitlements and reloads them from the backend, e.g. after the license changed.
   */
  public reload(): Promise<EntitlementsDto> {
    this.#entitlements = this.#load();
    return this.#entitlements;
  }

  #load(): Promise<EntitlementsDto> {
    const entitlements = backend.license.getEntitlements();
    entitlements.catch(() => {
      if (this.#entitlements === entitlements) {
        this.#entitlements = undefined; // don't cache failures, so the next access retries
      }
    });
    return entitlements;
  }

}

const instance = new LicenseEntitlements();
export default instance;
