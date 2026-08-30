import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "RESUME CERTIFIED>",
  description:
    "Upload your resume and a job description to get an ATS-style fit score and a downloadable Fit Certificate PDF.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
