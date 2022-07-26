import { I18nOptions } from 'vue-i18n';

export enum Locale {
  EN = 'en',
  DE = 'de'
}

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

export const defaultLocale = Locale.EN;
