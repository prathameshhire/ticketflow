const fs = require('fs');
const path = require('path');

const isVercelBuild = Boolean(process.env.VERCEL);

if (isVercelBuild && !process.env.NG_APP_API_URL) {
  throw new Error('NG_APP_API_URL is required for Vercel builds.');
}

const apiBaseUrl = process.env.NG_APP_API_URL || 'http://localhost:8080/api';
const assetsDir = path.join(__dirname, '..', 'src', 'assets');
const envFile = path.join(assetsDir, 'env.js');

fs.mkdirSync(assetsDir, { recursive: true });

const contents = `window.__env = Object.freeze({
  API_BASE_URL: ${JSON.stringify(apiBaseUrl)}
});
`;

fs.writeFileSync(envFile, contents, 'utf8');
console.log(`Generated src/assets/env.js with API_BASE_URL=${apiBaseUrl}`);
