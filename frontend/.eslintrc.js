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
        'vue/block-tag-newline': ['error', { 'singleline': 'consistent', 'multiline': 'consistent' }],
        'vue/html-closing-bracket-spacing': 'off',
        'vue/html-self-closing': 'off',
        'vue/max-attributes-per-line': 'off',
        'vue/padding-line-between-blocks': 'error',
        'vue/singleline-html-element-content-newline': 'off',
        'vue/space-infix-ops': 'error',
      }
    },
  ],
  rules: {
    'keyword-spacing': ['error', { 'before': true, 'after': true }],
    'linebreak-style': ['error', 'unix'],
    'lines-between-class-members': ['error', 'always', { 'exceptAfterSingleLine': true }],
    'no-multiple-empty-lines': ['error', { 'max': 1, 'maxEOF': 0, 'maxBOF': 0 }],
    'no-unused-vars': 'off',
    'object-curly-spacing': ['error', 'always'],
    'padded-blocks': ['error', 'never'],
    'quotes': ['error', 'single'],
    'semi': ['error', 'always'],
    'space-infix-ops': 'error',
    '@typescript-eslint/indent': ['error', 2],
    '@typescript-eslint/no-unused-vars': 'error',
    'no-undef': 'off' // types checked by typescript already
  }
};
