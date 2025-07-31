import { I18nOptions } from 'vue-i18n';
import deDe from './de-DE.json';
import enUs from './en-US.json';
import frFr from './fr-FR.json';
import itIt from './it-IT.json';
import koKr from './ko-KR.json';
import lvLv from './lv-LV.json';
import nlNl from './nl-NL.json';
import ptBr from './pt-BR.json';
import ptPt from './pt-PT.json';
import ruRu from './ru-RU.json';
import trTr from './tr-TR.json';
import uaUa from './uk-UA.json';
import zhTw from './zh-TW.json';

import { createI18n } from 'vue-i18n';

export enum Locale {
  ENUS = 'en-US',
  DEDE = 'de-DE',
  FRFR = 'fr-FR',
  ITIT = 'it-IT',
  KOKR = 'ko-KR',
  LVLV = 'lv-LV',
  NLNL = 'nl-NL',
  PTBR = 'pt-BR',
  PTPT = 'pt-PT',
  RURU = 'ru-RU',
  TRTR = 'tr-TR',
  UKUA = 'uk-UA',
  ZHTW = 'zh-TW',
}

export const messages = {
  [Locale.ENUS]: enUs,
  [Locale.DEDE]: deDe,
  [Locale.FRFR]: frFr,
  [Locale.ITIT]: itIt,
  [Locale.KOKR]: koKr,
  [Locale.LVLV]: lvLv,
  [Locale.NLNL]: nlNl,
  [Locale.PTBR]: ptBr,
  [Locale.PTPT]: ptPt,
  [Locale.RURU]: ruRu,
  [Locale.TRTR]: trTr,
  [Locale.UKUA]: uaUa,
  [Locale.ZHTW]: zhTw
};

const defaultShortDatetimeFormat = {
  short: {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  },
  long: {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit' 
  }
} as const;

export const datetimeFormats: I18nOptions['datetimeFormats'] = {
  [Locale.ENUS]: defaultShortDatetimeFormat,
  [Locale.DEDE]: defaultShortDatetimeFormat,
  [Locale.FRFR]: defaultShortDatetimeFormat,
  [Locale.ITIT]: defaultShortDatetimeFormat,
  [Locale.KOKR]: defaultShortDatetimeFormat,
  [Locale.LVLV]: defaultShortDatetimeFormat,
  [Locale.NLNL]: defaultShortDatetimeFormat,
  [Locale.PTBR]: defaultShortDatetimeFormat,
  [Locale.PTPT]: defaultShortDatetimeFormat,
  [Locale.RURU]: defaultShortDatetimeFormat,
  [Locale.TRTR]: defaultShortDatetimeFormat,
  [Locale.UKUA]: defaultShortDatetimeFormat,
  [Locale.ZHTW]: defaultShortDatetimeFormat,
};

const defaultNumberFormat = {
  percent: {
    style: 'percent',
    useGrouping: false
  }
} as const;

export const numberFormats: I18nOptions['numberFormats'] = {
  [Locale.ENUS]: defaultNumberFormat,
  [Locale.DEDE]: defaultNumberFormat,
  [Locale.FRFR]: defaultNumberFormat,
  [Locale.ITIT]: defaultNumberFormat,
  [Locale.KOKR]: defaultNumberFormat,
  [Locale.LVLV]: defaultNumberFormat,
  [Locale.NLNL]: defaultNumberFormat,
  [Locale.PTBR]: defaultNumberFormat,
  [Locale.PTPT]: defaultNumberFormat,
  [Locale.RURU]: defaultNumberFormat,
  [Locale.TRTR]: defaultNumberFormat,
  [Locale.UKUA]: defaultNumberFormat,
  [Locale.ZHTW]: defaultNumberFormat
};

export const mapToLocale = (local: string): Locale =>
  (Object.values(Locale) as string[]).includes(local)
    ? (local as Locale)
    : Locale.ENUS;

const i18n = createI18n({
  locale: navigator.language,
  fallbackLocale: Locale.ENUS,
  messages,
  datetimeFormats,
  numberFormats,
  globalInjection: true,
  missingWarn: false,
  fallbackWarn: false,
  legacy: false
});

export default i18n;
