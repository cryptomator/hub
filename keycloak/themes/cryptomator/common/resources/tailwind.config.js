module.exports = {
  content: [
    '../../login/*.html',
    '../../login/theme.properties',
  ],
  theme: {
    extend: {
      colors: {
        current: 'currentColor',
        'primary': '#49B04A',
        'primary2': '#009F69',
        'primary-l1': '#66CC68',
        'primary-l2': '#EBF5EB',
        'primary-d1': '#407F41',
        'primary-d2': '#2D4D2E',
        'secondary': '#008A7B',
        'secondary2': '#00747E',
        'tertiary': '#005E71',
        'tertiary2': '#2F4858',
        'dark': '#1F2122',
        'gray_0': '#222222',
        'gray_1': '#3B3B3B',
        'gray_2': '#515151',
        'gray_3': '#626262',
        'gray_4': '#7E7E7E',
        'gray_5': '#9E9E9E',
        'gray_6': '#B1B1B1',
        'gray_7': '#CFCFCF',
        'gray_8': '#E1E1E1',
        'gray_9': '#F7F7F7',
        'sky-dark': '#0b0f17',
        'sky-medium': '#3f8bd9',
        'sky-light': '#9ac7f5',
      },
      backgroundImage: {
        'hills': "url('../img/header-background.png')",
        'cryptobot': "url('../img/logo.svg')",
      }
    },
    fontFamily: {
      'headline': 'Quicksand, sans-serif',
      'body': 'Open Sans, sans-serif',
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
}
