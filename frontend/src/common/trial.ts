import AxiosStatic from 'axios';

/**
 * Client for the public Cryptomator license API, used to obtain a trial license directly from the browser.
 * This way trial retrieval also works when the Hub server itself has no outbound internet access.
 * The required Altcha proof of work is solved by the {@code <altcha-widget>}, whose payload is passed as captcha.
 */

export type TrialLicense = {
  hubId: string;
  licenseKey: string;
};

export async function requestTrialLicense(licenseApiUrl: string, captcha: string): Promise<TrialLicense> {
  const axios = AxiosStatic.create({ baseURL: licenseApiUrl, timeout: 15000 }); // unlike same-origin backend calls, this crosses the internet — fail fast instead of spinning forever
  const params = new URLSearchParams({ captcha: captcha });
  return axios.post<TrialLicense>('/hub/trial', params).then(response => response.data);
}
