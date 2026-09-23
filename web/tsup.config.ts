import { defineConfig } from 'tsup'

export default defineConfig({
  entry: ['src/cli/index.ts'],
  outDir: 'dist-cli',
  format: 'esm',
  target: 'node18',
  sourcemap: true,
  clean: true,
  banner: {
    js: '#!/usr/bin/env node',
  },
  // Bundle every dependency (express, commander) into the CLI so it runs
  // standalone from the APK assets without requiring a node_modules tree.
  noExternal: ['express', 'commander'],
})
