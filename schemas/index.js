const fs = require("fs");
const Xsd2JsonSchema = require('xsd2jsonschema').Xsd2JsonSchema;
const filepath = process.argv[2] ?? "test.xsd";

fs.readFile(filepath, "utf8", function(err, schema) {
    // console.log(schema);
    const xs2js = new Xsd2JsonSchema();

    try {
        const convertedSchemas = xs2js.processAllSchemas({
            schemas: {[filepath]: schema}
        });
        const jsonSchema = convertedSchemas[filepath].getJsonSchema();
        console.log(JSON.stringify(jsonSchema, null, 2));
    } catch (e) {
        console.log(e);
    }
});
