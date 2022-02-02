
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

export class FrontendError extends Error {

  constructor(msg: string) {
    super(msg);
  }
}

export class WrongPasswordError extends FrontendError {

  constructor() {
    super('Password is incorrect');
  }
}
