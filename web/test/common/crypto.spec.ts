import { Crypto } from '@peculiar/webcrypto';
import { expect } from 'chai';
import { describe } from 'mocha';
import { Masterkey, WrappedMasterkey } from '../../src/common/crypto';

describe('Masterkey', () => {

  before(done => {
    // since this test runs on Node, we need to replace window.crypto:
    // @ts-ignore: global not defined (but will be available within Node)
    global.crypto = new Crypto();
    done()
  })

  it('create', async () => {
    const orig = await Masterkey.create();

    expect(orig).to.be.not.null
  })

  it('create and wrap', async () => {
    const orig = await Masterkey.create();
    const wrapped = await orig.wrap("pass");

    expect(wrapped).to.be.not.null
  })

  describe('from wrapped', () => {
    let wrapped: WrappedMasterkey;

    beforeEach(async () => {
      wrapped = await (await Masterkey.create()).wrap("pass");
    })

    it('unwrap with wrong pw', async () => {
      return Masterkey.unwrap("wrong", wrapped)
        .then(k => {
          expect.fail('should not succeed');
        })
        .catch(e => {
          expect(e).to.have.property('message').that.does.not.contain('should not succeed');
        });
    })

  })


})
