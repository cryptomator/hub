import { Window } from 'happy-dom';

// Node >= 25 ships its own localStorage global (undefined unless --localstorage-file is set),
// which vitest's environment will not override with happy-dom's working implementation
if (typeof localStorage === 'undefined') {
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: new Window().localStorage });
}
