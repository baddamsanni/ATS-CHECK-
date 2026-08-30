/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Proxy /api/* to the Spring backend during dev so the browser calls same-origin.
  async rewrites() {
    const backend = process.env.BACKEND_URL || "http://localhost:8081";
    return [
      {
        source: "/api/:path*",
        destination: `${backend}/api/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
