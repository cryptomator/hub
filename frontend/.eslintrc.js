// eslint-disable-next-line no-undef
module.exports = {
  plugins: ['@typescript-eslint'],
  parserOptions: {
    // eslint-disable-next-line no-undef
    parser: require.resolve('@typescript-eslint/parser'),
    extraFileExtensions: ['.vue'],
    ecmaFeatures: {
      jsx: true
    }
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
  ],
  globals: {
    defineProps: 'readonly',
    defineEmits: 'readonly'
  },
  overrides: [
    {
      files: ['*.vue'],
      rules: {
        'vue/max-attributes-per-line': 'off',
        'vue/singleline-html-element-content-newline': 'off',
        'vue/html-closing-bracket-spacing': 'off',
        'vue/html-self-closing': 'off',
      }
    },
  ],
  rules: {
    'indent': ['error', 2],
    'linebreak-style': ['error', 'unix'],
    'no-unused-vars': 'off',
    'object-curly-spacing': ['error', 'always'],
    'quotes': ['error', 'single'],
    'semi': ['error', 'always'],
    '@typescript-eslint/no-unused-vars': 'error',
  }
};
