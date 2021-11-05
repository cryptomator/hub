import { expect } from 'chai';
import { describe } from 'mocha';
import { uuid } from '../../src/common/util';

describe('uuid', () => {

  it('should create distinct uuid', () => {
    const uuid1 = uuid();
    const uuid2 = uuid();
    expect(uuid1).not.to.equal(uuid2);
  });

});
