import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { SiteHeader } from "./site-header";

describe("SiteHeader", () => {
  const html = renderToStaticMarkup(<SiteHeader />);

  it("블로그 이름은 홈으로 가는 링크다", () => {
    expect(html).toMatch(/<a[^>]*href="\/"[^>]*>.*KongLog.*?<\/a>/);
  });

  it("주 메뉴에 글과 소개 링크가 있다", () => {
    expect(html).toMatch(/<a[^>]*href="\/posts"[^>]*>글<\/a>/);
    expect(html).toMatch(/<a[^>]*href="\/about"[^>]*>소개<\/a>/);
  });

  it("메뉴 버튼은 처음에 접힌 상태이고 자기가 여닫는 메뉴를 가리킨다", () => {
    const button = html.match(/<button[^>]*>/)?.[0] ?? "";
    expect(button).toContain('aria-label="메뉴"');
    expect(button).toContain('aria-expanded="false"');

    const controls = button.match(/aria-controls="([^"]+)"/)?.[1];
    expect(controls).toBeTruthy();
    expect(html).toContain(`<nav id="${controls}"`);
  });
});
