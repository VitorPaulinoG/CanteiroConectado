/* global __dirname */
const fs = require('node:fs');
const path = require('node:path');
const expoConfig = require('eslint-config-expo/flat');

const FEATURE_DIR = path.join(__dirname, 'src', 'feature');
const features = fs
  .readdirSync(FEATURE_DIR, { withFileTypes: true })
  .filter((entry) => entry.isDirectory())
  .map((entry) => entry.name);

// Uma feature só pode importar de `shared` (e de si mesma): imports para outra feature são proibidos,
// tanto pelo alias `@/feature/<outra>` quanto por caminho relativo (`../<outra>`).
const featureBoundaries = features.map((feature) => {
  const others = features.filter((other) => other !== feature);
  const patterns = others.flatMap((other) => [
    `@/feature/${other}`,
    `@/feature/${other}/**`,
    ...[1, 2, 3, 4, 5].flatMap((depth) => {
      const up = '../'.repeat(depth);
      return [`${up}${other}`, `${up}${other}/**`];
    }),
  ]);

  return {
    files: [`src/feature/${feature}/**/*.{ts,tsx}`],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: patterns,
              message: 'Uma feature não pode importar de outra feature. Mova o código comum para `@/shared`.',
            },
          ],
        },
      ],
    },
  };
});

module.exports = [
  ...expoConfig,
  {
    ignores: ['dist/*'],
  },
  ...featureBoundaries,
];
