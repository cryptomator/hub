module.exports = {
    plugins: ['@typescript-eslint'],
    parserOptions: {
      parser: require.resolve('@typescript-eslint/parser'),
      extraFileExtensions: ['.vue'],
      ecmaFeatures: {
        jsx: true
      }
    },
    extends: [
      'plugin:vue/vue3-recommended'
    ],
    overrides: [{
      files: ['*.ts', '*.tsx'],
      rules: {
        'no-unused-vars': 'off',
      }
    }],
    overrides: [{
      files: ['*.vue'],
      rules: {
        'vue/max-attributes-per-line': 'off',
        'vue/singleline-html-element-content-newline': 'off',
      }
    }]
  }