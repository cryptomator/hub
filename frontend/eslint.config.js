// @ts-check
import eslint from "@eslint/js";
import pluginVue from 'eslint-plugin-vue';
import stylistic from '@stylistic/eslint-plugin';
import tseslint from 'typescript-eslint';
import { defineConfig } from 'eslint/config';

const commonRules = {
    '@typescript-eslint/ban-ts-comment': 'warn',
    '@typescript-eslint/no-explicit-any': 'warn',
    '@typescript-eslint/no-unsafe-function-type': 'warn',
    '@typescript-eslint/no-unused-vars': 'off', // is checked by noUnusedLocals in tsconfig.json // or use ['error', { 'ignoreRestSiblings': true }]
    'no-unused-vars': 'off', // is checked by noUnusedLocals in tsconfig.json
    'no-undef': 'off', // types checked by typescript already
    'complexity': ['warn', { max: 10 }],
    '@stylistic/keyword-spacing': ['error', { before: true, after: true}],
    '@stylistic/linebreak-style': ['error', 'unix'],
    '@stylistic/lines-between-class-members': ['error', 'always', { exceptAfterSingleLine: true, }],
    '@stylistic/no-multiple-empty-lines': ['error', { max: 1, maxEOF: 0, maxBOF: 0 }],
    '@stylistic/object-curly-spacing': ['error', 'always'],
    '@stylistic/padded-blocks': ['error', { blocks: 'never', classes: 'always', switches: 'never' }],
    '@stylistic/quotes': ['error', 'single'],
    '@stylistic/semi': ['error', 'always'],
    '@stylistic/space-infix-ops': 'error',
    '@stylistic/operator-linebreak': ['error', 'before'],
    '@stylistic/indent': ['error', 2, { SwitchCase: 1 }],
    '@stylistic/template-curly-spacing': ['error', 'never'],
    '@stylistic/rest-spread-spacing': ['error', 'never'],
};

export default defineConfig(
{
    ignores: [
        'coverage/**',
        'dist/**',
        'node_modules/**'
    ]
},
{
    files: ['src/**/*.ts', 'test/**/*.ts'],
    extends: [
        eslint.configs.recommended,
        tseslint.configs.recommended,
    ],
    plugins: {
        '@stylistic': stylistic,
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
        ...commonRules,
    }
},
{
    files: ['test/**/*.ts'],
    rules: {
        ...commonRules,
        '@typescript-eslint/no-unused-expressions': 'off'
    }
},
{
    files: ['src/**/*.vue'],
    extends: [
        eslint.configs.recommended,
        tseslint.configs.recommended,
        pluginVue.configs["flat/recommended"],
    ],
    plugins: {
        '@stylistic': stylistic,
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
        ...commonRules,
        'vue/max-attributes-per-line': ['error', {
            singleline: {
                max: 10
            },
            multiline: {
                max: 1
            }
        }],
        'vue/html-closing-bracket-spacing': ['warn'],
        'vue/html-self-closing': ['error', {
            html: {
                void: 'always',
                normal: 'any',
                component: 'always'
            }
        }],
        'vue/singleline-html-element-content-newline': ['off'],
        'vue/multi-word-component-names': ['warn'],
    },
}
);