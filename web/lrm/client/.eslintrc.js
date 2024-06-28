module.exports = {
  root: true,
  env: {
    browser: true,
    node: true
  },
  extends: [
    '@nuxtjs/eslint-config-typescript',
    'plugin:nuxt/recommended',
    'plugin:vue/base',
    'plugin:vuetify/base'
  ],
  plugins: [
  ],
  // add your custom rules here
  rules: {}
}
