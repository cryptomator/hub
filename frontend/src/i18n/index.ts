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
  EN_US = 'en-US',
  DE_DE = 'de-DE',
  FR_FR = 'fr-FR',
  IT_IT = 'it-IT',
  KO_KR = 'ko-KR',
  LV_LV = 'lv-LV',
  NL_NL = 'nl-NL',
  PT_BR = 'pt-BR',
  PT_PT = 'pt-PT',
  RU_RU = 'ru-RU',
  TR_TR = 'tr-TR',
  UK_UA = 'uk-UA',
  ZH_TW = 'zh-TW',
}

export const messages = {
  [Locale.EN_US]: enUs,
  [Locale.DE_DE]: deDe,
  [Locale.FR_FR]: frFr,
  [Locale.IT_IT]: itIt,
  [Locale.KO_KR]: koKr,
  [Locale.LV_LV]: lvLv,
  [Locale.NL_NL]: nlNl,
  [Locale.PT_BR]: ptBr,
  [Locale.PT_PT]: ptPt,
  [Locale.RU_RU]: ruRu,
  [Locale.TR_TR]: trTr,
  [Locale.UK_UA]: uaUa,
  [Locale.ZH_TW]: zhTw
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
  [Locale.EN_US]: defaultShortDatetimeFormat,
  [Locale.DE_DE]: defaultShortDatetimeFormat,
  [Locale.FR_FR]: defaultShortDatetimeFormat,
  [Locale.IT_IT]: defaultShortDatetimeFormat,
  [Locale.KO_KR]: defaultShortDatetimeFormat,
  [Locale.LV_LV]: defaultShortDatetimeFormat,
  [Locale.NL_NL]: defaultShortDatetimeFormat,
  [Locale.PT_BR]: defaultShortDatetimeFormat,
  [Locale.PT_PT]: defaultShortDatetimeFormat,
  [Locale.RU_RU]: defaultShortDatetimeFormat,
  [Locale.TR_TR]: defaultShortDatetimeFormat,
  [Locale.UK_UA]: defaultShortDatetimeFormat,
  [Locale.ZH_TW]: defaultShortDatetimeFormat,
};

const defaultNumberFormat = {
  percent: {
    style: 'percent',
    useGrouping: false
  }
} as const;

export const numberFormats: I18nOptions['numberFormats'] = {
  [Locale.EN_US]: defaultNumberFormat,
  [Locale.DE_DE]: defaultNumberFormat,
  [Locale.FR_FR]: defaultNumberFormat,
  [Locale.IT_IT]: defaultNumberFormat,
  [Locale.KO_KR]: defaultNumberFormat,
  [Locale.LV_LV]: defaultNumberFormat,
  [Locale.NL_NL]: defaultNumberFormat,
  [Locale.PT_BR]: defaultNumberFormat,
  [Locale.PT_PT]: defaultNumberFormat,
  [Locale.RU_RU]: defaultNumberFormat,
  [Locale.TR_TR]: defaultNumberFormat,
  [Locale.UK_UA]: defaultNumberFormat,
  [Locale.ZH_TW]: defaultNumberFormat
};

export const mapToLocale = (local: string): Locale =>
  (Object.values(Locale) as string[]).includes(local)
    ? (local as Locale)
    : Locale.EN_US;

const i18n = createI18n({
  locale: navigator.language,
  fallbackLocale: Locale.EN_US,
  messages,
  datetimeFormats,
  numberFormats,
  globalInjection: true,
  missingWarn: false,
  fallbackWarn: false,
  legacy: false
});

export default i18n;
