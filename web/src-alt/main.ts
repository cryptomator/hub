import { Spi } from './spi';
import './style.css';
import { Vault } from './vault';

const vault = Vault.create();
const vaultId = 'foobar';
const hubUrl = 'hub+' + location.protocol + '//' + location.hostname + ':' + location.port + '/vault/' + vaultId

vault.createVaultConfig(vaultId, hubUrl).then(token => {
  const div = document.querySelector<HTMLDivElement>('#jwt')!
  div.innerHTML = `<b>jwt</b>: <code>${token}</code>`
});

const spi = new Spi()
vault.encryptMasterkey("foobar").then(masterkey => {
  spi.createVault(uuid(), "name1", masterkey.encrypted, masterkey.iterations, masterkey.salt)
})

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}
