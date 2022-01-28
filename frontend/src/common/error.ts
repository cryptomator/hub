
export class BackendError extends Error {
  constructor(msg: string) {
    super(msg);
  }
}

export class ForbiddenError extends BackendError {
  constructor() {
    super('Not authorized to access resource');
  }
}

export class NotFoundError extends BackendError {
  constructor() {
    super('Requested resource not found');
  }
}

export class ConflictError extends BackendError {
  constructor() {
    super('Resource already exists');
  }
}

export class StrangeError extends BackendError {

  readonly thrownObject: any;
  constructor(thrownObject: any) {
    super('Unidentified error thrown');
    this.thrownObject = thrownObject;
  }
}

