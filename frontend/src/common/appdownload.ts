export function isMobile(): boolean {
  return /android|iphone|ipad|ipod/i.test(navigator.userAgent) || isIpadOs();
}

// iPadOS reports itself as a Mac in the user agent; the touch screen gives it away
function isIpadOs(): boolean {
  return /mac/i.test(navigator.userAgent) && navigator.maxTouchPoints > 1;
}

export function appDownloadUrl(): string {
  const ua = navigator.userAgent;
  if (/android/i.test(ua)) {
    return 'https://play.google.com/store/apps/details?id=org.cryptomator.freemium';
  } else if (/iphone|ipad|ipod/i.test(ua) || isIpadOs()) {
    return 'https://apps.apple.com/app/cryptomator/id1560822163';
  } else if (/windows/i.test(ua)) {
    return 'https://cryptomator.org/downloads/win/thanks/';
  } else if (/mac/i.test(ua)) {
    return cachedMacDownloadUrl ?? macDownloadUrl();
  } else if (/linux/i.test(ua)) {
    return 'https://cryptomator.org/downloads/linux/thanks/';
  } else {
    return 'https://cryptomator.org/downloads/';
  }
}

let cachedMacDownloadUrl: string | undefined;

const macFallbackUrl = 'https://cryptomator.org/downloads/#mac';

// same WebGL renderer sniffing as cryptomator.org uses to tell Apple Silicon and Intel Macs apart;
// the context is released right away and the result cached, as browsers cap concurrent WebGL contexts
function macDownloadUrl(): string {
  try {
    const gl = document.createElement('canvas').getContext('webgl');
    if (gl) {
      const info = gl.getExtension('WEBGL_debug_renderer_info');
      const renderer = info ? String(gl.getParameter(info.UNMASKED_RENDERER_WEBGL)).toLowerCase() : '';
      gl.getExtension('WEBGL_lose_context')?.loseContext();
      cachedMacDownloadUrl = macUrlForRenderer(renderer);
      return cachedMacDownloadUrl;
    }
  } catch {
    // detection is best-effort only
  }
  return macFallbackUrl;
}

function macUrlForRenderer(renderer: string): string {
  if (renderer.includes('apple')) {
    return 'https://cryptomator.org/downloads/mac-arm64/thanks/';
  } else if (/intel|amd|radeon/.test(renderer)) {
    return 'https://cryptomator.org/downloads/mac/thanks/';
  } else {
    return macFallbackUrl;
  }
}
