import {
  fetchData,
  constituteLabel,
  getDepartmentsInProd,
  getActorsFromDepartment,
  state,
  sortClientConfig,
  getActorsInfo,
} from "../../script/annuaire/data.js";
import { CLIENT_ID_PREFIX } from "../../script/annuaire/constants.js";
import { expect, jest } from "@jest/globals";

describe("Data utils", () => {
  test("should constitute label from client_id", () => {
    const clients = [
      { client_id: "fr.health.samu750" },
      { client_id: "fr.health.samu76B" },
      { client_id: "fr.health.snp974" },
      { client_id: "fr.health.test.scriptal" },
    ];
    const expected_labels = ["SAMU 75", "SAMU 76B", "SNP 974", ""];
    clients.forEach((c, index) => {
      expect(constituteLabel(c)).toBe(expected_labels[index]);
    });
  });

  describe("State-based data utils", () => {
    beforeEach(() => {
      state.clientsConfigurations = [
        {
          client_id: CLIENT_ID_PREFIX.SAMU + "750",
          client_type: "Editeur A",
          label: "SAMU 75",
          perimeters: {
            "15-15": true,
            "15-gps": false,
            "15-nexsis": false,
            "15-smur": true,
            "15-cap": false,
            "15-cnr114": false,
            "15-portail": false,
          },
        },
        {
          client_id: CLIENT_ID_PREFIX.SNP + "330",
          client_type: "Editeur B",
          label: "SNP 33",
          perimeters: {
            "15-15": false,
            "15-gps": false,
            "15-nexsis": true,
            "15-smur": false,
            "15-cap": false,
            "15-cnr114": false,
            "15-portail": false,
          },
        },
        {
          client_id: CLIENT_ID_PREFIX.SNP + "750",
          client_type: "Editeur C",
          label: "SNP 75",
          perimeters: {
            "15-15": true,
            "15-gps": true,
            "15-nexsis": false,
            "15-smur": true,
            "15-cap": false,
            "15-cnr114": false,
            "15-portail": false,
          },
        },
        {
          client_id: "fr.health.lrm2",
          client_type: "ANS",
          label: "",
          perimeters: {
            "15-15": false,
            "15-gps": false,
            "15-nexsis": true,
            "15-smur": false,
            "15-cap": false,
            "15-cnr114": false,
            "15-portail": false,
          },
        },
        {
          client_id: "fr.health.lrm1",
          client_type: "ANS",
          label: "",
          perimeters: {
            "15-15": false,
            "15-gps": false,
            "15-nexsis": false,
            "15-smur": false,
            "15-cap": false,
            "15-cnr114": false,
            "15-portail": false,
          },
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

    test("should sort client config by client_type then by client_id", () => {
      const sortedClientConfig = sortClientConfig(state.clientsConfigurations);
      const expected_client_id_suite = [
        "fr.health.lrm1",
        "fr.health.lrm2",
        CLIENT_ID_PREFIX.SAMU + "750",
        CLIENT_ID_PREFIX.SNP + "330",
        CLIENT_ID_PREFIX.SNP + "750",
      ];
      sortedClientConfig.forEach((c, index) => {
        expect(c.client_id).toEqual(expected_client_id_suite[index]);
      });
    });

    test("should return client actors info", () => {
      const expectedInfo = [
        "15-15, 15-SMUR/RPIS",
        "15-NexSIS",
        "15-15, 15-SMUR/RPIS, 15-GPS",
        "",
        "",
      ];
      state.clientsConfigurations.forEach((c, index) => {
        expect(getActorsInfo(c)).toEqual(expectedInfo[index]);
      });
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
          client_id: "fr.health.lrm",
          client_name: "LRM",
          client_type: "SAS",
          perimeters: {
            "15-15": true,
            "15-gps": true,
            "15-nexsis": true,
            "15-smur": true,
            "15-cap": true,
            "15-cnr114": true,
            "15-portail": true,
          },
        },
        {
          client_id: "fr.health.lrm2",
          client_name: "LRM 2",
          client_type: "SAS",
          perimeters: {
            "15-15": true,
            "15-gps": false,
            "15-nexsis": false,
            "15-smur": false,
            "15-cap": false,
            "15-cnr114": false,
            "15-portail": false,
          },
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