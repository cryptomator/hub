module.exports = {
  plugins: ['@typescript-eslint'],
  parserOptions: {
    parser: require.resolve('@typescript-eslint/parser'),
    extraFileExtensions: ['.vue'],
    ecmaFeatures: {
      jsx: true
    }
  },
  env: {
    'browser': true
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
  ],
  globals: {
    defineProps: 'readonly',
    defineEmits: 'readonly',
    defineExpose: 'readonly',
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
    'keyword-spacing': ['error', { 'before': true, 'after': true }],
    'linebreak-style': ['error', 'unix'],
    'no-unused-vars': 'off',
    'object-curly-spacing': ['error', 'always'],
    'quotes': ['error', 'single'],
    'semi': ['error', 'always'],
    'space-infix-ops': ['error'],
    '@typescript-eslint/indent': ['error', 2],
    '@typescript-eslint/no-unused-vars': 'error',
    'no-undef': 'off' // types checked by typescript already
  }
};
