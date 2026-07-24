const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = {
  '/api': {
    target: 'http://localhost:8080',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
    ws: true,
    onProxyReq: (proxyReq, req, res) => {
      // Explicitly forward Authorization header
      if (req.headers.authorization) {
        proxyReq.setHeader('Authorization', req.headers.authorization);
      }
      // Set X-Forwarded-For to localhost for Thor auth check
      proxyReq.setHeader('X-Forwarded-For', '127.0.0.1');
    }
  }
};
