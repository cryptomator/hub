import { I18nOptions } from 'vue-i18n';
import de from './de-DE.json';
import en from './en-US.json';
import it from './it-IT.json';
import ko from './ko-KR.json';
import nl from './nl-NL.json';
import pt from './pt-PT.json';

import { createI18n } from 'vue-i18n';

// ISO 639‑1 two letter code
export enum Locale {
  EN = 'en',
  DE = 'de',
  IT = 'it',
  KO = 'ko',
  NL = 'nl',
  PT = 'pt'
}

export const messages = {
  [Locale.EN]: en,
  [Locale.DE]: de,
  [Locale.IT]: it,
  [Locale.KO]: ko,
  [Locale.NL]: nl,
  [Locale.PT]: pt
};

const defaultShortDatetimeFormat = {
  short: {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  }
} as const;

export const datetimeFormats: I18nOptions['datetimeFormats'] = {
  [Locale.EN]: defaultShortDatetimeFormat,
  [Locale.DE]: defaultShortDatetimeFormat,
  [Locale.IT]: defaultShortDatetimeFormat,
  [Locale.KO]: defaultShortDatetimeFormat,
  [Locale.NL]: defaultShortDatetimeFormat,
  [Locale.PT]: defaultShortDatetimeFormat,
};

const defaultNumberFormat = {
  percent: {
    style: 'percent',
    useGrouping: false
  }
} as const;

export const numberFormats: I18nOptions['numberFormats'] = {
  [Locale.EN]: defaultNumberFormat,
  [Locale.DE]: defaultNumberFormat,
  [Locale.IT]: defaultNumberFormat,
  [Locale.KO]: defaultNumberFormat,
  [Locale.NL]: defaultNumberFormat,
  [Locale.PT]: defaultNumberFormat,
};

export const mapToLocale = (local: string): Locale =>
  (Object.values(Locale) as string[]).includes(local)
    ? (local as Locale)
    : Locale.EN;

const i18n = createI18n({
  locale: navigator.language,
  fallbackLocale: Locale.EN,
  messages,
  datetimeFormats,
  numberFormats,
  globalInjection: true,
  missingWarn: false,
  fallbackWarn: false,
  legacy: false
});

export default i18n;
