### cisu-create-message changes :
wrap coord, riskThreat (array in xsd)

go one step deeper in recipients array : recipient.recipient (wrapper obj in xsd)

lower case in CivicAddress (won't be an issue anymore with OpenAPI generation)

rewrite locId, heightRole propertyNames to match model (won't be an issue anymore with OpenAPI generation)

remove '~' in template (remove unwanted extra whitespaces)


### model changes :
*commit 0295d6bf*

add Jackson annotations to handle wrapper element
```
@JacksonXmlProperty(localName = "recipient")
@JacksonXmlElementWrapper(useWrapping = false)
```

handle differences in propertyNames in JSON & xsd schemas :
```
@JsonProperty("locId")
@JacksonXmlProperty(localName = "loc_Id")
```

*commit d8c41920*

add property to exclude null values (will be automatic with OpenAPI generator)
```
@JsonInclude(JsonInclude.Include.NON_EMPTY)
```

add attribute to handle namespace at root level & rename specific message
```
@JacksonXmlRootElement(localName = "message")
@JacksonXmlProperty(isAttribute = true)
String xmlns = "urn:emergency:cisu:2.0";
```
