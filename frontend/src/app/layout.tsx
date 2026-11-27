import type { Metadata } from "next";
import { connection } from "next/server";
import "./globals.css";

export const metadata: Metadata = {
  title: "ReviewHop",
  description: "Wiederhole gelöste Lernaufgaben zum richtigen Zeitpunkt.",
};

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  await connection();
  return (
    <html lang="de">
      <body>{children}</body>
    </html>
  );
}
