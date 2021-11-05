import de from './de-DE.json';
import en from './en-US.json';
import { Locales } from './locales';

export const messages = {
  [Locales.EN]: en,
  [Locales.DE]: de
};

export const defaultLocale = Locales.EN;
