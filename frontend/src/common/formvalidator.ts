import i18n from '../i18n';

const { t } = i18n.global;

export interface ValidationResult {
  valid: boolean;
  errors: Record<string, string>;
}

export class FormValidator {

  /**
   * Validates user form data
   */
  static validateUser(data: {
    firstName?: string,
    lastName?: string,
    username: string,
    email?: string,
    password: string,
    passwordConfirm: string,
    isEditMode: boolean,
    initialEmail?: string,
    pictureUrl?: string,
    isValidImageUrl?: boolean
  }): ValidationResult {
    const errors: Record<string, string> = {};

    // Required fields validation
    if (!data.firstName?.trim()) errors.firstName = t('userEditCreate.validation.required');
    if (!data.lastName?.trim()) errors.lastName = t('userEditCreate.validation.required');
    if (!data.username.trim()) errors.username = t('userEditCreate.validation.required');

    // Email validation
    const emailTrimmed = data.email?.trim() ?? '';
    const emailRequired = this.isEmailRequired(data.isEditMode, data.initialEmail);

    if (emailRequired && !emailTrimmed) {
      errors.email = t('userEditCreate.validation.required');
    } else if (emailTrimmed && !this.isValidEmail(emailTrimmed)) {
      errors.email = t('userEditCreate.invalidEmail');
    }

    // Password confirmation validation
    if (data.password && data.password !== data.passwordConfirm) {
      errors.passwordConfirm = t('userEditCreate.validation.passwordMismatch');
    }

    // For create mode, require password
    if (!data.isEditMode && !data.password) {
      errors.password = t('userEditCreate.validation.required');
    }

    // Validate picture URL
    const pictureUrlError = this.validatePictureUrl(data.pictureUrl, data.isValidImageUrl, 'userEditCreate.invalidImageUrl');
    if (pictureUrlError) errors.pictureUrl = pictureUrlError;

    return {
      valid: Object.keys(errors).length === 0,
      errors
    };
  }

  /**
   * Validates group form data
   */
  static validateGroup(data: {
    name: string,
    pictureUrl?: string,
    isValidImageUrl?: boolean
  }): ValidationResult {
    const errors: Record<string, string> = {};

    // Required field validation
    if (!data.name.trim()) errors.name = t('groupEditCreate.validation.required');

    // Validate picture URL
    const pictureUrlError = this.validatePictureUrl(data.pictureUrl, data.isValidImageUrl, 'groupEditCreate.invalidImageUrl');
    if (pictureUrlError) errors.pictureUrl = pictureUrlError;

    return {
      valid: Object.keys(errors).length === 0,
      errors
    };
  }

  /**
   * Common validation for picture URLs
   * @private
   */
  private static validatePictureUrl(pictureUrl: string | undefined, isValidImageUrl: boolean | undefined, errorKey: string): string | undefined {
    if (pictureUrl && !isValidImageUrl) {
      return t(errorKey);
    }
    return undefined;
  }

  /**
   * Validates email format
   */
  static isValidEmail(email?: string): boolean {
    if (!email) return false;
    return /^[^\s@]+@[^\s@]+\.[a-z]{2,}$/i.test(email.trim());
  }

  /**
   * Checks if email is required
   */
  static isEmailRequired(isEditMode: boolean, initialEmail?: string): boolean {
    if (isEditMode) {
      // EDIT: required only if user previously had email
      return initialEmail !== undefined && initialEmail.trim() !== '';
    } else {
      // CREATE: always required
      return true;
    }
  }

  /**
   * Evaluates password strength
   */
  static evaluatePasswordStrength(password: string): 'weak' | 'medium' | 'strong' | '' {
    if (!password) return '';

    const strong = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{10,}$/;
    const medium = /^(?=.*[a-zA-Z])(?=.*\d).{6,}$/;

    if (strong.test(password)) return 'strong';
    if (medium.test(password)) return 'medium';
    return 'weak';
  }

  /**
   * Validates image URL by attempting to load it
   */
  static validateImageUrl(url?: string, timeoutMs: number = 1000): Promise<boolean> {
    if (!url) {
      return Promise.resolve(false);
    }
    return new Promise((resolve) => {
      const timeout = setTimeout(() => {
        resolve(false);
      }, timeoutMs);
      const img = new Image();
      img.onload = () => {
        clearTimeout(timeout);
        resolve(true);
      };
      img.onerror = () => {
        clearTimeout(timeout);
        resolve(false);
      };
      img.src = url;
    });
  }

}
