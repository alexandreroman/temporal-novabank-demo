// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  // The Temporal SDK and protobufjs packages are CommonJS with no `exports`
  // map and use extensionless subpath imports (e.g. @temporalio/common/lib/
  // errors), which Node's strict ESM resolver rejects in the production
  // server build (`node .output/server/index.mjs`). They must be inlined as a
  // consistent set so the bundler resolves subpaths and keeps CJS interop
  // intact: @temporalio/common <-> @temporalio/proto share a namespace, and
  // protobufjs's `util.pool` breaks if its @protobufjs/* helpers are left
  // external. @grpc/grpc-js and long stay external (resolvable root imports).
  nitro: {
    externals: {
      inline: [
        '@temporalio/client',
        '@temporalio/common',
        '@temporalio/proto',
        '@temporalio/activity',
        '@temporalio/envconfig',
        'protobufjs',
        '@protobufjs/aspromise',
        '@protobufjs/base64',
        '@protobufjs/codegen',
        '@protobufjs/eventemitter',
        '@protobufjs/fetch',
        '@protobufjs/float',
        '@protobufjs/path',
        '@protobufjs/pool',
        '@protobufjs/utf8'
      ]
    }
  },
  devtools: { enabled: true },
  devServer: {
    port: 3000
  },
  css: ['~/assets/css/main.css'],
  app: {
    head: {
      title: 'NovaBank — Open Your Account',
      link: [
        {
          rel: 'preconnect',
          href: 'https://fonts.googleapis.com'
        },
        {
          rel: 'preconnect',
          href: 'https://fonts.gstatic.com',
          crossorigin: ''
        },
        {
          rel: 'stylesheet',
          href: 'https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Playfair+Display:wght@400;600;700&display=swap'
        }
      ]
    }
  }
})
