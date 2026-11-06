import { Ref, ref } from 'vue';
import backend, { EntitlementsDto } from './backend';

class LicenseEntitlements {

  #entitlements?: Promise<EntitlementsDto>;
  readonly #current = ref<EntitlementsDto>();

  /**
   * The most recently loaded entitlements, `undefined` until they have been loaded for the first time.
   * Reactive, so every component using it reflects a {@link reload}.
   */
  public get current(): Readonly<Ref<EntitlementsDto | undefined>> {
    return this.#current;
  }

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
    entitlements.then(loaded => {
      if (this.#entitlements === entitlements) {
        this.#current.value = loaded; // a request superseded by a newer reload must not overwrite its result
      }
    }, () => {
      if (this.#entitlements === entitlements) {
        this.#entitlements = undefined; // don't cache failures, so the next access retries
      }
    });
    return entitlements;
  }

}

const instance = new LicenseEntitlements();
export default instance;
