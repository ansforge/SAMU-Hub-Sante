# LRM Client UI

## Development Setup

- Copy the `.env.template` and rename it `.env`:

```bash
cp .env.template .env
```

- *Optional:* Update the content of the `.env` file:
  - `NUXT_PUBLIC_BACKEND_LRM_SERVER` controls the backend instance the app will connect to. Use `<environment>.hub.esante.gouv.fr` to connect to a specific environment (or `hub.esante.gouv.fr` for production).
  - `NUXT_PUBLIC_MODEL_BRANCH` sets the base ref in the dropdown when fetching the model examples.
  - `NUXT_PUBLIC_VHOST_MAP` controls the available vhost and their model version. Format is vhost as key and model version as value.
  - `NUXT_PUBLIC_CLIENT_MAP` controls what's available in the landing page dropdown (which clients can connect to one another). Format is array of arrays with first a given clientId and in second an array with the associated clientIds => [[samuA, [samuB, samuC]], ...]
- Install dependencies

```bash
npm install
```

- Serve with hot reload at [localhost:3000](http://localhost:3000)

```bash
npm run dev
```

### Tests

TODO: implement functional tests at the component level and unit tests for basic logic

Application tests are implemented using Cypress (cf [cypress/e2e](./cypress/e2e/) directory).

To run the tests locally:

- Build the Vue app: `npm run build:test`. *Note: the build:test variant script is used to instrument the app in order to generate coverage report.*
- Start the app: `npm run start`
- Run the tests: `npm run test:e2e`
- Display coverage report: `npm run test:e2e:report`

Optional: To run the test in debug mode within the Cypress UI, use `npx cypress open`.

## Production

```bash
# build for production and launch server
$ npm run build
$ npm run start

# generate static project
$ npm run generate
```

For detailed explanation on how things work, check out the [documentation](https://nuxtjs.org).

## Special Directories

You can create the following extra directories, some of which have special behaviors. Only `pages` is required; you can delete them if you don't want to use their functionality.

### `assets`

The assets directory contains your uncompiled assets such as Stylus or Sass files, images, or fonts.

More information about the usage of this directory in [the documentation](https://nuxtjs.org/docs/2.x/directory-structure/assets).

### `components`

The components directory contains your Vue.js components. Components make up the different parts of your page and can be reused and imported into your pages, layouts and even other components.

More information about the usage of this directory in [the documentation](https://nuxtjs.org/docs/2.x/directory-structure/components).

### `layouts`

Layouts are a great help when you want to change the look and feel of your Nuxt app, whether you want to include a sidebar or have distinct layouts for mobile and desktop.

More information about the usage of this directory in [the documentation](https://nuxtjs.org/docs/2.x/directory-structure/layouts).


### `pages`

This directory contains your application views and routes. Nuxt will read all the `*.vue` files inside this directory and setup Vue Router automatically.

More information about the usage of this directory in [the documentation](https://nuxtjs.org/docs/2.x/get-started/routing).

### `plugins`

The plugins directory contains JavaScript plugins that you want to run before instantiating the root Vue.js Application. This is the place to add Vue plugins and to inject functions or constants. Every time you need to use `Vue.use()`, you should create a file in `plugins/` and add its path to plugins in `nuxt.config.js`.

More information about the usage of this directory in [the documentation](https://nuxtjs.org/docs/2.x/directory-structure/plugins).

### `static`

This directory contains your static files. Each file inside this directory is mapped to `/`.

Example: `/static/robots.txt` is mapped as `/robots.txt`.

More information about the usage of this directory in [the documentation](https://nuxtjs.org/docs/2.x/directory-structure/static).

### `store`

This directory contains your Vuex store files. Creating a file in this directory automatically activates Vuex.

More information about the usage of this directory in [the documentation](https://nuxtjs.org/docs/2.x/directory-structure/store).

## Standards

### NPM cript naming

We encourage to follow the [scriptlint standard](https://github.com/peerigon/scriptlint/wiki/The-scriptlint-%22standard%22-tl%3Bdr).

When adding new scripts, make sure:

- to use camelCase
- to abstract script name from their implementation (ex: `lint:css` instead of `stylint`)
- to use namespaces to categorize scripts (ex: `test:e2e`, and all the test related scripts under the `test` namespace)
- to use `:` as a namespace separator (ex: `lint:js`)

More details are available on the link above.
