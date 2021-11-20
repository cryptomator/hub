module.exports = {
  mode: 'jit',
  purge: [
    './public/**/*.html',
    './src/**/*.{ts,vue}',
  ],
  darkMode: false, // or 'media' or 'class'
  theme: {
    extend: {
      colors: {
        'primary': '#49B04A',
        'primary-l1': '#66CC68',
        'primary-l2': '#EBF5EB',
        'primary-d1': '#407F41',
        'primary-d2': '#2D4D2E',
        'secondary': '#008A7B',
        'tertiary': '#005E71',
        'dark': '#1E2B33',
      },
    },
    fontFamily: {
      logo: ['Quicksand', 'bold']
    },
    variants: {
      extend: {},
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
};
