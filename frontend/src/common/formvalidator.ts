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
    firstName: string,
    lastName: string,
    username: string,
    email: string,
    password: string,
    passwordConfirm: string,
    isEditMode: boolean
  }): ValidationResult {
    const errors: Record<string, string> = {};

    // Required fields validation
    if (!data.firstName.trim()) errors.firstName = t('userEditCreate.validation.required');
    if (!data.lastName.trim()) errors.lastName = t('userEditCreate.validation.required');
    if (!data.username.trim()) errors.username = t('userEditCreate.validation.required');

    // Email validation
    if (!data.email.trim()) {
      errors.email = t('userEditCreate.validation.required');
    } else if (!this.isValidEmail(data.email.trim())) {
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

    return {
      valid: Object.keys(errors).length === 0,
      errors
    };
  }

  /**
   * Validates group form data
   */
  static validateGroup(data: {
    name: string
  }): ValidationResult {
    const errors: Record<string, string> = {};

    if (!data.name.trim()) {
      errors.name = t('group.edit.validation.nameRequired');
    }

    return {
      valid: Object.keys(errors).length === 0,
      errors
    };
  }

  /**
   * Validates email format
   */
  static isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email.trim());
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
}
