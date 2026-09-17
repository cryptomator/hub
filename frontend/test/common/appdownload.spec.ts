import { afterEach, describe, expect, it, vi } from 'vitest';

const androidUserAgent = 'Mozilla/5.0 (Linux; Android 14)';
const iphoneUserAgent = 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)';
const windowsUserAgent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)';
const macUserAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)';

describe('appDownloadUrl', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  ([
    [androidUserAgent, 'https://play.google.com/store/apps/details?id=org.cryptomator.freemium'],
    [iphoneUserAgent, 'https://apps.apple.com/app/cryptomator/id1560822163'],
    [windowsUserAgent, 'https://cryptomator.org/downloads/win/thanks/'],
    ['Mozilla/5.0 (X11; Linux x86_64)', 'https://cryptomator.org/downloads/linux/thanks/'],
    ['SomethingElse/1.0', 'https://cryptomator.org/downloads/']
  ] as const).forEach(([userAgent, url]) => {
    it(`resolves the download url for ${userAgent}`, async () => {
      const { appDownloadUrl } = await loadAppDownload(userAgent);

      expect(appDownloadUrl()).toBe(url);
    });
  });

  ([
    ['Apple M2', 'https://cryptomator.org/downloads/mac-arm64/thanks/'],
    ['Intel Iris OpenGL Engine', 'https://cryptomator.org/downloads/mac/thanks/'],
    ['AMD Radeon Pro 5500M', 'https://cryptomator.org/downloads/mac/thanks/'],
    ['Unknown GPU', 'https://cryptomator.org/downloads/#mac']
  ] as const).forEach(([renderer, url]) => {
    it(`tells mac architectures apart via the WebGL renderer "${renderer}"`, async () => {
      stubGetContext(webGlContext(renderer));
      const { appDownloadUrl } = await loadAppDownload(macUserAgent);

      expect(appDownloadUrl()).toBe(url);
    });
  });

  it('treats iPadOS pretending to be a Mac as an Apple mobile device', async () => {
    vi.spyOn(navigator, 'maxTouchPoints', 'get').mockReturnValue(5);
    const { appDownloadUrl, isMobile } = await loadAppDownload(macUserAgent);

    expect(appDownloadUrl()).toBe('https://apps.apple.com/app/cryptomator/id1560822163');
    expect(isMobile()).toBe(true);
  });

  it('sniffs the mac renderer only once', async () => {
    const getContext = stubGetContext(webGlContext('Apple M2'));
    const { appDownloadUrl } = await loadAppDownload(macUserAgent);

    expect(appDownloadUrl()).toBe(appDownloadUrl());
    expect(getContext).toHaveBeenCalledOnce();
  });

  it('caches the fallback when the context lacks the renderer info extension', async () => {
    const getContext = stubGetContext(webGlContext());
    const { appDownloadUrl } = await loadAppDownload(macUserAgent);

    expect(appDownloadUrl()).toBe('https://cryptomator.org/downloads/#mac');
    expect(appDownloadUrl()).toBe('https://cryptomator.org/downloads/#mac');
    expect(getContext).toHaveBeenCalledOnce();
  });

  it('retries the sniff when no WebGL context was available', async () => {
    const getContext = stubGetContext(null);
    const { appDownloadUrl } = await loadAppDownload(macUserAgent);

    expect(appDownloadUrl()).toBe('https://cryptomator.org/downloads/#mac');
    getContext.mockRestore();
    stubGetContext(webGlContext('Apple M2'));
    expect(appDownloadUrl()).toBe('https://cryptomator.org/downloads/mac-arm64/thanks/');
  });

  it('retries the sniff when creating the WebGL context throws', async () => {
    const getContext = vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockImplementation(() => {
      throw new Error('webgl blocked');
    });
    const { appDownloadUrl } = await loadAppDownload(macUserAgent);

    expect(appDownloadUrl()).toBe('https://cryptomator.org/downloads/#mac');
    getContext.mockRestore();
    stubGetContext(webGlContext('Apple M2'));
    expect(appDownloadUrl()).toBe('https://cryptomator.org/downloads/mac-arm64/thanks/');
  });
});

describe('isMobile', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  ([
    [iphoneUserAgent, true],
    [androidUserAgent, true],
    [windowsUserAgent, false],
    [macUserAgent, false]
  ] as const).forEach(([userAgent, mobile]) => {
    it(`classifies ${userAgent}`, async () => {
      const { isMobile } = await loadAppDownload(userAgent);

      expect(isMobile()).toBe(mobile);
    });
  });
});

/* ---------- MOCKS ---------- */

// the module memoizes the mac sniff, so each test imports a fresh copy
async function loadAppDownload(userAgent: string): Promise<typeof import('../../src/common/appdownload')> {
  vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue(userAgent);
  vi.resetModules();
  return await import('../../src/common/appdownload');
}

function webGlContext(renderer?: string) {
  return {
    getExtension: (name: string) => name === 'WEBGL_debug_renderer_info' && renderer !== undefined ? { UNMASKED_RENDERER_WEBGL: 0x9246 } : null,
    getParameter: () => renderer
  };
}

function stubGetContext(gl: ReturnType<typeof webGlContext> | null) {
  return vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(gl as never);
}
