module.exports = {
  purge: [],
  darkMode: false, // or 'media' or 'class'
  theme: {
    extend: {
      colors: {
        'primary': '#49B04A',
        'primary-l2': '#EBF5EB',
        'secondary': '#008A7B',
        'tertiary': '#005E71',
        'dark': '#1E2B33',
      },
    },
  },
  variants: {
    extend: {},
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
};
