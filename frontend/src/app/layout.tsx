import type { Metadata } from "next";
import "@fontsource/jua/400.css";
import "@fontsource/ibm-plex-sans-kr/400.css";
import "@fontsource/ibm-plex-sans-kr/600.css";
import "@fontsource/ibm-plex-sans-kr/700.css";
import "@fontsource/jetbrains-mono/400.css";
import "./globals.css";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";
import { SITE_NAME } from "@/lib/site";

export const metadata: Metadata = {
  title: {
    default: SITE_NAME,
    template: `%s | ${SITE_NAME}`,
  },
  description: "개발하다 만난 문제를 콩알만큼씩 적어 둬요.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className="h-full antialiased">
      <body className="flex min-h-full flex-col bg-bg font-sans text-body text-ink">
        <SiteHeader />
        <main className="mx-auto w-full max-w-wide flex-1 px-4">
          {children}
        </main>
        <SiteFooter />
      </body>
    </html>
  );
}
