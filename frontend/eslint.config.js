// @ts-check
import eslint from "@eslint/js";
import pluginVue from 'eslint-plugin-vue';
import tseslint from 'typescript-eslint';

export default tseslint.config(
{
    files: ['src/**/*.ts', 'test/**/*.ts'],
    extends: [
        eslint.configs.recommended,
        ...tseslint.configs.recommended,
    ],
    plugins: {
        '@typescript-eslint': tseslint.plugin,
    },
    languageOptions: {
        parser: tseslint.parser,
        parserOptions: {
            projectService: true,
            tsconfigRootDir: import.meta.dirname,
            ecmaVersion: 'latest',
            sourceType: 'module',
        },
    },
    rules: {
        '@typescript-eslint/ban-ts-comment': 'warn',
        '@typescript-eslint/no-explicit-any': 'warn',
        '@typescript-eslint/no-unsafe-function-type': 'warn',
        '@typescript-eslint/no-unused-vars': 'off', // is checked by noUnusedLocals in tsconfig.json // or use ['error', { 'ignoreRestSiblings': true }]
        'no-unused-vars': 'off', // is checked by noUnusedLocals in tsconfig.json
        'no-undef': 'off', // types checked by typescript already
        'keyword-spacing': ['error', { before: true, after: true}],
        'linebreak-style': ['error', 'unix'],
        'lines-between-class-members': ['error', 'always', { exceptAfterSingleLine: true, }],
        'no-multiple-empty-lines': ['error', { max: 1, maxEOF: 0, maxBOF: 0 }],
        'object-curly-spacing': ['error', 'always'],
        'padded-blocks': ['error', 'never'],
        'quotes': ['error', 'single'],
        'semi': ['error', 'always'],
        'space-infix-ops': 'error',
        'indent': ['error', 2, { SwitchCase: 1 }],
    }
},
{
    files: ['test/**/*.ts'],
    rules: {
        '@typescript-eslint/no-unused-expressions': 'off'
    }
},
{
    files: ['src/**/*.vue'],
    extends: [
        eslint.configs.recommended,
        ...tseslint.configs.recommended,
        ...pluginVue.configs["flat/recommended"],
    ],
    plugins: {
        '@typescript-eslint': tseslint.plugin,
    },
    languageOptions: {
        parserOptions: {
            parser: tseslint.parser,
            ecmaVersion: 'latest',
            sourceType: 'module',
            extraFileExtensions: ['.vue'],
            ecmaFeatures: {
                jsx: true
            }
        },
    },
    rules: {
        '@typescript-eslint/ban-ts-comment': 'warn',
        '@typescript-eslint/no-explicit-any': 'warn',
        '@typescript-eslint/no-unsafe-function-type': 'warn',
        '@typescript-eslint/no-unused-vars': 'off', // is checked by noUnusedLocals in tsconfig.json // or use ['error', { 'ignoreRestSiblings': true }]
        'no-unused-vars': 'off', // is checked by noUnusedLocals in tsconfig.json
        'no-undef': 'off', // types checked by typescript already
        'keyword-spacing': ['error', { before: true, after: true}],
        'linebreak-style': ['error', 'unix'],
        'lines-between-class-members': ['error', 'always', { exceptAfterSingleLine: true, }],
        'no-multiple-empty-lines': ['error', { max: 1, maxEOF: 0, maxBOF: 0 }],
        'object-curly-spacing': ['error', 'always'],
        'padded-blocks': ['error', 'never'],
        'quotes': ['error', 'single'],
        'semi': ['error', 'always'],
        'space-infix-ops': 'error',
        'indent': ['error', 2, { SwitchCase: 1 }],
        'vue/block-tag-newline': ['error', { singleline: 'consistent', multiline: 'consistent', }],
        'vue/html-closing-bracket-spacing': 'off',
        'vue/html-self-closing': 'off',
        'vue/max-attributes-per-line': 'off',
        'vue/padding-line-between-blocks': 'error',
        'vue/singleline-html-element-content-newline': 'off',
        'vue/space-infix-ops': 'error',
    },
}
);