import { constituteLabel } from "../../script/annuaire/data.js";

describe("constitute label from CSV client raw", () => {

  test("fr.health.samu750 -> SAMU 75", () => {
    const client = { client_id: "fr.health.samu750" }; //row in CSV (we only need client_id here)
    expect(constituteLabel(client)).toBe("SAMU 75");
  });

  test("fr.health.samu76B -> SAMU 76B", () => {
    const client = { client_id: "fr.health.samu76B" };
    expect(constituteLabel(client)).toBe("SAMU 76B");
  });

  test("fr.health.snp974 -> SNP 974", () => {
    const client = { client_id: "fr.health.snp974" };
    expect(constituteLabel(client)).toBe("SNP 974");
  });

	test("should return empty string if client_id is not SAMU or SNP", () => {
    const client = { client_id: "fr.health.test.scriptal" };
    expect(constituteLabel(client)).toBe("");
  });
	
});
