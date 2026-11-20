import { describe, expect, it } from "vitest";
import { apiUrl } from "./api";

describe("apiUrl", () => {
  it("주소와 경로를 슬래시 하나로 잇는다", () => {
    expect(apiUrl("/api/ping", "http://localhost:8080")).toBe(
      "http://localhost:8080/api/ping",
    );
  });

  it("주소 끝과 경로 앞의 슬래시가 겹쳐도 하나로 맞춘다", () => {
    expect(apiUrl("/api/ping", "http://localhost:8080/")).toBe(
      "http://localhost:8080/api/ping",
    );
  });

  it("경로 앞에 슬래시가 없어도 붙인다", () => {
    expect(apiUrl("api/ping", "http://localhost:8080")).toBe(
      "http://localhost:8080/api/ping",
    );
  });
});
