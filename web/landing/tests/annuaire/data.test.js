import {
  fetchData,
  constituteLabel,
  renameKeys,
  getDepartmentsInProd,
  getActorsFromDepartment,
  state,
} from "../../script/annuaire/data.js";
import { keyMap, CLIENT_ID_PREFIX } from "../../script/annuaire/constants.js";
import { jest } from "@jest/globals";

describe("Data utils", () => {
  test("should constitute label from CSV client raw", () => {
    const clients = [
      { client_id: "fr.health.samu750" }, //rows in CSV (we only need client_id here)
      { client_id: "fr.health.samu76B" },
      { client_id: "fr.health.snp974" },
      { client_id: "fr.health.test.scriptal" },
    ];
    const expected_labels = ["SAMU 75", "SAMU 76B", "SNP 974", ""];
    clients.forEach((c, index) => {
      expect(constituteLabel(c)).toBe(expected_labels[index]);
    });
  });

  test("should rename keys based on the mapping", () => {
    const client_fetched = {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.9",
      "P: 15-smur": "1.6,1.7",
      client_id: "fr.health.lrm",
      editor: "ANS",
    };
    const client_expected = {
      "15-15": "1.5,2.0,2.1",
      "15-GPS": "1.3",
      "15-NexSIS": "1.9",
      "15-SMUR/RPIS": "1.6,1.7",
      client_id: "fr.health.lrm",
      editor: "ANS",
    };
    expect(renameKeys(client_fetched, keyMap)).toEqual(client_expected);
  });

  describe("State-based data utils", () => {
    beforeEach(() => {
      state.clientsConfigurations = [
        {
          client_id: CLIENT_ID_PREFIX.SAMU + "750",
          editor: "Editeur A",
          "15-15": "1.5,2.0,2.1",
          "15-GPS": "1.3",
          "15-NexSIS": "1.9",
          "15-SMUR/RPIS": "1.6,1.7",
        },
        {
          client_id: CLIENT_ID_PREFIX.SNP + "330",
          editor: "Editeur B",
          "15-15": "1.5,2.0,2.1",
          "15-GPS": "1.3",
          "15-NexSIS": "1.9",
          "15-SMUR/RPIS": "1.6,1.7",
        },
        {
          client_id: CLIENT_ID_PREFIX.SNP + "750",
          editor: "Editeur C",
          "15-15": "1.5,2.0,2.1",
          "15-GPS": "1.3",
          "15-NexSIS": "1.9",
          "15-SMUR/RPIS": "1.6,1.7",
        },
        {
          client_id: "fr.health.lrm",
          editor: "ANS",
          "15-15": "1.5,2.0,2.1",
          "15-GPS": "1.3",
          "15-NexSIS": "1.9",
          "15-SMUR/RPIS": "1.6,1.7",
        },
      ];
    });

    test("should return departments in prod from clients configurations", () => {
      expect(getDepartmentsInProd().sort()).toEqual(["33", "75"]);
    });

    test("should get actors from department", () => {
      const actors = getActorsFromDepartment("75");
      const expected_actors = [
        state.clientsConfigurations[0],
        state.clientsConfigurations[2],
      ];
      expect(actors).toEqual(expected_actors);
    });
  });

  describe("fetchData", () => {
    beforeEach(() => {
      global.fetch = jest.fn();
    });

    afterEach(() => {
      jest.resetAllMocks();
    });

    test("should return parsed JSON when fetch is successful", async () => {
      const mockData = [
        {
          "P: 15-15": "1.5,2.0,2.1",
          "P: 15-gps": "1.3",
          "P: 15-nexsis": "1.9",
          "P: 15-smur": "1.6,1.7",
          client_id: "fr.health.lrm",
          editor: "ANS",
        },
        {
          "P: 15-15": "1.5,2.0,2.1",
          "P: 15-gps": "1.3",
          "P: 15-nexsis": "1.9",
          "P: 15-smur": "1.6,1.7",
          client_id: "fr.health.lrm2",
          editor: "ANS",
        },
      ];

      global.fetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockData,
      });

      const result = await fetchData("https://api.test/success");
      expect(result).toEqual(mockData);
      expect(global.fetch).toHaveBeenCalledWith("https://api.test/success");
    });

    test("should return null and log error message when response is not ok", async () => {
      const consoleSpy = jest
        .spyOn(console, "error")
        .mockImplementation(() => {});

      global.fetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
      });

      const url = "https://api.test/notfound";
      const result = await fetchData(url);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalledWith(
        `Erreur lors de la récupération des données depuis ${url} :`,
        new Error("Erreur HTTP : 404"),
      );

      consoleSpy.mockRestore();
    });

    test("should return null and log error message when fetch throws", async () => {
      const consoleSpy = jest
        .spyOn(console, "error")
        .mockImplementation(() => {});
      const url = "https://api.test/fail";

      global.fetch.mockRejectedValueOnce(new Error("Network failure"));

      const result = await fetchData(url);

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalledWith(
        `Erreur lors de la récupération des données depuis ${url} :`,
        new Error("Network failure"),
      );

      consoleSpy.mockRestore();
    });
  });
});
