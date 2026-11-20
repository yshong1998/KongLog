import type { Metadata } from "next";
import "@fontsource/jua/400.css";
import "@fontsource/ibm-plex-sans-kr/400.css";
import "@fontsource/ibm-plex-sans-kr/600.css";
import "@fontsource/ibm-plex-sans-kr/700.css";
import "@fontsource/jetbrains-mono/400.css";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "KongLog",
    template: "%s | KongLog",
  },
  description: "개발하다 만난 문제를 콩알만큼씩 적어 둬요.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className="h-full antialiased">
      <body className="flex min-h-full flex-col bg-bg font-sans text-body text-ink">
        {children}
      </body>
    </html>
  );
}
