import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { SiteFooter } from "./site-footer";

describe("SiteFooter", () => {
  const html = renderToStaticMarkup(<SiteFooter />);

  it("저작권 표기가 있다", () => {
    expect(html).toContain("© 2026 KongLog");
  });

  it("GitHub으로 가는 링크가 있다", () => {
    expect(html).toMatch(/<a[^>]*href="https:\/\/github\.com\/[^"]+"[^>]*>GitHub<\/a>/);
  });
});
