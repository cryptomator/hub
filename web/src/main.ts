import './style.css';
import { Vault } from './vault';

const vault = Vault.create();
const vaultId = 'foobar';
const hubUrl = 'hub+' + location.protocol + '//' + location.hostname + ':' + location.port + '/vault/' + vaultId

vault.createVaultConfig(vaultId, hubUrl).then(token => {
  const div = document.querySelector<HTMLDivElement>('#jwt')!
  div.innerHTML = `<b>jwt</b>: <code>${token}</code>`
});

vault.encryptMasterkey("foobar").then(masterkey => {
  const div = document.querySelector<HTMLDivElement>('#key')!
  div.innerHTML = `<b>key</b>: ${masterkey.encrypted}<br>
    <b>salt</b>: ${masterkey.salt}`
})

// vault.generateMasterkey().then(mk => {
//   crypto.subtle.exportKey('raw', mk).then(raw => {
//     console.log(vault.base64Url(raw));
//   });
//   vault.createVaultConfig("foobar", mk).then(token => {
//     console.log(token);
//     app.innerHTML = `
//       <h1>Hello Vite!</h1>
//       <a href="https://vitejs.dev/guide/features.html" target="_blank">Documentation</a>
//       ${token}`
//   });
// });
