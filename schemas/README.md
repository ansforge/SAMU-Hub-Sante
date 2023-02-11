## XSD to JSON Schema
```
node index.js shiporder.xsd shiporder.xsd.json    
./loop.sh # for all files
```

## AsyncAPI to Java class
```
asyncapi generate models java ./shiporder.api.json --packageName=com.hubsante --log-diagnostics -o class.Java
```

## XML to JSON in Java
```
# From root of repo => might need `cd ..`
gradle -Pmain=com.hubsante.Example run 
```