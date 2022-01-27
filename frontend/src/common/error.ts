export class BackendError extends Error {
  constructor(msg: string) {
    super(msg);
  }
}

export class NotAuthorizedError extends BackendError {
  constructor() {
    super('Not authorized to access resource');
  }
}

export class NotFoundError extends BackendError {
  constructor() {
    super('Requested resource not found');
  }
}

export class AlreadyExistsError extends BackendError {
  constructor() {
    super('Resource already exists');
  }
}

