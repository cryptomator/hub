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

export const defaultLocale = Locale.EN;
