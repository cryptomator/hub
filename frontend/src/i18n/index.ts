import { I18nOptions } from 'vue-i18n';
import de from './de-DE.json';
import en from './en-US.json';

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
