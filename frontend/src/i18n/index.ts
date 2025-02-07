import { I18nOptions } from 'vue-i18n';
import de from './de-DE.json';
import en from './en-US.json';

import { createI18n } from 'vue-i18n';

export enum Locale {
  EN = 'en',
  DE = 'de'
}

export const messages = {
  [Locale.EN]: en,
  [Locale.DE]: de
};

export const datetimeFormats: I18nOptions['datetimeFormats'] = {
  [Locale.EN]: {
    short: {
      year: 'numeric', month: 'long', day: 'numeric'
    }
  },
  [Locale.DE]: {
    short: {
      year: 'numeric', month: 'long', day: 'numeric'
    }
  }
};

export const numberFormats: I18nOptions['numberFormats'] = {
  [Locale.EN]: {
    percent: {
      style: 'percent', useGrouping: false
    }
  },
  [Locale.DE]: {
    percent: {
      style: 'percent', useGrouping: false
    }
  }
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
