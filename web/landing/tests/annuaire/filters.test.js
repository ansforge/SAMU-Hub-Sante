import {
  getActors,
  getEditors,
  getPerimeter,
  getCurrentFilteredClientsConfig,
} from "../../script/annuaire/filters.js";
import { state } from "../../script/annuaire/data.js";
import { perimeter, FILTER_IDS } from "../../script/annuaire/constants.js";

describe("Filters utils", () => {
  beforeEach(() => {
    state.clientsConfigurations = [
      {
        "P: 15-15": "1.5,2.0,2.1",
        "P: 15-gps": "",
        "P: 15-nexsis": "1.9",
        "P: 15-smur": "",
        client_id: "fr.health.lrm",
        editor: "ANS",
        label: "",
      },
      {
        "P: 15-15": "1.5",
        "P: 15-gps": "",
        "P: 15-nexsis": "",
        "P: 15-smur": "",
        client_id: "fr.health.snp410",
        editor: "Appligos",
        label: "SNP 41",
      },
      {
        "P: 15-15": "1.5",
        "P: 15-gps": "",
        "P: 15-nexsis": "",
        "P: 15-smur": "",
        client_id: "fr.health.samu410",
        editor: "Appligos",
        label: "SAMU 41",
      },
      {
        "P: 15-15": "",
        "P: 15-gps": "",
        "P: 15-nexsis": "1.9",
        "P: 15-smur": "",
        client_id: "fr.health.fire",
        editor: "NexSIS",
        label: "",
      },
    ];
  });

  test("getActors should return unique non-empty labels", () => {
    expect(getActors()).toEqual(["SNP 41", "SAMU 41"]);
  });

  test("getEditors should return unique editors", () => {
    expect(getEditors()).toEqual(["ANS", "Appligos", "NexSIS"]);
  });

  test("getPerimeter should return a copy of perimeter", () => {
    expect(getPerimeter()).toEqual(perimeter);
  });

  describe("getCurrentFilteredClientsConfig", () => {
    beforeEach(() => {
      // Mock expected HTML inputs
      document.body.innerHTML = `
        <select id="${FILTER_IDS.actor}">
          <option value=""></option>
          <option value="SNP 41">SNP 41</option>
          <option value="SAMU 41">SAMU 41</option>
        </select>
        <select id="${FILTER_IDS.editor}">
          <option value=""></option>
          <option value="ANS">ANS</option>
          <option value="Appligos">Appligos</option>
          <option value="NexSIS">NexSIS</option>
        </select>
        <select id="${FILTER_IDS.perimeter}">
          <option value=""></option>
          <option value="15-15">15-15</option>
          <option value="15-NexSIS">15-NexSIS</option>
          <option value="15-SMUR/RPIS">15-SMUR/RPIS</option>
          <option value="15-GPS">15-GPS</option>
        </select>
      `;
    });

    test("should return all configs when no filter is selected", () => {
      expect(getCurrentFilteredClientsConfig()).toEqual(
        state.clientsConfigurations,
      );
    });

    test("should filter by actor", () => {
      const label = "SNP 41";
      document.getElementById(FILTER_IDS.actor).value = label;
      const result = getCurrentFilteredClientsConfig();
      const expectedResult = state.clientsConfigurations.filter(
        (client) => client.label == label,
      );
      expect(result).toEqual(expectedResult);
    });

    test("should filter by editor", () => {
      const editor = "ANS";
      document.getElementById(FILTER_IDS.editor).value = editor;
      const result = getCurrentFilteredClientsConfig();
      const expectedResult = state.clientsConfigurations.filter(
        (client) => client.editor == editor,
      );
      expect(result).toEqual(expectedResult);
    });

    test("should filter by perimeter", () => {
      const perimeter = "15-15";
      document.getElementById(FILTER_IDS.perimeter).value = perimeter;
      const result = getCurrentFilteredClientsConfig();
      const expectedResult = state.clientsConfigurations.filter(
        (client) => client.perimeter !== "",
      );
      expect(result).toEqual(expectedResult);
    });

    test("should apply multiple filters (perimeter and editor)", () => {
      const perimeter = "15-15";
      const editor = "ANS";
      document.getElementById(FILTER_IDS.perimeter).value = perimeter;
      document.getElementById(FILTER_IDS.editor).value = editor;
      const result = getCurrentFilteredClientsConfig();
      const expectedResult = state.clientsConfigurations.filter(
        (client) => client.perimeter !== "" && client.editor == editor,
      );
      expect(result).toEqual(expectedResult);
    });

    test("should apply multiple filters (perimeter, actor and editor)", () => {
      const perimeter = "15-15";
      const editor = "ANS";
      const actor = "SAMU 41";
      document.getElementById(FILTER_IDS.perimeter).value = perimeter;
      document.getElementById(FILTER_IDS.editor).value = editor;
      document.getElementById(FILTER_IDS.actor).value = actor;
      const result = getCurrentFilteredClientsConfig();
      const expectedResult = state.clientsConfigurations.filter(
        (client) =>
          client.perimeter !== "" &&
          client.editor == editor &&
          client.label == actor,
      );
      expect(result).toEqual(expectedResult);
    });
  });
});
